package com.ahmetkaragunlu.financeai.feature.transaction.data.remote

import com.ahmetkaragunlu.financeai.core.database.testing.AccountDatabaseFixture
import com.ahmetkaragunlu.financeai.core.media.remote.PhotoRemoteCache
import com.ahmetkaragunlu.financeai.core.sync.contract.SyncPayload
import com.ahmetkaragunlu.financeai.core.sync.local.entity.SyncRecord
import com.ahmetkaragunlu.financeai.feature.transaction.data.local.entity.TransactionEntity
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class TransactionRemotePhotoTest {
    @Test
    fun sharedPreparationAndSelectionPreserveIdentityAndDoNotPersistTheInternalPathKey(): Unit =
        runBlocking {
            AccountDatabaseFixture().use { f ->
                f.activate()
                val file =
                    File.createTempFile("transaction-photo-test-", ".jpg", f.context.cacheDir)
                try {
                    val id = f.database.transactionDao().insertTransaction(
                        TransactionEntity(
                            ownerId = "A",
                            firestoreId = "receipt",
                            currencyCode = "USD",
                            amountMinor = 1000,
                            transaction = TransactionType.EXPENSE,
                            category = CategoryType.FOOD,
                            date = 100,
                            photoUri = file.path
                        )
                    )
                    val metadata = mapOf(
                        "photoStorageUrl" to "https://example.test/receipt",
                        "photoVersion" to "v1"
                    )
                    f.database.syncRecordDao().save(
                        SyncRecord(
                            "A",
                            "transactions",
                            "receipt",
                            basePayload = SyncPayload.encode(metadata)
                        )
                    )
                    val cache = PhotoRemoteCache(
                        f.context,
                        { error("No media download expected") },
                        f.session,
                        Dispatchers.IO
                    )
                    val store = TransactionRemoteStore(f.database, cache)
                    val account = f.session.requireAccount()
                    val normalized = store.normalize(
                        metadata + mapOf(
                            "amountMinor" to 2550L,
                            "currencyCode" to "USD",
                            "transaction" to "EXPENSE",
                            "category" to "FOOD",
                            "date" to 150L,
                            "localPhotoUri" to "/injected.jpg"
                        ), account
                    )
                    assertFalse(normalized.containsKey("localPhotoUri"))
                    val prepared = store.prepare(account, "receipt", normalized)
                    assertEquals(file.path, prepared["localPhotoUri"])
                    store.apply(account, "receipt", prepared)
                    val row = checkNotNull(
                        f.database.transactionDao().getTransactionByFirestoreId("receipt")
                    )
                    assertEquals(id.toInt(), row.id)
                    assertEquals(2550L, row.amountMinor)
                    assertEquals(file.path, row.photoUri)
                    val removed = prepared + mapOf("photoRemoved" to true)
                    assertSame(removed, store.prepare(account, "receipt", removed))
                    store.apply(account, "receipt", removed)
                    assertNull(
                        f.database.transactionDao().getTransactionByFirestoreId("receipt")?.photoUri
                    )
                } finally {
                    file.delete()
                }
            }
        }
}
