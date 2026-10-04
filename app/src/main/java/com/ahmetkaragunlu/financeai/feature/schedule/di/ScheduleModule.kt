package com.ahmetkaragunlu.financeai.feature.schedule.di

import com.ahmetkaragunlu.financeai.feature.schedule.data.repository.ScheduledTransactionRepositoryImpl
import com.ahmetkaragunlu.financeai.feature.schedule.domain.repository.ScheduledTransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ScheduleModule {
    @Binds
    @Singleton
    abstract fun bindScheduledTransactionRepository(implementation: ScheduledTransactionRepositoryImpl): ScheduledTransactionRepository
}
