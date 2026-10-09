package com.ahmetkaragunlu.financeai.feature.aichat.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AiFirebaseConfigTest {
    @Test
    fun diagnosticsNeverIncludeTheClientKey() {
        val config = AiFirebaseConfig.of("ai-project", "ai-app", "synthetic-client-key")
        val diagnostic = config.toString()
        assertFalse(diagnostic.contains("synthetic-client-key"))
        assertTrue(diagnostic.contains("apiKey=[REDACTED]"))
        assertTrue(diagnostic.contains("ai-project"))
    }

    @Test
    fun retainsTheThreeClientValues() {
        val config = AiFirebaseConfig.of("ai-project", "ai-app", "client-key")
        assertEquals("ai-project", config.projectId)
        assertEquals("ai-app", config.applicationId)
        assertEquals("client-key", config.apiKey)
    }

    @Test
    fun blankClientValuesAreRejected() {
        for (values in
            listOf(
                Triple("", "ai-app", "client-key"),
                Triple("ai-project", "", "client-key"),
                Triple("ai-project", "ai-app", ""),
                Triple(" \t", "ai-app", "client-key"),
                Triple("ai-project", " \t", "client-key"),
                Triple("ai-project", "ai-app", " \t"),
            )) {
            try {
                AiFirebaseConfig.of(values.first, values.second, values.third)
                fail("Blank AI Firebase value accepted")
            } catch (error: IllegalArgumentException) {
                assertFalse(error.message.orEmpty().contains("client-key"))
            }
        }
    }
}
