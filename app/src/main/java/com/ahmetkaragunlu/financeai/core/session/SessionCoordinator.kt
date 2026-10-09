package com.ahmetkaragunlu.financeai.core.session

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import androidx.work.WorkManager
import com.ahmetkaragunlu.financeai.core.coroutines.di.ApplicationScope
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.firebase.FirestoreCollections
import com.ahmetkaragunlu.financeai.core.firebase.UserFields
import com.ahmetkaragunlu.financeai.core.money.MoneyAmounts
import com.ahmetkaragunlu.financeai.core.session.local.entity.AccountPreferences
import com.ahmetkaragunlu.financeai.core.session.local.entity.ActiveAccountRow
import com.ahmetkaragunlu.financeai.core.sync.AccountSyncEngine
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.ahmetkaragunlu.financeai.core.sync.contract.FinancialFields
import com.ahmetkaragunlu.financeai.core.work.AccountWork
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

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

    suspend fun prepare() = session.withStateLock {
        val user = auth.currentUser?.takeIf { it.isEmailVerified }
        if (user != null && user.uid == session.account.value?.ownerId) {
            if (accountJob?.isActive != true) startAccountJob(session.requireAccount())
            return@withStateLock
        }
        stopAccount()
        if (user == null) return@withStateLock
        val local = database.accountDao().get(user.uid)
        val preferences = local?.takeIf { it.timeZoneId != null } ?: withTimeout(15_000) {
            val proposed = MoneyAmounts.currencyForRegion(Locale.getDefault())
            val ref = firestore.collection(FirestoreCollections.USERS).document(user.uid)
            firestore.runTransaction { transaction ->
                val document = transaction.get(ref)
                val currency = document.getString(FinancialFields.CURRENCY_CODE) ?: local?.currencyCode
                    ?: checkNotNull(proposed) { "Device region has no currency" }
                require(MoneyAmounts.scale(currency) >= 0)
                val zone = document.getString(UserFields.TIME_ZONE_ID) ?: ZoneId.systemDefault().id
                ZoneId.of(zone)
                transaction.set(ref, mapOf(FinancialFields.CURRENCY_CODE to currency, UserFields.TIME_ZONE_ID to zone), SetOptions.merge())
                AccountPreferences(user.uid, currency, zone)
            }.await()
        }
        check(auth.currentUser?.uid == user.uid)
        database.withTransaction {
            database.accountDao().save(preferences)
            database.accountDao().setActive(ActiveAccountRow(ownerId = user.uid))
        }
        session.activate(user.uid, preferences.currencyCode, checkNotNull(preferences.timeZoneId))
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

    suspend fun activeAccountFor(ownerId: String): ActiveAccount? {
        prepare()
        return session.account.value?.takeIf { it.ownerId == ownerId }
    }

    suspend fun signOut() = session.withStateLock {
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
            workManager.cancelAllWorkByTag(AccountWork.tag(old.ownerId))
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
        }
    }
}
