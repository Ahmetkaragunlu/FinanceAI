package com.ahmetkaragunlu.financeai.feature.aichat.data.remote

import com.google.firebase.FirebaseOptions
import com.google.gson.JsonParser
import java.io.Reader

/** Firebase client configuration; never a Gemini key or a server credential. */
internal data class AiFirebaseConfig(
    val projectId: String,
    val applicationId: String,
    val apiKey: String
) {
    fun toOptions(): FirebaseOptions = FirebaseOptions.Builder()
        .setProjectId(projectId).setApplicationId(applicationId).setApiKey(apiKey).build()

    companion object {
        fun read(reader: Reader, packageName: String): AiFirebaseConfig {
            val config = JsonParser.parseReader(reader).asJsonObject
            require(!config.has("private_key")) { "AI configuration must be a Firebase Android client file." }
            val projectId = config.getAsJsonObject("project_info")?.get("project_id")?.asString
            require(!projectId.isNullOrBlank()) { "AI Firebase project id is missing." }
            val clients =
                config.getAsJsonArray("client")?.map { it.asJsonObject }.orEmpty().filter {
                    it.getAsJsonObject("client_info")?.getAsJsonObject("android_client_info")
                        ?.get("package_name")?.asString == packageName
                }
            require(clients.size == 1) { "AI configuration must contain exactly one matching Android client." }
            val client = clients.single()
            val appId = client.getAsJsonObject("client_info")?.get("mobilesdk_app_id")?.asString
            val key = client.getAsJsonArray("api_key")
                ?.firstOrNull()?.asJsonObject?.get("current_key")?.asString
            require(!appId.isNullOrBlank() && !key.isNullOrBlank()) {
                "AI Firebase app id or client API key is missing."
            }
            return AiFirebaseConfig(projectId, appId, key)
        }
    }
}
