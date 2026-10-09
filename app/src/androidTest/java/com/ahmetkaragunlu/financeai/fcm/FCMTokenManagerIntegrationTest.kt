package com.ahmetkaragunlu.financeai.fcm

import com.ahmetkaragunlu.financeai.core.sync.testing.EmulatorAccountFixture
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.Source
import com.google.firebase.messaging.FirebaseMessaging
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

/** Actual array transforms in an explicit demo namespace; Messaging and Auth are controlled. */
class FCMTokenManagerIntegrationTest {
    @Test fun registeringAndRevokingOneSyntheticDevicePreservesOtherRegisteredDevices(): Unit = runBlocking {
        val f = EmulatorAccountFixture()
        try {
            withTimeout(25_000) {
                val owner = "token-${UUID.randomUUID()}"
                f.activate(owner)
                val profile = f.firestore.collection("users").document(owner)
                profile.set(mapOf("fcmTokens" to listOf("other-synthetic-device"))).await()
                val messaging = mock(FirebaseMessaging::class.java)
                `when`(messaging.deleteToken()).thenReturn(Tasks.forResult(null))
                val manager = FCMTokenManager(f.firestore, f.auth, messaging, f.local.database,
                    f.local.workManager, f.local.clock)
                manager.suppliedToken("synthetic-current-device")
                manager.flush(owner)
                assertEquals(listOf("other-synthetic-device", "synthetic-current-device"),
                    profile.get(Source.SERVER).await().get("fcmTokens"))
                assertTrue(f.local.database.tokenOperationDao().pending(owner).isEmpty())
                manager.removeFCMToken()
                assertEquals(listOf("other-synthetic-device"), profile.get(Source.SERVER).await().get("fcmTokens"))
                assertTrue(f.local.database.tokenOperationDao().pending(owner).isEmpty())
            }
        } finally {
            f.close()
        }
    }
}
