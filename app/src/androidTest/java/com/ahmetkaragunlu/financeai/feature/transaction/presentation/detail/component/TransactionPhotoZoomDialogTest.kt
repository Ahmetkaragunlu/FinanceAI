package com.ahmetkaragunlu.financeai.feature.transaction.presentation.detail.component

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.media.testing.ReceiptImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TransactionPhotoZoomDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun changeDeleteAndCloseKeepTheirIndependentCallbacks() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val events = mutableListOf<String>()
        ReceiptImage(context).use { image ->
            compose.setContent {
                MaterialTheme {
                    TransactionPhotoZoomDialog(image.file.path, { events += "close" },
                        { events += "change" }, { events += "delete" })
                }
            }
            compose.onNodeWithContentDescription(context.getString(R.string.full_screen_photo_desc))
                .assertIsDisplayed()
            compose.onNodeWithText(context.getString(R.string.change)).performClick()
            compose.onNodeWithText(context.getString(R.string.delete)).performClick()
            compose.runOnIdle { assertEquals(listOf("change", "delete"), events) }
            compose.onNodeWithContentDescription(context.getString(R.string.close)).performClick()
            compose.runOnIdle { assertEquals(listOf("change", "delete", "close"), events) }
        }
    }
}
