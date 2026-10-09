package com.ahmetkaragunlu.financeai.fcm

import com.ahmetkaragunlu.financeai.core.firebase.UserFields
import com.ahmetkaragunlu.financeai.core.work.AccountWork
import com.ahmetkaragunlu.financeai.fcm.data.local.entity.TokenOperation
import com.ahmetkaragunlu.financeai.fcm.testing.TokenManagerFixture
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import java.io.IOException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.eq
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

class FCMTokenManagerTest {
    @Test fun suppliedTokenIsDurableAndKeepsItsAccountScopedRegistrationWork(): Unit = runBlocking {
        TokenManagerFixture().use { f ->
            f.local.activate()
            f.manager.suppliedToken("synthetic-device")
            val operation = f.local.database.tokenOperationDao().pending("A").single()
            assertEquals(TokenOperation("A", "synthetic-device", false, f.local.clock.millis()), operation)
            val works = withTimeout(5_000) {
                f.local.workManager.getWorkInfosForUniqueWorkFlow("token_registration_A").first { it.isNotEmpty() }
            }
            assertTrue(works.single().tags.contains(AccountWork.tag("A")))
            assertTrue(works.single().tags.contains("com.ahmetkaragunlu.financeai.fcm.work.TokenRegistrationWorker"))
            assertThrows(IllegalArgumentException::class.java) { runBlocking { f.manager.suppliedToken(" ") } }
            f.signIn(null)
            f.manager.suppliedToken("signed-out-token")
            assertEquals(listOf(operation), f.local.database.tokenOperationDao().pending("A"))
        }
    }

    @Test fun lateSdkTokenCannotQueueWorkForAnAccountThatHasChanged(): Unit = runBlocking {
        TokenManagerFixture().use { f ->
            val token = TaskCompletionSource<String>()
            `when`(f.messaging.token).thenReturn(token.task)
            val request = async(start = CoroutineStart.UNDISPATCHED) { f.manager.updateFCMToken() }
            f.signIn("B")
            token.setResult("synthetic-late-token")
            request.await()
            assertTrue(f.local.database.tokenOperationDao().pending("A").isEmpty())
            assertTrue(f.local.database.tokenOperationDao().pending("B").isEmpty())
        }
    }

    @Test fun cancelledSdkLookupDoesNotCreateRegistrationIntent(): Unit = runBlocking {
        TokenManagerFixture().use { f ->
            val token = TaskCompletionSource<String>()
            `when`(f.messaging.token).thenReturn(token.task)
            val request = async(start = CoroutineStart.UNDISPATCHED) { f.manager.updateFCMToken() }
            request.cancelAndJoin()
            assertTrue(f.local.database.tokenOperationDao().pending("A").isEmpty())
            assertFalse(token.task.isComplete)
        }
    }

    @Test fun successfulFlushAcknowledgesOnlyAfterUpdatingTheCorrectUserField(): Unit = runBlocking {
        TokenManagerFixture().use { f ->
            f.manager.suppliedToken("synthetic-device")
            f.manager.flush("A")
            val transform = ArgumentCaptor.forClass(Any::class.java)
            verify(f.profile).update(eq("fcmTokens"), transform.capture())
            assertEquals(FieldValue.arrayUnion("synthetic-device").javaClass, transform.value.javaClass)
            assertTrue(f.local.database.tokenOperationDao().pending("A").isEmpty())
            assertEquals("synthetic-device", f.manager.registeredDeviceToken("A"))
            assertTrue(checkNotNull(f.local.database.tokenOperationDao().latestRegistered("A")).acknowledged)
        }
    }

    @Test fun failedRevocationPreservesDurableIntentAndUsesTheRetainedTokenBeforeSdkRotation(): Unit = runBlocking {
        TokenManagerFixture().use { f ->
            f.local.database.tokenOperationDao().save(TokenOperation("A", "retained-device", false, 1, true))
            val error = IOException("synthetic offline")
            f.write = Tasks.forException(error)
            val actual = assertThrows(IOException::class.java) { runBlocking { f.manager.removeFCMToken() } }
            assertSame(error, actual)
            val operation = f.local.database.tokenOperationDao().pending("A").single()
            assertTrue(operation.remove)
            assertEquals("retained-device", operation.token)
            verify(f.messaging).deleteToken()
            verify(f.messaging, never()).token
            val transform = ArgumentCaptor.forClass(Any::class.java)
            verify(f.profile).update(eq("fcmTokens"), transform.capture())
            assertEquals(FieldValue.arrayRemove("retained-device").javaClass, transform.value.javaClass)
        }
    }

    @Test fun accountSwitchDuringRemoteWriteDoesNotAcknowledgeFormerAccountIntent(): Unit = runBlocking {
        TokenManagerFixture().use { f ->
            f.manager.suppliedToken("synthetic-device")
            val gate = TaskCompletionSource<Void>()
            f.write = gate.task
            val started = TaskCompletionSource<Void>()
            doAnswer { started.setResult(null); gate.task }.`when`(f.profile).update(eq(UserFields.FCM_TOKENS), any())
            val flush = async { f.manager.flush("A") }
            withTimeout(5_000) { started.task.await() }
            f.signIn("B")
            gate.setResult(null)
            flush.await()
            assertEquals(1, f.local.database.tokenOperationDao().pending("A").size)
        }
    }

    @Test fun olderRemoteAcknowledgementCannotEraseANewerTokenOperation(): Unit = runBlocking {
        TokenManagerFixture().use { f ->
            val before = TokenOperation("A", "synthetic-device", false, 10)
            f.local.database.tokenOperationDao().save(before)
            val gate = TaskCompletionSource<Void>()
            val started = TaskCompletionSource<Void>()
            doAnswer { started.setResult(null); gate.task }.`when`(f.profile).update(eq(UserFields.FCM_TOKENS), any())
            val flush = async { f.manager.flush("A") }
            withTimeout(5_000) { started.task.await() }
            val newer = before.copy(remove = true, createdAt = 11)
            f.local.database.tokenOperationDao().save(newer)
            gate.setResult(null)
            flush.await()
            assertEquals(listOf(newer), f.local.database.tokenOperationDao().pending("A"))
        }
    }

    @Test fun restoreTokenRemainsRestrictedToTheCurrentVerifiedAccount(): Unit = runBlocking {
        TokenManagerFixture().use { f ->
            f.local.database.tokenOperationDao().save(TokenOperation("A", "A-device", false, 10))
            f.local.database.tokenOperationDao().save(TokenOperation("B", "B-device", false, 20))
            assertEquals("A-device", f.manager.registeredDeviceToken("A"))
            assertNull(f.manager.registeredDeviceToken("B"))
            f.signIn("A", verified = false)
            assertNull(f.manager.registeredDeviceToken("A"))
            f.signIn(null)
            assertNull(f.manager.registeredDeviceToken("A"))
        }
    }
}
