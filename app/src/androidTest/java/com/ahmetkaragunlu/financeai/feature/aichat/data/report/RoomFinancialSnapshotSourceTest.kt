package com.ahmetkaragunlu.financeai.feature.aichat.data.report

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.session.local.entity.ActiveAccountRow
import com.ahmetkaragunlu.financeai.core.time.FinancePeriods
import com.ahmetkaragunlu.financeai.feature.budget.data.local.entity.BudgetEntity
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.BudgetType
import com.ahmetkaragunlu.financeai.feature.transaction.data.local.entity.TransactionEntity
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RoomFinancialSnapshotSourceTest {
    @Test fun snapshotReadsOnlyTheActiveAccountsCalendarMonthWithoutDeletingHistory() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java).build()
        try {
            val session = AccountSession().apply { activate("A", "USD") }
            database.accountDao().setActive(ActiveAccountRow(ownerId = "A"))
            val clock = Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneId.systemDefault())
            val month = FinancePeriods.month(clock)
            val rows = listOf(
                "previous" to month.start - 1,
                "first" to month.start,
                "last" to month.endExclusive - 1,
                "next" to month.endExclusive
            )
            for ((id, date) in rows) {
                database.transactionDao().insertTransaction(TransactionEntity(
                    ownerId = "A", firestoreId = id, amountMinor = 1000, currencyCode = "USD",
                    transaction = TransactionType.EXPENSE, category = CategoryType.FOOD, date = date
                ))
            }
            database.transactionDao().insertTransaction(TransactionEntity(
                ownerId = "B", firestoreId = "foreign", amountMinor = 99000, currencyCode = "USD",
                transaction = TransactionType.EXPENSE, category = CategoryType.FOOD, date = clock.millis()
            ))
            for (owner in listOf("A", "B")) {
                database.budgetDao().insertBudget(BudgetEntity(ownerId = owner, firestoreId = "budget-$owner",
                    amountMinor = 50000, currencyCode = "USD", budgetType = BudgetType.GENERAL_MONTHLY))
            }
            val source = RoomFinancialSnapshotSource(database, session, clock)
            val account = session.requireAccount()
            val snapshot = source.read(account)
            assertEquals(month.start, snapshot.monthStart)
            assertEquals(month.endExclusive, snapshot.monthEndExclusive)
            assertEquals(setOf("first", "last"), snapshot.transactions.map { it.firestoreId }.toSet())
            assertTrue(snapshot.transactions.all { it.ownerId == "A" })
            assertEquals("A", snapshot.budgets.single().ownerId)
            assertEquals(4, database.transactionDao().getAllTransactionsOneShot().size)
            session.activate("B", "USD")
            database.accountDao().setActive(ActiveAccountRow(ownerId = "B"))
            try { source.read(account); fail("Stale snapshot accepted") }
            catch (_: CancellationException) { }
        } finally { database.close() }
    }
}
