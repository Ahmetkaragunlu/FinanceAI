package com.ahmetkaragunlu.financeai.feature.budget.data.remote

import com.ahmetkaragunlu.financeai.core.database.testing.AccountDatabaseFixture
import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.BudgetType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BudgetRemoteStoreTest {
    private val legacy = mapOf<String, Any?>("amount" to 12.50, "budgetType" to "CATEGORY_AMOUNT", "category" to "FOOD")
    @Test fun legacyAndMinorBudgetFieldsUpdateTheSameLocalRuleAndStayAccountScoped() = runBlocking {
        AccountDatabaseFixture().use { f ->
            f.activate()
            val store = BudgetRemoteStore(f.database)
            val account = f.session.requireAccount()
            val first = store.normalize(legacy, account)
            assertEquals(1250L, first["amountMinor"])
            store.apply(account, "rule", first)
            val original = checkNotNull(f.database.budgetDao().getBudgetByFirestoreId("rule"))
            val updatedPayload = store.normalize(legacy + mapOf("amountMinor" to 12345678901234L,
                "budgetType" to "CATEGORY_PERCENTAGE", "limitPercentage" to 12.5), account)
            store.apply(account, "rule", updatedPayload)
            val updated = checkNotNull(f.database.budgetDao().getBudgetByFirestoreId("rule"))
            assertEquals(original.id, updated.id)
            assertEquals(12345678901234L, updated.amountMinor)
            assertEquals(BudgetType.CATEGORY_PERCENTAGE, updated.budgetType)
            assertEquals(12.5, checkNotNull(updated.limitPercentage), 0.0)
            assertTrue(updated.syncedToFirebase)
            f.activate("B", "EUR")
            store.apply(f.session.requireAccount(), "rule", store.normalize(legacy, f.session.requireAccount()))
            store.apply(f.session.requireAccount(), "rule", null)
            f.activate()
            assertEquals(updated, f.database.budgetDao().getBudgetByFirestoreId("rule"))
            store.apply(account, "rule", null)
            assertNull(f.database.budgetDao().getBudgetByFirestoreId("rule"))
        }
    }
    @Test fun invalidCurrencyEnumAndFractionalMinorValuesDoNotReplaceABudget() = runBlocking {
        AccountDatabaseFixture().use { f ->
            f.activate()
            val store = BudgetRemoteStore(f.database)
            val account = f.session.requireAccount()
            store.apply(account, "rule", store.normalize(legacy, account))
            val original = f.database.budgetDao().getBudgetByFirestoreId("rule")
            for (patch in listOf(mapOf("currencyCode" to "EUR"), mapOf("budgetType" to "unknown"),
                mapOf("category" to "unknown"), mapOf("amountMinor" to 1.5))) {
                try { store.apply(account, "rule", legacy + patch); fail("Expected invalid remote budget") }
                catch (e: Exception) { assertTrue(e is DataAccessException.InvalidRemoteData || e is IllegalArgumentException || e is ArithmeticException) }
                assertEquals(original, f.database.budgetDao().getBudgetByFirestoreId("rule"))
            }
        }
    }
}
