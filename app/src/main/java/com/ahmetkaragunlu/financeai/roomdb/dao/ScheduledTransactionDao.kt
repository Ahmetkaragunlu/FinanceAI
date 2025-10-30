package com.ahmetkaragunlu.financeai.roomdb.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ahmetkaragunlu.financeai.roomdb.entitiy.ScheduledTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledTransaction(transaction: ScheduledTransactionEntity)

    @Delete
    suspend fun deleteScheduledTransaction(transaction: ScheduledTransactionEntity)

    @Query("SELECT * FROM scheduled_transactions_table ORDER BY scheduledDate ASC")
    fun getAllScheduledTransactions(): Flow<List<ScheduledTransactionEntity>>

    @Query("SELECT * FROM scheduled_transactions_table WHERE scheduledDate <= :currentTime")
    fun getPendingScheduledTransactions(currentTime: Long): Flow<List<ScheduledTransactionEntity>>
}
