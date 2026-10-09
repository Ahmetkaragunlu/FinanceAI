package com.ahmetkaragunlu.financeai.core.ui.component

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import java.util.Locale
import org.junit.Rule
import org.junit.Test

class PercentageTextTest {
    @get:Rule val compose = createComposeRule()
    @Test fun uiConfigurationControlsPercentageLanguageRatherThanAnUnrelatedProcessLocale() {
        val tr = Configuration().apply { setLocales(LocaleList(Locale.forLanguageTag("tr-TR"))) }
        val en = Configuration().apply { setLocales(LocaleList(Locale.US)) }
        compose.setContent {
            CompositionLocalProvider(LocalConfiguration provides tr) { Text(87.5.formatAsUiPercentage()) }
            CompositionLocalProvider(LocalConfiguration provides en) { Text(87.formatAsUiPercentage()) }
        }
        compose.onNodeWithText("%88").assertIsDisplayed()
        compose.onNodeWithText("87%").assertIsDisplayed()
    }
}
