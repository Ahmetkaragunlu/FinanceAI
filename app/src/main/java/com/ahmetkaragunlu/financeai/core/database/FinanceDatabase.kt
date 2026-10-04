
package com.ahmetkaragunlu.financeai.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ahmetkaragunlu.financeai.core.database.converter.Converters
import com.ahmetkaragunlu.financeai.feature.aichat.data.local.AiMessageDao
import com.ahmetkaragunlu.financeai.feature.aichat.data.local.AiMessageEntity
import com.ahmetkaragunlu.financeai.feature.budget.data.local.BudgetDao
import com.ahmetkaragunlu.financeai.feature.budget.data.local.BudgetEntity
import com.ahmetkaragunlu.financeai.feature.schedule.data.local.ScheduledTransactionDao
import com.ahmetkaragunlu.financeai.feature.schedule.data.local.ScheduledTransactionEntity
import com.ahmetkaragunlu.financeai.feature.transaction.data.local.TransactionDao
import com.ahmetkaragunlu.financeai.feature.transaction.data.local.TransactionEntity


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
    abstract fun budgetDao(): BudgetDao
    abstract fun aiMessageDao(): AiMessageDao
}
