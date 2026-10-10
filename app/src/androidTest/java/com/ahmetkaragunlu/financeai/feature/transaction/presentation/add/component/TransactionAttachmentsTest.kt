package com.ahmetkaragunlu.financeai.feature.transaction.presentation.add.component

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.media.testing.ReceiptImage
import com.ahmetkaragunlu.financeai.feature.location.domain.model.LocationData
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TransactionAttachmentsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun emptyAttachmentsDispatchTheirOwnPickerCallbacks() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val events = mutableListOf<String>()
        compose.setContent {
            MaterialTheme {
                TransactionAttachments(null, null, { events += "location" }, { events += "photo" },
                    { events += "clear-location" }, { events += "clear-photo" })
            }
        }
        compose.onNodeWithText(context.getString(R.string.location_optional)).performClick()
        compose.onNodeWithText(context.getString(R.string.photo_optional)).performClick()
        compose.runOnIdle { assertEquals(listOf("location", "photo"), events) }
    }

    @Test
    fun selectedPhotoUsesRemovalWithoutReopeningThePhotoPicker() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val events = mutableListOf<String>()
        ReceiptImage(context).use { image ->
            compose.setContent {
                MaterialTheme {
                    TransactionAttachments(null, image.uri, { events += "location" },
                        { events += "photo" }, { events += "clear-location" }, { events += "clear-photo" })
                }
            }
            compose.onNodeWithContentDescription(context.getString(R.string.selected_photo)).performClick()
            compose.runOnIdle { assertEquals(emptyList<String>(), events) }
            compose.onNodeWithContentDescription(context.getString(R.string.remove_photo)).performClick()
            compose.runOnIdle { assertEquals(listOf("clear-photo"), events) }
        }
    }

    @Test
    fun selectedLocationCanBeOpenedAndClearedIndependentlyOfThePhoto() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val events = mutableListOf<String>()
        compose.setContent {
            MaterialTheme {
                TransactionAttachments(LocationData(41.0, 29.0, "Full address", "Short address"), null,
                    { events += "location" }, { events += "photo" },
                    { events += "clear-location" }, { events += "clear-photo" })
            }
        }
        compose.onNodeWithText("Short address").performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.remove_photo)).performClick()
        compose.runOnIdle { assertEquals(listOf("location", "clear-location"), events) }
    }
}
