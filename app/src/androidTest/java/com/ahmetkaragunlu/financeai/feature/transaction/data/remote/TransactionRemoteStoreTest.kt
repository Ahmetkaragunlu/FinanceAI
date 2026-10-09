package com.ahmetkaragunlu.financeai.feature.transaction.data.remote

import com.ahmetkaragunlu.financeai.core.database.testing.AccountDatabaseFixture
import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.ahmetkaragunlu.financeai.core.media.remote.PhotoRemoteCache
import dagger.Lazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TransactionRemoteStoreTest {
    private val legacy = mapOf<String, Any?>("amount" to 12.50, "transaction" to "EXPENSE", "category" to "FOOD",
        "note" to "receipt", "date" to 123L, "locationFull" to "full", "locationShort" to "short",
        "latitude" to 41.0, "longitude" to 29.0)

    @Test fun legacyAndLongMinorValuesApplyWithStableLocalIdentityAndFeatureFields() = runBlocking {
        AccountDatabaseFixture().use { f ->
            f.activate()
            val cache = PhotoRemoteCache(f.context, Lazy { error("No media network expected") }, f.session, Dispatchers.IO)
            val store = TransactionRemoteStore(f.database, cache)
            val account = f.session.requireAccount()
            val first = store.normalize(legacy + ("localPhotoUri" to "/injected"), account)
            assertEquals(1250L, first["amountMinor"])
            assertEquals("USD", first["currencyCode"])
            assertFalse(first.containsKey("localPhotoUri"))
            store.apply(account, "receipt", store.prepare(account, "receipt", first))
            val original = checkNotNull(f.database.transactionDao().getTransactionByFirestoreId("receipt"))
            assertEquals(1250L, original.amountMinor)
            assertEquals("full", original.locationFull)
            assertEquals(41.0, checkNotNull(original.latitude), 0.0)
            assertTrue(original.syncedToFirebase)
            val changed = store.normalize(legacy + mapOf("amountMinor" to 12345678901234L, "amount" to 1.0, "note" to "changed", "date" to 456L), account)
            assertEquals(12345678901234L, changed["amountMinor"])
            store.apply(account, "receipt", changed)
            val updated = checkNotNull(f.database.transactionDao().getTransactionByFirestoreId("receipt"))
            assertEquals(original.id, updated.id)
            assertEquals(12345678901234L, updated.amountMinor)
            assertEquals(456L, updated.date)
            assertEquals("changed", updated.note)
            store.apply(account, "receipt", null)
            store.apply(account, "receipt", null)
            assertNull(f.database.transactionDao().getTransactionByFirestoreId("receipt"))
        }
    }

    @Test fun invalidCurrencyEnumFractionalAndUnsupportedMoneyNeverReplaceTheExistingRow() = runBlocking {
        AccountDatabaseFixture().use { f ->
            f.activate()
            val store = TransactionRemoteStore(f.database, PhotoRemoteCache(f.context, Lazy { error("No media network expected") }, f.session, Dispatchers.IO))
            val account = f.session.requireAccount()
            store.apply(account, "receipt", store.normalize(legacy, account))
            val original = f.database.transactionDao().getTransactionByFirestoreId("receipt")
            for (patch in listOf(mapOf("currencyCode" to "EUR"), mapOf("transaction" to "unknown"),
                mapOf("category" to "unknown"), mapOf("amountMinor" to 1.5), mapOf("amountMinor" to Double.MAX_VALUE),
                mapOf("amountMinor" to 9007199254740993L))) {
                try { store.apply(account, "receipt", legacy + patch); fail("Expected invalid remote record") }
                catch (e: Exception) { assertTrue(e is DataAccessException.InvalidRemoteData || e is IllegalArgumentException || e is ArithmeticException) }
                assertEquals(original, f.database.transactionDao().getTransactionByFirestoreId("receipt"))
            }
        }
    }
}
