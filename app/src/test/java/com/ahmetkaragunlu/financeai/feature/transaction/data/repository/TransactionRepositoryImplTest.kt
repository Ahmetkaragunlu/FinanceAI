package com.ahmetkaragunlu.financeai.feature.transaction.data.repository

import com.ahmetkaragunlu.financeai.feature.transaction.data.local.TransactionDao
import com.ahmetkaragunlu.financeai.feature.transaction.data.local.TransactionEntity
import com.ahmetkaragunlu.financeai.feature.transaction.data.mapper.toDomain
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryExpense
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TransactionRepositoryImplTest {
    @Test
    fun `insert returns generated local identity and preserves the offline write`() = runBlocking {
        val dao = RecordingTransactionDao()
        val repository = TransactionRepositoryImpl(dao, Dispatchers.Unconfined)
        val row = row(id = 0).copy(syncedToFirebase = false)

        assertEquals(73L, repository.insertTransaction(row.toDomain()))
        assertEquals(row, dao.inserted)
    }

    @Test
    fun `observable reads preserve source order and suppress equal consecutive results`() = runBlocking {
        val rows = listOf(row(9), row(4))
        val changed = listOf(rows[1], rows[0])
        val dao = RecordingTransactionDao().apply {
            allRows = flowOf(rows, rows.map { it.copy() }, changed)
        }
        val repository = TransactionRepositoryImpl(dao, Dispatchers.Unconfined)

        assertEquals(
            listOf(rows.map { it.toDomain() }, changed.map { it.toDomain() }),
            repository.observeTransactions().toList()
        )
    }

    @Test
    fun `detail reads preserve absent and present values and requested identity`() = runBlocking {
        val row = row(19)
        val dao = RecordingTransactionDao().apply { detailRows = flowOf(null, row) }
        val repository = TransactionRepositoryImpl(dao, Dispatchers.Unconfined)

        assertEquals(listOf(null, row.toDomain()), repository.observeTransactionById(19).toList())
        assertEquals(19, dao.requestedId)
    }

    @Test
    fun `local storage read failure is propagated rather than converted to an empty list`() {
        val failure = IllegalStateException("database read failed")
        val dao = RecordingTransactionDao().apply {
            allRows = flow { throw failure }
        }
        val repository = TransactionRepositoryImpl(dao, Dispatchers.Unconfined)

        val thrown = assertThrows(IllegalStateException::class.java) {
            runBlocking { repository.observeTransactions().toList() }
        }
        assertEquals(failure.message, thrown.message)
    }

    private fun row(id: Int) = TransactionEntity(
        id = id, firestoreId = "remote-$id", amount = 25.75,
        transaction = TransactionType.EXPENSE, category = CategoryType.FOOD,
        note = "Yemek", date = 1_750_000_000_000, syncedToFirebase = true
    )

    private class RecordingTransactionDao : TransactionDao {
        var allRows: Flow<List<TransactionEntity>> = flowOf(emptyList())
        var detailRows: Flow<TransactionEntity?> = flowOf(null)
        var inserted: TransactionEntity? = null
        var requestedId: Int? = null

        override suspend fun insertTransaction(transaction: TransactionEntity): Long {
            inserted = transaction
            return 73
        }

        override fun observeTransactions(): Flow<List<TransactionEntity>> = allRows
        override fun observeTransactionById(id: Int): Flow<TransactionEntity?> {
            requestedId = id
            return detailRows
        }

        override suspend fun deleteTransaction(transaction: TransactionEntity): Unit = unexpected()
        override suspend fun updateTransaction(transaction: TransactionEntity): Unit = unexpected()
        override suspend fun getAllTransactionsOneShot(): List<TransactionEntity> = unexpected()
        override fun observeTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> = unexpected()
        override fun observeTransactionsByCategoryAndDate(category: CategoryType, startDate: Long, endDate: Long): Flow<List<TransactionEntity>> = unexpected()
        override fun observeTotalIncomeByDateRange(startDate: Long, endDate: Long): Flow<Double?> = unexpected()
        override fun observeTotalExpenseByDateRange(startDate: Long, endDate: Long): Flow<Double?> = unexpected()
        override fun observeTransactionsByTypeAndDate(transactionType: TransactionType, startDate: Long, endDate: Long): Flow<List<TransactionEntity>> = unexpected()
        override fun observeCategoryExpensesByTypeAndDateRange(transactionType: TransactionType, startDate: Long, endDate: Long): Flow<List<CategoryExpense>> = unexpected()
        override suspend fun getTransactionByFirestoreId(firestoreId: String): TransactionEntity? = unexpected()
        override fun observeUnsyncedTransactions(): Flow<List<TransactionEntity>> = unexpected()

        private fun unexpected(): Nothing = error("Unexpected DAO call")
    }
}
