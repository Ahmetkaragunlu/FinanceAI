package com.ahmetkaragunlu.financeai.feature.transaction.data.mapper

import com.ahmetkaragunlu.financeai.feature.transaction.data.local.entity.TransactionEntity
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionMapperTest {
    @Test
    fun `persisted identity financial data media and sync status survive both directions`() {
        val row = TransactionEntity(
            id = 73, firestoreId = "remote-transaction", currencyCode = "USD", amountMinor = 15275L,
            transaction = TransactionType.EXPENSE, category = CategoryType.FOOD,
            note = "Yemek", date = 1_750_000_000_000,
            photoUri = "file:///receipt.jpg", locationFull = "Tam adres", locationShort = "Konum",
            latitude = 41.01, longitude = 28.97, syncedToFirebase = true
        )

        assertEquals(row, row.toDomain().toEntity())
        assertEquals(row.copy(syncedToFirebase = false), row.toDomain().copy(syncedToFirebase = false).toEntity())
    }

    @Test
    fun `new offline record does not gain an id media or synced flag during mapping`() {
        val row = TransactionEntity(
            currencyCode = "USD", amountMinor = 0L, transaction = TransactionType.INCOME,
            category = CategoryType.SALARY, date = 0
        )

        assertEquals(row, row.toDomain().toEntity())
    }
}
