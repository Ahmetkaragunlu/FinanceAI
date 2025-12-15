
package com.ahmetkaragunlu.financeai.roomdb.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ahmetkaragunlu.financeai.roomdb.converters.Converters
import com.ahmetkaragunlu.financeai.roomdb.dao.AiMessageDao
import com.ahmetkaragunlu.financeai.roomdb.dao.BudgetDao
import com.ahmetkaragunlu.financeai.roomdb.dao.ScheduledTransactionDao
import com.ahmetkaragunlu.financeai.roomdb.dao.TransactionDao
import com.ahmetkaragunlu.financeai.roomdb.entitiy.AiMessageEntity
import com.ahmetkaragunlu.financeai.roomdb.entitiy.BudgetEntity
import com.ahmetkaragunlu.financeai.roomdb.entitiy.ScheduledTransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.entitiy.TransactionEntity


@Database(
    entities = [
        TransactionEntity::class,
        ScheduledTransactionEntity::class,
        BudgetEntity::class,
        AiMessageEntity::class
               ],
    version = 13,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun scheduledTransactionDao(): ScheduledTransactionDao
    abstract fun BudgetDao(): BudgetDao
    abstract fun aiMessageDao(): AiMessageDao
}
