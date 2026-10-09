package com.ahmetkaragunlu.financeai.fcm.testing

import com.ahmetkaragunlu.financeai.core.database.testing.AccountDatabaseFixture
import com.ahmetkaragunlu.financeai.core.firebase.UserFields
import com.ahmetkaragunlu.financeai.fcm.FCMTokenManager
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock

/** Real isolated Room/work queues and controlled SDK boundaries; no live device tokens. */
class TokenManagerFixture : AutoCloseable {
    val local = AccountDatabaseFixture()
    val auth = mock(FirebaseAuth::class.java)
    val messaging = mock(FirebaseMessaging::class.java)
    val firestore = mock(FirebaseFirestore::class.java)
    val users = mock(CollectionReference::class.java)
    val profile = mock(DocumentReference::class.java)
    var current: FirebaseUser? = null
    var write: Task<Void> = Tasks.forResult(null)
    val manager = FCMTokenManager(firestore, auth, messaging, local.database, local.workManager, local.clock)

    init {
        `when`(auth.currentUser).thenAnswer { current }
        `when`(firestore.collection("users")).thenReturn(users)
        `when`(users.document("A")).thenReturn(profile)
        doAnswer { write }.`when`(profile).update(eq(UserFields.FCM_TOKENS), any())
        `when`(messaging.deleteToken()).thenReturn(Tasks.forResult(null))
        signIn("A")
    }

    fun signIn(owner: String?, verified: Boolean = true) {
        current = owner?.let {
            mock(FirebaseUser::class.java).also { user ->
                `when`(user.uid).thenReturn(it)
                `when`(user.isEmailVerified).thenReturn(verified)
            }
        }
    }

    override fun close() = local.close()
}
