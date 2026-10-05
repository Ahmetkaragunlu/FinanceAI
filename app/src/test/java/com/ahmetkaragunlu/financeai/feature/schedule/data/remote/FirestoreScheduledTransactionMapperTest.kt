package com.ahmetkaragunlu.financeai.feature.schedule.data.remote

import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class FirestoreScheduledTransactionMapperTest {
    @Test
    fun `scheduled date notification flags and nullable note keep their wire names`() {
        val transaction = ScheduledTransaction(
            id = 48, firestoreId = "remote-schedule", currencyCode = "USD", amount = 27.25,
            type = TransactionType.EXPENSE, category = CategoryType.FOOD,
            note = null, scheduledDate = 1_750_000_000_000L,
            notificationSent = true, expirationNotificationSent = false,
            photoUri = "/private/local-photo.jpg", latitude = 41.0, longitude = 29.0
        )

        assertEquals(
            mapOf(
                "currencyCode" to "USD", "amountMinor" to 2725L, "amount" to 27.25, "type" to "EXPENSE", "category" to "FOOD",
                "note" to null, "scheduledDate" to 1_750_000_000_000L,
                "expirationNotificationSent" to false, "notificationSent" to true,
                "locationFull" to null, "locationShort" to null,
                "latitude" to 41.0, "longitude" to 29.0
            ),
            transaction.toFirebaseMap()
        )
    }
}
