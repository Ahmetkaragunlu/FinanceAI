package com.ahmetkaragunlu.financeai.feature.schedule.data.local.dao

import com.ahmetkaragunlu.financeai.core.database.testing.AccountDatabaseFixture
import com.ahmetkaragunlu.financeai.feature.schedule.data.local.entity.ScheduledTransactionEntity
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduledTransactionOneShotTest {
    @Test fun oneShotMatchesObservedOrderingAndNeverIncludesAnInactiveAccountsPlans(): Unit = runBlocking {
        AccountDatabaseFixture().use { f ->
            val dao = f.database.scheduledTransactionDao()
            for ((owner, id, date) in listOf(Triple("A", "later", 200L), Triple("B", "foreign", 50L), Triple("A", "first", 100L))) {
                dao.insertScheduledTransaction(ScheduledTransactionEntity(ownerId = owner, firestoreId = id, currencyCode = "USD",
                    amountMinor = 1000, type = TransactionType.EXPENSE, category = CategoryType.FOOD, note = null, scheduledDate = date))
            }
            assertTrue(dao.getScheduledTransactionsOneShot().isEmpty())
            f.activate()
            assertEquals(listOf("first", "later"), dao.getScheduledTransactionsOneShot().map { it.firestoreId })
            assertEquals(dao.observeScheduledTransactions().first(), dao.getScheduledTransactionsOneShot())
            f.activate("B")
            assertEquals(listOf("foreign"), dao.getScheduledTransactionsOneShot().map { it.firestoreId })
        }
    }
}
