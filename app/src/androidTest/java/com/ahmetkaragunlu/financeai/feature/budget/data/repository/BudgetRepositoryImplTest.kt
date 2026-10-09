package com.ahmetkaragunlu.financeai.feature.budget.data.repository

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.database.testing.AccountDatabaseFixture
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.session.local.entity.ActiveAccountRow
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.ahmetkaragunlu.financeai.core.sync.local.PendingChanges
import com.ahmetkaragunlu.financeai.feature.budget.domain.error.BudgetException
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.Budget
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.BudgetType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BudgetRepositoryImplTest {
    private fun repository(f: AccountDatabaseFixture) = BudgetRepositoryImpl(
        f.database.budgetDao(), f.database, f.session, f.pending, f.scheduler
    )

    @Test fun oldAccountBudgetCannotBeObservedEditedOrDeletedAfterSwitchingAccounts() = runBlocking {
        AccountDatabaseFixture().use { f ->
            f.activate()
            val repository = repository(f)
            repository.insertBudget(Budget(budgetType = BudgetType.GENERAL_MONTHLY, amount = 100.0))
            val stored = repository.observeBudgets().first().single()
            f.activate("B", "EUR")
            assertTrue(repository.observeBudgets().first().isEmpty())
            assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.insertBudget(stored.copy(amount = 150.0)) } }
            assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.deleteBudget(stored) } }
            assertEquals(1, f.database.syncRecordDao().pending("A").size)
            f.activate()
            assertEquals(stored, repository.observeBudgets().first().single())
        }
    }

    @Test fun outboxFailureRollsBackBudgetChangesAndSuccessfulDeleteKeepsATombstone() = runBlocking {
        AccountDatabaseFixture().use { f ->
            f.activate()
            val repository = repository(f)
            repository.insertBudget(Budget(budgetType = BudgetType.GENERAL_MONTHLY, amount = 100.0))
            val stored = repository.observeBudgets().first().single()
            f.rejectSyncWrites()
            assertThrows(SQLiteException::class.java) { runBlocking { repository.insertBudget(stored.copy(amount = 150.0)) } }
            assertThrows(SQLiteException::class.java) { runBlocking { repository.deleteBudget(stored) } }
            assertEquals(stored, repository.observeBudgets().first().single())
            f.allowSyncWrites()
            repository.deleteBudget(stored)
            assertTrue(repository.observeBudgets().first().isEmpty())
            val pending = checkNotNull(f.database.syncRecordDao().get("A", "budgets", stored.firestoreId))
            assertTrue(pending.pendingDelete)
            assertNotNull(pending.mutationId)
        }
    }

    @Test fun duplicateBudgetIsRejectedAtomicallyAndEditKeepsIdentityAndFraction() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, Configuration.Builder().build())
        val database = Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java).build()
        try {
            val session = AccountSession()
            database.accountDao().setActive(ActiveAccountRow(ownerId = "A"))
            session.activate("A", "USD")
            val repository = BudgetRepositoryImpl(database.budgetDao(), database, session,
                PendingChanges(database), SyncScheduler(WorkManager.getInstance(context)))
            val draft = Budget(budgetType = BudgetType.CATEGORY_AMOUNT, category = CategoryType.FOOD, amount = 10.25)
            val id = repository.insertBudget(draft)
            try { repository.insertBudget(draft); fail("Duplicate budget accepted") }
            catch (_: BudgetException.DuplicateRule) { }
            val first = repository.observeBudgets().first().single()
            assertEquals("A_budget_FOOD", first.firestoreId)
            repository.insertBudget(first.copy(amount = 20.75))
            val edited = repository.observeBudgets().first().single()
            assertEquals(id.toInt(), edited.id)
            assertEquals(first.firestoreId, edited.firestoreId)
            assertEquals(20.75, edited.amount, 0.0)
            assertEquals(1, database.syncRecordDao().pending("A").size)
        } finally { database.close(); WorkManagerTestInitHelper.closeWorkDatabase() }
    }
}
