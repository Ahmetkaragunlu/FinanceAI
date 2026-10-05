package com.ahmetkaragunlu.financeai.feature.budget.data.repository

import com.ahmetkaragunlu.financeai.core.session.local.entity.ActiveAccountRow

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.session.*
import com.ahmetkaragunlu.financeai.core.sync.*
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.*
import com.ahmetkaragunlu.financeai.feature.budget.domain.error.BudgetException
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BudgetRepositoryImplTest {
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
            repository.updateBudget(first.copy(amount = 20.75))
            val edited = repository.observeBudgets().first().single()
            assertEquals(id.toInt(), edited.id)
            assertEquals(first.firestoreId, edited.firestoreId)
            assertEquals(20.75, edited.amount, 0.0)
            assertEquals(1, database.syncRecordDao().pending("A").size)
        } finally { database.close(); WorkManagerTestInitHelper.closeWorkDatabase() }
    }
}
