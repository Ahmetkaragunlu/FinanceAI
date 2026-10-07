package com.ahmetkaragunlu.financeai.feature.aichat.data.remote

import com.google.firebase.FirebaseOptions

/** Firebase client configuration; never a Gemini key or a server credential. */
internal data class AiFirebaseConfig(
    val projectId: String,
    val applicationId: String,
    val apiKey: String
) {
    fun toOptions(): FirebaseOptions = FirebaseOptions.Builder()
        .setProjectId(projectId).setApplicationId(applicationId).setApiKey(apiKey).build()

    companion object {
        fun of(projectId: String, applicationId: String, apiKey: String): AiFirebaseConfig {
            require(projectId.isNotBlank()) { "AI Firebase project id is missing." }
            require(applicationId.isNotBlank()) { "AI Firebase app id is missing." }
            require(apiKey.isNotBlank()) { "AI Firebase client API key is missing." }
            return AiFirebaseConfig(projectId, applicationId, apiKey)
        }
    }
}
