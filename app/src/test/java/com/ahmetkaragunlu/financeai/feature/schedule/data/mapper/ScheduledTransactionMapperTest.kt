package com.ahmetkaragunlu.financeai.feature.schedule.data.mapper

import com.ahmetkaragunlu.financeai.feature.schedule.data.local.entity.ScheduledTransactionEntity
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduledTransactionMapperTest {
    @Test
    fun `schedule identity time notification flags and media survive mapping`() {
        val row = ScheduledTransactionEntity(
            id = 5_000_000_000, firestoreId = "remote-schedule", ownerId = "account-A", currencyCode = "USD", amountMinor = 12550L,
            type = TransactionType.EXPENSE, category = CategoryType.RENT, note = "Kira",
            scheduledDate = 1_800_000_000_000, expirationNotificationSent = true,
            notificationSent = true, photoUri = "file:///schedule.jpg",
            locationFull = "Tam adres", locationShort = "Konum", latitude = 40.9,
            longitude = 29.1, syncedToFirebase = true
        )

        assertEquals(row, row.toDomain().toEntity())
    }

    @Test
    fun `pending schedule retains absent note and media without enabling notifications`() {
        val row = ScheduledTransactionEntity(
            currencyCode = "USD", amountMinor = 4200L, type = TransactionType.INCOME, category = CategoryType.FREELANCE,
            note = null, scheduledDate = 0
        )

        assertEquals(row, row.toDomain().toEntity())
    }
}
