package com.ahmetkaragunlu.financeai.feature.transaction.data.remote

import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class FirestoreTransactionMapperTest {
    @Test
    fun `financial and location fields retain the existing remote contract`() {
        val transaction = Transaction(
            id = 73, firestoreId = "remote-id", currencyCode = "USD", amount = 152.75,
            transaction = TransactionType.EXPENSE, category = CategoryType.FOOD,
            note = "Receipt", date = 1_750_000_000_000L,
            photoUri = "/private/local-receipt.jpg", locationFull = "Full address",
            locationShort = "District", latitude = 41.01, longitude = 28.97,
            syncedToFirebase = true
        )

        assertEquals(
            mapOf(
                "currencyCode" to "USD", "amountMinor" to 15275L, "amount" to 152.75, "transaction" to "EXPENSE", "note" to "Receipt",
                "date" to 1_750_000_000_000L, "category" to "FOOD",
                "locationFull" to "Full address", "locationShort" to "District",
                "latitude" to 41.01, "longitude" to 28.97
            ),
            transaction.toFirebaseMap()
        )
    }

    @Test
    fun `absent optional values remain explicit nulls for merge writes`() {
        val transaction = Transaction(
            currencyCode = "USD", amount = 5.50, transaction = TransactionType.INCOME,
            category = CategoryType.SALARY, date = 0L
        )

        val fields = transaction.toFirebaseMap()

        assertEquals(setOf("locationFull", "locationShort", "latitude", "longitude"),
            fields.filterValues { it == null }.keys)
        assertEquals("", fields["note"])
    }
}
