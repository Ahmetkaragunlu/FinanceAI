package com.ahmetkaragunlu.financeai.core.session

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import androidx.work.WorkManager
import com.ahmetkaragunlu.financeai.core.coroutines.di.ApplicationScope
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.money.MoneyAmounts
import com.ahmetkaragunlu.financeai.core.sync.AccountSyncEngine
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

@Singleton
class SessionCoordinator @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val database: FinanceDatabase,
    val session: AccountSession,
    private val scheduler: SyncScheduler,
    private val engine: Lazy<AccountSyncEngine>,
    private val workManager: WorkManager,
    private val workRestorer: SessionWorkRestorer,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val applicationScope: CoroutineScope
) {
    private var observer: Job? = null
    private var accountJob: Job? = null

    @Synchronized
    fun start() {
        if (observer != null) return
        observer = applicationScope.launch {
            callbackFlow {
                val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
                auth.addAuthStateListener(listener)
                awaitClose { auth.removeAuthStateListener(listener) }
            }.collectLatest {
                try { prepare() } catch (e: CancellationException) { throw e }
                catch (e: Exception) { Log.w("SessionCoordinator", "Account preparation failed (${e.javaClass.simpleName})") }
            }
        }
    }

    suspend fun prepare() = session.mutex.withLock {
        val user = auth.currentUser?.takeIf { it.isEmailVerified }
        if (user != null && user.uid == session.account.value?.ownerId) {
            if (accountJob?.isActive != true) startAccountJob(session.requireAccount())
            return@withLock
        }
        stopAccount()
        if (user == null) return@withLock
        val local = database.accountDao().get(user.uid)
        val currency = local?.currencyCode ?: withTimeout(15_000) {
            val proposed = MoneyAmounts.currencyForRegion(Locale.getDefault())
            val ref = firestore.collection("users").document(user.uid)
            firestore.runTransaction { transaction ->
                val existing = transaction.get(ref).getString("currencyCode")
                if (existing != null) {
                    require(MoneyAmounts.scale(existing) >= 0)
                    existing
                } else {
                    transaction.set(ref, mapOf("currencyCode" to checkNotNull(proposed) { "Device region has no currency" }), SetOptions.merge())
                    checkNotNull(proposed)
                }
            }.await()
        }
        check(auth.currentUser?.uid == user.uid)
        database.withTransaction {
            database.accountDao().save(AccountPreferences(user.uid, currency))
            database.accountDao().setActive(ActiveAccountRow(ownerId = user.uid))
        }
        session.activate(user.uid, currency)
        val account = session.requireAccount()
        scheduler.enqueue(account.ownerId)
        startAccountJob(account)
    }

    private fun startAccountJob(account: ActiveAccount) {
        accountJob = applicationScope.launch {
            try {
                workRestorer.restore(account)
                engine.get().listen(account)
            }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Log.w("SessionCoordinator", "Account listener failed (${e.javaClass.simpleName})") }
        }
    }

    suspend fun signOut() = session.mutex.withLock {
        stopAccount()
        auth.signOut()
    }

    private suspend fun stopAccount() {
        val old = session.account.value
        session.deactivate()
        // Cancellation requests removal immediately; joining while holding the DB guard would deadlock.
        accountJob?.cancel()
        accountJob = null
        database.accountDao().clearActive()
        if (old != null) {
            scheduler.stop(old.ownerId)
            workManager.cancelAllWorkByTag("account_${old.ownerId}")
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
        }
    }
}
