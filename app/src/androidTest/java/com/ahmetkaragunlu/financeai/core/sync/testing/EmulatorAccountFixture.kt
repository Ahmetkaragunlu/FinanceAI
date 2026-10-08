package com.ahmetkaragunlu.financeai.core.sync.testing

import androidx.test.platform.app.InstrumentationRegistry
import com.ahmetkaragunlu.financeai.core.database.testing.AccountDatabaseFixture
import com.ahmetkaragunlu.financeai.core.sync.AccountSyncEngine
import com.ahmetkaragunlu.financeai.core.sync.contract.RemoteRecordStore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.tasks.await
import org.mockito.Mockito

/** Real Room/Firestore integration with controlled Auth callbacks; no real login or production SDK endpoint. */
class EmulatorAccountFixture {
    val local = AccountDatabaseFixture()
    private val host = InstrumentationRegistry.getArguments().getString("firestoreHost")
        ?: error("Pass firestoreHost for the explicit local integration suite")
    private val port = InstrumentationRegistry.getArguments().getString("firestorePort")?.toInt() ?: 8080
    private val app: FirebaseApp
    val firestore: FirebaseFirestore
    val auth: FirebaseAuth = Mockito.mock(FirebaseAuth::class.java)
    private val current = AtomicReference<FirebaseUser?>(null)
    private val listeners = CopyOnWriteArraySet<FirebaseAuth.AuthStateListener>()

    init {
        require(host in setOf("10.0.2.2", "127.0.0.1", "localhost"))
        require(port == 8080)
        val options = FirebaseOptions.Builder()
            .setProjectId("demo-financeai-integration")
            .setApplicationId("1:123:android:integration")
            .setApiKey("synthetic-emulator-key")
            .build()
        app = FirebaseApp.initializeApp(local.context, options, "integration-${UUID.randomUUID()}")
        val collectAutomatically: Boolean? = false
        app.setDataCollectionDefaultEnabled(collectAutomatically)
        firestore = FirebaseFirestore.getInstance(app)
        firestore.useEmulator(host, port)
        Mockito.`when`(auth.currentUser).thenAnswer { current.get() }
        Mockito.doAnswer { invocation ->
            val listener = invocation.getArgument<FirebaseAuth.AuthStateListener>(0)
            listeners += listener
            listener.onAuthStateChanged(auth)
            null
        }.`when`(auth).addAuthStateListener(Mockito.any(FirebaseAuth.AuthStateListener::class.java))
        Mockito.doAnswer { invocation ->
            listeners -= invocation.getArgument<FirebaseAuth.AuthStateListener>(0)
            null
        }.`when`(auth).removeAuthStateListener(Mockito.any(FirebaseAuth.AuthStateListener::class.java))
        Mockito.doAnswer { signIn(null); null }.`when`(auth).signOut()
    }

    fun signIn(owner: String?, verified: Boolean = true) {
        val user = owner?.let {
            Mockito.mock(FirebaseUser::class.java).also { value ->
                Mockito.`when`(value.uid).thenReturn(it)
                Mockito.`when`(value.isEmailVerified).thenReturn(verified)
            }
        }
        current.set(user)
        listeners.forEach { it.onAuthStateChanged(auth) }
    }

    suspend fun activate(owner: String, currency: String = "USD") {
        signIn(owner)
        local.activate(owner, currency)
    }

    fun engine(stores: Set<RemoteRecordStore>) = AccountSyncEngine(
        firestore, auth, local.database, local.session, local.scheduler, stores, emptySet()
    )

    suspend fun close() {
        firestore.terminate().await()
        app.delete()
        local.close()
    }
}
