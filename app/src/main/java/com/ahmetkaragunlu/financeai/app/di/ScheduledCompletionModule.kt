package com.ahmetkaragunlu.financeai.app.di

import com.ahmetkaragunlu.financeai.app.ScheduledCompletionCoordinator
import com.ahmetkaragunlu.financeai.feature.schedule.domain.usecase.CompleteScheduledTransaction
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ScheduledCompletionModule {
    @Binds abstract fun completion(implementation: ScheduledCompletionCoordinator): CompleteScheduledTransaction
}
