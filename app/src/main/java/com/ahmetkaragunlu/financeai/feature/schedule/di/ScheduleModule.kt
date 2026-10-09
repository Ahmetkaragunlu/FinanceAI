package com.ahmetkaragunlu.financeai.feature.schedule.di

import com.ahmetkaragunlu.financeai.feature.schedule.data.remote.FirebaseScheduleStateRestorer
import com.ahmetkaragunlu.financeai.feature.schedule.data.repository.ScheduledTransactionRepositoryImpl
import com.ahmetkaragunlu.financeai.feature.schedule.domain.repository.ScheduledTransactionRepository
import com.ahmetkaragunlu.financeai.feature.schedule.domain.usecase.RestoreScheduleState
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ScheduleModule {
    @Binds
    abstract fun bindScheduleStateRestorer(implementation: FirebaseScheduleStateRestorer): RestoreScheduleState

    @Binds
    @Singleton
    abstract fun bindScheduledTransactionRepository(implementation: ScheduledTransactionRepositoryImpl): ScheduledTransactionRepository
}
