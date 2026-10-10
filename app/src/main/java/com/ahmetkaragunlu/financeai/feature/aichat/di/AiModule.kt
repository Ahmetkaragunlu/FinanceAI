package com.ahmetkaragunlu.financeai.feature.aichat.di

import android.content.Context
import com.ahmetkaragunlu.financeai.BuildConfig
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.security.AppCheckInstaller
import com.ahmetkaragunlu.financeai.feature.aichat.data.remote.generation.AiFirebaseConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import com.ahmetkaragunlu.financeai.feature.aichat.data.remote.generation.AiRequestExecutor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {
    private const val AI_APP_NAME = "finance-ai"

    @Provides
    @Singleton
    @AiFirebaseApp
    fun provideAiAppCheck(@AiFirebaseApp app: FirebaseApp): FirebaseAppCheck = FirebaseAppCheck.getInstance(app)

    @Provides
    @Singleton
    @AiFirebaseApp
    fun provideAiFirebaseApp(@ApplicationContext context: Context): FirebaseApp {
        val options = AiFirebaseConfig.of(
            projectId = BuildConfig.AI_FIREBASE_PROJECT_ID,
            applicationId = BuildConfig.AI_FIREBASE_APP_ID,
            apiKey = BuildConfig.AI_FIREBASE_API_KEY
        ).toOptions()
        val defaultApp = FirebaseApp.getInstance()
        require(options.projectId != defaultApp.options.projectId) { "AI must use its separate Firebase project." }
        val app = FirebaseApp.getApps(context).firstOrNull { it.name == AI_APP_NAME }
            ?: FirebaseApp.initializeApp(context, options, AI_APP_NAME)
        check(app.options == options) { "Named AI app has a different Firebase configuration." }
        // AI-only app: do not enable automatic collection for unrelated SDKs such as FCM.
        app.isDataCollectionDefaultEnabled = false
        AppCheckInstaller.install(app)
        FirebaseAppCheck.getInstance(app).setTokenAutoRefreshEnabled(true)
        return app
    }

    @Provides
    @Singleton
    fun provideGenerativeModel(
        @ApplicationContext context: Context,
        @AiFirebaseApp app: FirebaseApp
    ): GenerativeModel =
        FirebaseAI.getInstance(app, GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                thinkingConfig = thinkingConfig { thinkingLevel = ThinkingLevel.LOW }
            },
            requestOptions = RequestOptions(timeoutInMillis = AiRequestExecutor.ATTEMPT_TIMEOUT_MILLIS),
            systemInstruction = content { text(context.getString(R.string.ai_detailed_system_instruction)) }
        )

}
