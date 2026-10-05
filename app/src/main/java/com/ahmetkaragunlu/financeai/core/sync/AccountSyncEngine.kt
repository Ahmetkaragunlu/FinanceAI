package com.ahmetkaragunlu.financeai.core.sync

import com.ahmetkaragunlu.financeai.core.sync.contract.SyncFields

import com.ahmetkaragunlu.financeai.core.sync.local.entity.SyncRecord

import android.util.Log
import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import androidx.room.withTransaction
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.session.ActiveAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

@Singleton
class AccountSyncEngine @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val database: FinanceDatabase,
    private val session: AccountSession,
    private val scheduler: SyncScheduler,
    remoteStores: Set<@JvmSuppressWildcards RemoteRecordStore>,
    private val participants: Set<@JvmSuppressWildcards AccountSyncParticipant>
) {
    private val stores = remoteStores.associateBy { it.collection }
    private val syncMutex = Mutex()

    suspend fun listen(account: ActiveAccount): Unit = coroutineScope {
        stores.values.forEach { store -> launch {
            callbackFlow {
                val registration = firestore.collection(store.collection)
                    .whereEqualTo(SyncFields.USER_ID, account.ownerId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) close(error)
                        else if (snapshot != null && !snapshot.metadata.isFromCache && !snapshot.metadata.hasPendingWrites()) {
                            trySend(snapshot)
                        }
                    }
                awaitClose { registration.remove() }
            }.collect { snapshot ->
                for (document in snapshot.documents) receiveSafely(account, store, document)
                snapshot.documentChanges.filter { it.type == DocumentChange.Type.REMOVED }.forEach { change ->
                    val fresh = change.document.reference.get(Source.SERVER).await()
                    if (!fresh.exists()) receiveSafely(account, store, fresh)
                }
            }
        } }
    }

    suspend fun synchronize(account: ActiveAccount) = syncMutex.withLock {
        ensureCurrent(account)
        val held = participants.flatMap { it.heldRecords(account) }.toSet()
        // Push against server transactions first; a pull can never overwrite a pending local intention.
        for (pending in database.syncRecordDao().pending(account.ownerId)) {
            if ((pending.collection to pending.remoteId) in held) continue
            try { push(account, pending) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                val permanent = e is DataAccessException.InvalidRemoteData || e is DataAccessException.AccessDenied ||
                    e is IllegalArgumentException || e is ArithmeticException || (e is FirebaseFirestoreException && e.code in setOf(
                    FirebaseFirestoreException.Code.PERMISSION_DENIED, FirebaseFirestoreException.Code.INVALID_ARGUMENT,
                    FirebaseFirestoreException.Code.FAILED_PRECONDITION))
                if (!permanent) throw e
                session.withAccount { current ->
                    check(current == account)
                    val latest = database.syncRecordDao().get(account.ownerId, pending.collection, pending.remoteId)
                    if (latest != null && latest.mutationId == pending.mutationId) database.syncRecordDao().save(latest.copy(permanentFailure = true))
                }
                Log.w("AccountSyncEngine", "Permanent record sync failure (${e.javaClass.simpleName})")
            }
        }
        participants.forEach { it.synchronize(account) }
        stores.values.forEach { store ->
            val snapshot = firestore.collection(store.collection).whereEqualTo(SyncFields.USER_ID, account.ownerId)
                .get(Source.SERVER).await()
            for (document in snapshot.documents) receiveSafely(account, store, document)
            val visible = snapshot.documents.map { it.id }.toSet()
            database.syncRecordDao().forAccount(account.ownerId).filter { it.collection == store.collection && it.remoteId !in visible && it.basePayload != null }.forEach { known ->
                val fresh = firestore.collection(store.collection).document(known.remoteId).get(Source.SERVER).await()
                if (!fresh.exists()) receiveSafely(account, store, fresh)
            }
        }
    }

    private fun ensureCurrent(account: ActiveAccount) {
        if (!session.isCurrent(account) || auth.currentUser?.uid != account.ownerId) throw CancellationException("Stale account")
    }

    private fun payload(store: RemoteRecordStore, document: DocumentSnapshot, account: ActiveAccount): String? {
        if (!document.exists()) return null
        if (document.getString(SyncFields.USER_ID) != account.ownerId) throw DataAccessException.AccessDenied()
        if (document.getBoolean(SyncFields.DELETED) == true) return null
        return SyncPayload.encode(store.normalize(document.data.orEmpty(), account))
    }

    private suspend fun receiveSafely(account: ActiveAccount, store: RemoteRecordStore, document: DocumentSnapshot) {
        try { receive(account, store, document) }
        catch (e: CancellationException) { throw e }
        catch (e: DataAccessException.InvalidRemoteData) { Log.w("AccountSyncEngine", "Invalid remote record retained without applying") }
        catch (e: IllegalArgumentException) { Log.w("AccountSyncEngine", "Invalid remote record retained without applying") }
        catch (e: ArithmeticException) { Log.w("AccountSyncEngine", "Invalid remote money retained without applying") }
    }

    private suspend fun receive(account: ActiveAccount, store: RemoteRecordStore, document: DocumentSnapshot) {
        if (participants.any { (store.collection to document.id) in it.heldRecords(account) }) return
        val remote = payload(store, document, account)
        val revision = document.getLong(SyncFields.REVISION) ?: 0L
        ensureCurrent(account)
        val prepared = remote?.let { store.prepare(account, document.id, SyncPayload.decode(it)) }
        session.mutex.withLock {
            ensureCurrent(account)
            database.withTransaction {
                val dao = database.syncRecordDao()
                val old = dao.get(account.ownerId, store.collection, document.id)
                if (document.exists() && old != null && revision < old.baseRevision) return@withTransaction
                if (old?.mutationId != null) {
                    val decision = if (acknowledgesDeletion(document.getBoolean(SyncFields.DELETED) == true,
                        document.getString(SyncFields.PREVIOUS_MUTATION_ID), old.mutationId)) Reconciliation.Equal
                        else reconcile(old.basePayload, if (old.pendingDelete) null else old.pendingPayload, remote)
                    if (decision == Reconciliation.Conflict) {
                        dao.save(old.copy(conflictPayload = remote, conflictRevision = revision))
                    } else if (decision == Reconciliation.Equal) {
                        store.apply(account, document.id, prepared)
                        dao.save(SyncRecord(account.ownerId, store.collection, document.id, remote, revision))
                    }
                    return@withTransaction
                }
                store.apply(account, document.id, prepared)
                dao.save(SyncRecord(account.ownerId, store.collection, document.id, remote, revision))
            }
        }
    }

    private suspend fun push(account: ActiveAccount, pending: SyncRecord) {
        ensureCurrent(account)
        val store = stores.getValue(pending.collection)
        val ref = firestore.collection(store.collection).document(pending.remoteId)
        val outcome = firestore.runTransaction { transaction ->
            ensureCurrent(account)
            val document = transaction.get(ref)
            val remote = payload(store, document, account)
            val revision = document.getLong(SyncFields.REVISION) ?: 0L
            // Mutation identity acknowledges retries after remote success / local process death.
            if (document.getString(SyncFields.MUTATION_ID) == pending.mutationId) return@runTransaction RemoteOutcome(remote, revision, false)
            if (acknowledgesDeletion(document.getBoolean(SyncFields.DELETED) == true,
                    document.getString(SyncFields.PREVIOUS_MUTATION_ID), pending.mutationId)) return@runTransaction RemoteOutcome(remote, revision, false)
            when (val decision = reconcile(pending.basePayload, if (pending.pendingDelete) null else pending.pendingPayload, remote)) {
                Reconciliation.Conflict -> RemoteOutcome(remote, revision, true)
                Reconciliation.Equal -> RemoteOutcome(remote, revision, false)
                is Reconciliation.Write -> {
                    val values = decision.payload?.let(SyncPayload::decode).orEmpty().toMutableMap()
                    values.putAll(mapOf(SyncFields.USER_ID to account.ownerId, SyncFields.REVISION to revision + 1,
                        SyncFields.MUTATION_ID to pending.mutationId, SyncFields.DELETED to (decision.payload == null)))
                    transaction.set(ref, values, SetOptions.merge())
                    RemoteOutcome(decision.payload, revision + 1, false)
                }
            }
        }.await()
        ensureCurrent(account)
        val prepared = if (outcome.conflict) null else outcome.payload?.let { store.prepare(account, pending.remoteId, SyncPayload.decode(it)) }
        session.mutex.withLock {
            ensureCurrent(account)
            database.withTransaction {
                val dao = database.syncRecordDao()
                val current = dao.get(account.ownerId, pending.collection, pending.remoteId) ?: return@withTransaction
                val acknowledgement = acknowledgeSync(current, pending.mutationId, outcome.payload, outcome.revision, outcome.conflict)
                if (acknowledgement.applyRemote) store.apply(account, pending.remoteId, prepared)
                dao.save(acknowledgement.record)
            }
        }
    }

    suspend fun resolve(conflict: SyncRecord, keepLocal: Boolean) {
        val expected = session.requireAccount()
        require(conflict.ownerId == expected.ownerId)
        val store = stores.getValue(conflict.collection)
        val document = firestore.collection(conflict.collection).document(conflict.remoteId).get(Source.SERVER).await()
        val freshPayload = payload(store, document, expected)
        val freshRevision = document.getLong(SyncFields.REVISION) ?: 0L
        ensureCurrent(expected)
        val prepared = if (keepLocal) null else freshPayload?.let { store.prepare(expected, conflict.remoteId, SyncPayload.decode(it)) }
        val featureResolution = participants.firstNotNullOfOrNull {
            it.prepareResolution(expected, conflict, keepLocal, document.data.orEmpty())
        }
        session.withAccount { account ->
            check(account == expected)
            require(conflict.ownerId == account.ownerId)
            database.withTransaction {
                val dao = database.syncRecordDao()
                val current = dao.get(account.ownerId, conflict.collection, conflict.remoteId) ?: return@withTransaction
                if (current != conflict || current.conflictRevision == null) return@withTransaction
                if (freshRevision != current.conflictRevision || !SyncPayload.equivalent(freshPayload, current.conflictPayload)) {
                    dao.save(current.copy(conflictPayload = freshPayload, conflictRevision = freshRevision))
                    return@withTransaction
                }
                if (featureResolution?.apply(current) == true) return@withTransaction
                if (participants.any { it.resolve(account, current, keepLocal, freshPayload, prepared, freshRevision) })
                    return@withTransaction
                val store = stores.getValue(current.collection)
                if (keepLocal) dao.save(current.copy(basePayload = current.conflictPayload,
                    baseRevision = current.conflictRevision, conflictPayload = null, conflictRevision = null))
                else {
                    store.apply(account, current.remoteId, prepared)
                    dao.save(SyncRecord(account.ownerId, current.collection, current.remoteId, current.conflictPayload, current.conflictRevision))
                }
            }
            scheduler.enqueue(account.ownerId)
        }
    }

    private data class RemoteOutcome(val payload: String?, val revision: Long, val conflict: Boolean)
}
