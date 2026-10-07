package com.ahmetkaragunlu.financeai.feature.aichat.data.remote

import java.io.StringReader
import org.junit.Assert.*
import org.junit.Test

class AiFirebaseConfigTest {
    private fun client(packageName: String, appId: String, key: String = "client-key") = """
        {"client_info":{"mobilesdk_app_id":"$appId","android_client_info":{"package_name":"$packageName"}},
         "api_key":[{"current_key":"$key"}]}
    """.trimIndent()
    private fun config(clients: String, project: String = "ai-project") = """
        {"project_info":{"project_id":"$project","storage_bucket":"unused-bucket"},"client":[$clients]}
    """.trimIndent()

    @Test fun selectsTheCorrectAndroidClientFromAFileWithMultipleApps() {
        val input = config(client("other.app", "other-app") + "," + client("finance.app", "ai-app"))
        val options = AiFirebaseConfig.read(StringReader(input), "finance.app")
        assertEquals("ai-project", options.projectId)
        assertEquals("ai-app", options.applicationId)
        assertEquals("client-key", options.apiKey)
    }

    @Test fun wrongPackageAndAmbiguousClientAreRejectedInsteadOfFallingBack() {
        for (clients in listOf(client("other.app", "other-app"),
            client("finance.app", "one") + "," + client("finance.app", "two"))) {
            try { AiFirebaseConfig.read(StringReader(config(clients)), "finance.app"); fail("Invalid client selection accepted") }
            catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun missingRequiredClientFieldsAreRejectedWithoutLeakingValues() {
        for (input in listOf(config(client("finance.app", "")),
            config(client("finance.app", "ai-app", "")), config(client("finance.app", "ai-app"), ""))) {
            try { AiFirebaseConfig.read(StringReader(input), "finance.app"); fail("Incomplete configuration accepted") }
            catch (error: IllegalArgumentException) { assertFalse(error.message.orEmpty().contains("client-key")) }
        }
    }

    @Test fun serverCredentialFileCannotBeTreatedAsClientConfiguration() {
        val input = """{"private_key":"not-a-client-file","project_info":{"project_id":"ai-project"}}"""
        try { AiFirebaseConfig.read(StringReader(input), "finance.app"); fail("Server credential accepted") }
        catch (error: IllegalArgumentException) { assertFalse(error.message.orEmpty().contains("not-a-client-file")) }
    }
}
