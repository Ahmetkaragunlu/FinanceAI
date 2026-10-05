package com.ahmetkaragunlu.financeai.feature.transaction.data.local.dao

import com.ahmetkaragunlu.financeai.feature.transaction.data.local.entity.TransactionEntity
import com.ahmetkaragunlu.financeai.feature.transaction.data.local.model.CategoryExpenseRow
import com.ahmetkaragunlu.financeai.feature.transaction.data.local.model.FinancialSummaryRow

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transaction_table WHERE ownerId = (SELECT ownerId FROM active_account WHERE id = 0) AND id = :id")
    fun observeTransactionById(id: Int): Flow<TransactionEntity?>

    @Query("SELECT * FROM transaction_table WHERE ownerId = (SELECT ownerId FROM active_account WHERE id = 0)")
    suspend fun getAllTransactionsOneShot(): List<TransactionEntity>
    @Query("SELECT * FROM transaction_table WHERE ownerId = (SELECT ownerId FROM active_account WHERE id = 0) ORDER BY date DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transaction_table WHERE ownerId = (SELECT ownerId FROM active_account WHERE id = 0) AND date >= :startDate AND date < :endDate ORDER BY date DESC")
    fun observeTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transaction_table WHERE ownerId = (SELECT ownerId FROM active_account WHERE id = 0) AND category = :category AND date >= :startDate AND date < :endDate ORDER BY date DESC")
    fun observeTransactionsByCategoryAndDate(category: CategoryType, startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN `transaction` = 'INCOME' THEN amountMinor ELSE 0 END), 0) AS incomeMinor,
               COALESCE(SUM(CASE WHEN `transaction` = 'EXPENSE' THEN amountMinor ELSE 0 END), 0) AS expenseMinor
        FROM transaction_table
        WHERE ownerId = (SELECT ownerId FROM active_account WHERE id = 0)
          AND date >= :startDate AND date < :endDate
    """)
    fun observeFinancialSummary(startDate: Long, endDate: Long): Flow<FinancialSummaryRow>

    @Query("SELECT SUM(amountMinor) FROM transaction_table WHERE ownerId = (SELECT ownerId FROM active_account WHERE id = 0) AND `transaction` = 'EXPENSE' AND date >= :startDate AND date < :endDate")
    fun observeTotalExpenseByDateRange(startDate: Long, endDate: Long): Flow<Long?>

    @Query("SELECT * FROM transaction_table WHERE ownerId = (SELECT ownerId FROM active_account WHERE id = 0) AND `transaction` = :transactionType AND date >= :startDate AND date < :endDate ORDER BY date DESC")
    fun observeTransactionsByTypeAndDate(transactionType: TransactionType, startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("""
    SELECT category, SUM(amountMinor) as totalMinor
    FROM transaction_table WHERE ownerId = (SELECT ownerId FROM active_account WHERE id = 0) AND `transaction` = :transactionType AND date >= :startDate AND date < :endDate
    GROUP BY category
""")
    fun observeCategoryExpensesByTypeAndDateRange(
        transactionType: TransactionType,
        startDate: Long,
        endDate: Long
    ): Flow<List<CategoryExpenseRow>>

    @Query("SELECT * FROM transaction_table WHERE ownerId = (SELECT ownerId FROM active_account WHERE id = 0) AND firestoreId = :firestoreId LIMIT 1")
    suspend fun getTransactionByFirestoreId(firestoreId: String): TransactionEntity?

}
