package com.ahmetkaragunlu.financeai.app.presentation.sync

import androidx.lifecycle.ViewModelStore
import com.ahmetkaragunlu.financeai.core.database.testing.AccountDatabaseFixture
import com.ahmetkaragunlu.financeai.core.session.ActiveAccount
import com.ahmetkaragunlu.financeai.core.sync.AccountSyncEngine
import com.ahmetkaragunlu.financeai.core.sync.contract.RemoteRecordStore
import com.ahmetkaragunlu.financeai.core.sync.local.entity.SyncRecord
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock

class SyncConflictViewModelTest {
    @Test fun duplicateResolutionIsBlockedAndFailureOrCancellationReleasesTheGuardForRetry() = runBlocking {
        val f = AccountDatabaseFixture()
        val models = ViewModelStore()
        try {
            f.activate()
            val firestore = mock(FirebaseFirestore::class.java)
            val auth = mock(FirebaseAuth::class.java)
            val user = mock(FirebaseUser::class.java)
            `when`(auth.currentUser).thenReturn(user)
            `when`(user.uid).thenReturn("A")
            val collection = mock(CollectionReference::class.java)
            val reference = mock(DocumentReference::class.java)
            val document = mock(DocumentSnapshot::class.java)
            `when`(firestore.collection("records")).thenReturn(collection)
            `when`(collection.document("record")).thenReturn(reference)
            `when`(document.exists()).thenReturn(false)
            val gate = TaskCompletionSource<DocumentSnapshot>()
            var fetch: Task<DocumentSnapshot> = gate.task
            var requests = 0
            doAnswer { requests++; fetch }.`when`(reference).get(Source.SERVER)
            val store = object : RemoteRecordStore {
                override val collection = "records"
                override fun normalize(data: Map<String, Any?>, account: ActiveAccount) = data
                override suspend fun apply(account: ActiveAccount, remoteId: String, data: Map<String, Any?>?) = error("Keep-local must not apply remote data")
            }
            val record = SyncRecord("A", "records", "record", pendingPayload = "{}", mutationId = "local", conflictRevision = 0)
            f.database.syncRecordDao().save(record)
            val engine = AccountSyncEngine(firestore, auth, f.database, f.session, f.scheduler, setOf(store), emptySet())
            val vm = withContext(Dispatchers.Main) { SyncConflictViewModel(f.database, engine).also { models.put("sync", it) } }
            withContext(Dispatchers.Main) { vm.resolve(record, true); vm.resolve(record, false) }
            assertTrue(vm.busy.value)
            assertEquals(1, requests)
            gate.setException(FirebaseNetworkException("private"))
            withTimeout(5_000) { while (vm.busy.value) delay(10) }
            assertTrue(vm.error.value)
            fetch = Tasks.forException(CancellationException())
            withContext(Dispatchers.Main) { vm.resolve(record, true) }
            withTimeout(5_000) { while (vm.busy.value) delay(10) }
            assertFalse(vm.error.value)
            fetch = Tasks.forResult(document)
            withContext(Dispatchers.Main) { vm.resolve(record, true) }
            withTimeout(5_000) { while (vm.busy.value) delay(10) }
            assertFalse(vm.error.value)
            assertEquals(3, requests)
            val resolved = f.database.syncRecordDao().get("A", "records", "record")
            assertEquals("local", resolved?.mutationId)
            assertEquals(null, resolved?.conflictRevision)
        } finally {
            withContext(Dispatchers.Main) { models.clear() }
            f.close()
        }
    }
}
