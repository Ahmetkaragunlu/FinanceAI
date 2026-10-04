package com.ahmetkaragunlu.financeai.feature.aichat.di

import com.ahmetkaragunlu.financeai.feature.aichat.data.repository.AiRepositoryImpl
import com.ahmetkaragunlu.financeai.feature.aichat.domain.repository.AiRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAiRepository(implementation: AiRepositoryImpl): AiRepository
}
