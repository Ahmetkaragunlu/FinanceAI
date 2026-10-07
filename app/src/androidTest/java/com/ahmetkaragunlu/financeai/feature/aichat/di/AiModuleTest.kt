package com.ahmetkaragunlu.financeai.feature.aichat.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.ahmetkaragunlu.financeai.BuildConfig
import com.ahmetkaragunlu.financeai.core.firebase.di.FirebaseModule
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import org.junit.Assert.*
import org.junit.Test

class AiModuleTest {
    @Test fun aiUsesItsNamedAppWhileSessionDataAndPushKeepTheDefaultProject() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val defaultApp = checkNotNull(FirebaseApp.initializeApp(context))
        val collectionBefore = defaultApp.isDataCollectionDefaultEnabled
        val defaultCheck = FirebaseAppCheck.getInstance(defaultApp)
        val defaultPush = FirebaseMessaging.getInstance()
        val pushAutoInitBefore = defaultPush.isAutoInitEnabled

        val aiApp = AiModule.provideAiFirebaseApp(context)
        assertNotSame(defaultApp, aiApp)
        assertNotEquals(aiApp.options.projectId, defaultApp.options.projectId)
        assertEquals(BuildConfig.AI_FIREBASE_PROJECT_ID, aiApp.options.projectId)
        assertEquals(BuildConfig.AI_FIREBASE_APP_ID, aiApp.options.applicationId)
        // Avoid including the API key in assertion failure output.
        assertTrue("AI app must use the configured client key", aiApp.options.apiKey == BuildConfig.AI_FIREBASE_API_KEY)
        assertNull(aiApp.options.databaseUrl)
        assertNull(aiApp.options.storageBucket)
        assertNull(aiApp.options.gcmSenderId)
        assertSame(defaultApp, FirebaseApp.getInstance())
        assertSame(aiApp, AiModule.provideAiFirebaseApp(context))
        assertEquals(collectionBefore, defaultApp.isDataCollectionDefaultEnabled)
        assertEquals(pushAutoInitBefore, defaultPush.isAutoInitEnabled)
        assertFalse(aiApp.isDataCollectionDefaultEnabled)
        assertNotSame(defaultCheck, FirebaseAppCheck.getInstance(aiApp))

        assertSame(FirebaseAuth.getInstance(), FirebaseModule.provideFirebaseAuth())
        assertSame(FirebaseFirestore.getInstance(), FirebaseModule.provideFirebaseStore())
        assertSame(FirebaseStorage.getInstance(), FirebaseModule.provideFirebaseStorage())
        assertSame(defaultPush, FirebaseModule.provideFirebaseMessaging())
        assertSame(FirebaseFunctions.getInstance("us-central1"), FirebaseModule.provideFirebaseFunctions())
        assertSame(defaultApp, FirebaseModule.provideFirebaseAuth().app)
        assertSame(defaultApp, FirebaseModule.provideFirebaseStore().app)
        assertSame(defaultApp, FirebaseModule.provideFirebaseStorage().app)
        // No token retrieval or generateContent: this verifies routing, not a live AI response.
    }
}
