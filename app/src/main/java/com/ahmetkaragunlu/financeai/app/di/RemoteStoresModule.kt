package com.ahmetkaragunlu.financeai.app.di

import com.ahmetkaragunlu.financeai.core.sync.RemoteRecordStore
import com.ahmetkaragunlu.financeai.core.sync.AccountSyncParticipant
import com.ahmetkaragunlu.financeai.feature.schedule.data.reminder.ScheduleCommands
import com.ahmetkaragunlu.financeai.feature.aichat.data.remote.AiMessageRemoteStore
import com.ahmetkaragunlu.financeai.feature.budget.data.remote.BudgetRemoteStore
import com.ahmetkaragunlu.financeai.feature.schedule.data.remote.ScheduledTransactionRemoteStore
import com.ahmetkaragunlu.financeai.feature.schedule.data.remote.SharedReminderRemoteStore
import com.ahmetkaragunlu.financeai.feature.transaction.data.remote.TransactionRemoteStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Application composition; the synchronization engine knows no concrete feature store. */
@Module
@InstallIn(SingletonComponent::class)
object RemoteStoresModule {
    @Provides
    fun provideParticipants(commands: ScheduleCommands): Set<AccountSyncParticipant> = setOf(commands)
    @Provides
    fun provideStores(transactions: TransactionRemoteStore, schedules: ScheduledTransactionRemoteStore,
        budgets: BudgetRemoteStore, messages: AiMessageRemoteStore, reminders: SharedReminderRemoteStore): Set<RemoteRecordStore> =
        setOf(transactions, schedules, budgets, messages, reminders)
}
