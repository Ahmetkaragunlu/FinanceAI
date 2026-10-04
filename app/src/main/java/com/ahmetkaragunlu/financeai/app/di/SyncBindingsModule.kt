package com.ahmetkaragunlu.financeai.app.di

import com.ahmetkaragunlu.financeai.feature.budget.domain.sync.BudgetSync
import com.ahmetkaragunlu.financeai.feature.schedule.domain.sync.ScheduledTransactionSync
import com.ahmetkaragunlu.financeai.feature.transaction.domain.sync.TransactionSync
import com.ahmetkaragunlu.financeai.firebasesync.FirebaseSyncService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Temporary composition seam: all bindings share the existing singleton until phase 2 splits sync. */
@Module
@InstallIn(SingletonComponent::class)
abstract class SyncBindingsModule {
    @Binds
    abstract fun bindTransactionSync(implementation: FirebaseSyncService): TransactionSync

    @Binds
    abstract fun bindBudgetSync(implementation: FirebaseSyncService): BudgetSync

    @Binds
    abstract fun bindScheduledTransactionSync(implementation: FirebaseSyncService): ScheduledTransactionSync
}
