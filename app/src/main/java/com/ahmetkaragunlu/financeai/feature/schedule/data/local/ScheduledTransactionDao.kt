package com.ahmetkaragunlu.financeai.feature.schedule.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledTransaction(transaction: ScheduledTransactionEntity): Long

    @Update
    suspend fun updateScheduledTransaction(transaction: ScheduledTransactionEntity)

    @Delete
    suspend fun deleteScheduledTransaction(transaction: ScheduledTransactionEntity)

    @Query("SELECT * FROM scheduled_transactions_table ORDER BY scheduledDate ASC")
    fun observeScheduledTransactions(): Flow<List<ScheduledTransactionEntity>>

    @Query("SELECT * FROM scheduled_transactions_table WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getScheduledTransactionByFirestoreId(firestoreId: String): ScheduledTransactionEntity?

    @Query("SELECT * FROM scheduled_transactions_table WHERE id = :localId LIMIT 1")
    suspend fun getScheduledTransactionById(localId: Long): ScheduledTransactionEntity?

    @Query("SELECT * FROM scheduled_transactions_table WHERE syncedToFirebase = 0")
    fun observeUnsyncedScheduledTransactions(): Flow<List<ScheduledTransactionEntity>>
}
