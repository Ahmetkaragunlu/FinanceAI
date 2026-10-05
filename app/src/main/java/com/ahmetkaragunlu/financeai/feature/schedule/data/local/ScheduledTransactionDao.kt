package com.ahmetkaragunlu.financeai.feature.schedule.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledTransaction(transaction: ScheduledTransactionEntity): Long

    @Delete
    suspend fun deleteScheduledTransaction(transaction: ScheduledTransactionEntity)

    @Query("SELECT * FROM scheduled_transactions_table WHERE ownerId = (SELECT ownerId FROM active_account WHERE id = 0) ORDER BY scheduledDate ASC")
    fun observeScheduledTransactions(): Flow<List<ScheduledTransactionEntity>>

    @Query("SELECT * FROM scheduled_transactions_table WHERE ownerId = (SELECT ownerId FROM active_account WHERE id = 0) AND firestoreId = :firestoreId LIMIT 1")
    suspend fun getScheduledTransactionByFirestoreId(firestoreId: String): ScheduledTransactionEntity?

    @Query("SELECT * FROM scheduled_transactions_table WHERE ownerId = (SELECT ownerId FROM active_account WHERE id = 0) AND id = :localId LIMIT 1")
    suspend fun getScheduledTransactionById(localId: Long): ScheduledTransactionEntity?

}
