package com.ahmetkaragunlu.financeai.feature.auth.presentation.validation

import android.util.Patterns
import androidx.core.util.PatternsCompat
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthFormValidationParityTest {
    @Test
    fun installedAndroidAndCompatKeepTheSameEmailRule() {
        assertEquals(Patterns.EMAIL_ADDRESS.pattern(), PatternsCompat.EMAIL_ADDRESS.pattern())
        listOf("a@b.co", "User.Name+tag_1%2@example-domain.com", "", "a@b", " a@b.co", "a@b.co ",
            "ü@b.co", "a@-b.co", "a@b..co", "a".repeat(256) + "@b.co", "a".repeat(257) + "@b.co",
            "a@" + "b".repeat(65) + ".co", "a@" + "b".repeat(66) + ".co",
            "a@b." + "c".repeat(26), "a@b." + "c".repeat(27)).forEach {
            assertEquals(it, Patterns.EMAIL_ADDRESS.matcher(it).matches(), AuthFormValidation.isEmailValid(it))
        }
    }
}
