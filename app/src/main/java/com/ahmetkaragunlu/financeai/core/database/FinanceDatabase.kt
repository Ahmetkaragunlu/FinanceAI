
package com.ahmetkaragunlu.financeai.core.database

import com.ahmetkaragunlu.financeai.core.session.local.dao.AccountDao
import com.ahmetkaragunlu.financeai.core.session.local.entity.AccountPreferences
import com.ahmetkaragunlu.financeai.core.session.local.entity.ActiveAccountRow
import com.ahmetkaragunlu.financeai.core.sync.local.dao.SyncRecordDao
import com.ahmetkaragunlu.financeai.core.sync.local.entity.SyncRecord

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ahmetkaragunlu.financeai.core.media.local.entity.PhotoOperation
import com.ahmetkaragunlu.financeai.core.media.local.dao.PhotoOperationDao
import com.ahmetkaragunlu.financeai.core.database.converter.Converters
import com.ahmetkaragunlu.financeai.fcm.data.local.entity.PushEvent
import com.ahmetkaragunlu.financeai.fcm.data.local.dao.PushEventDao
import com.ahmetkaragunlu.financeai.fcm.data.local.entity.TokenOperation
import com.ahmetkaragunlu.financeai.fcm.data.local.dao.TokenOperationDao
import com.ahmetkaragunlu.financeai.feature.schedule.data.local.entity.ReminderState
import com.ahmetkaragunlu.financeai.feature.schedule.data.local.dao.ReminderStateDao
import com.ahmetkaragunlu.financeai.feature.schedule.data.local.entity.ScheduleCommand
import com.ahmetkaragunlu.financeai.feature.schedule.data.local.dao.ScheduleCommandDao
import com.ahmetkaragunlu.financeai.feature.aichat.data.local.dao.AiMessageDao
import com.ahmetkaragunlu.financeai.feature.aichat.data.local.entity.AiMessageEntity
import com.ahmetkaragunlu.financeai.feature.budget.data.local.dao.BudgetDao
import com.ahmetkaragunlu.financeai.feature.budget.data.local.entity.BudgetEntity
import com.ahmetkaragunlu.financeai.feature.schedule.data.local.dao.ScheduledTransactionDao
import com.ahmetkaragunlu.financeai.feature.schedule.data.local.entity.ScheduledTransactionEntity
import com.ahmetkaragunlu.financeai.feature.transaction.data.local.dao.TransactionDao
import com.ahmetkaragunlu.financeai.feature.transaction.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        ScheduledTransactionEntity::class,
        BudgetEntity::class,
        AiMessageEntity::class, AccountPreferences::class, ActiveAccountRow::class, SyncRecord::class,
        ReminderState::class, ScheduleCommand::class, PhotoOperation::class, PushEvent::class, TokenOperation::class
               ],
    version = 15,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun photoOperationDao(): PhotoOperationDao
    abstract fun scheduleCommandDao(): ScheduleCommandDao
    abstract fun reminderStateDao(): ReminderStateDao
    abstract fun pushEventDao(): PushEventDao
    abstract fun tokenOperationDao(): TokenOperationDao
    abstract fun accountDao(): AccountDao
    abstract fun syncRecordDao(): SyncRecordDao
    abstract fun transactionDao(): TransactionDao
    abstract fun scheduledTransactionDao(): ScheduledTransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun aiMessageDao(): AiMessageDao
}
