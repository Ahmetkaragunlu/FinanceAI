package com.ahmetkaragunlu.financeai.feature.aichat.di

import com.ahmetkaragunlu.financeai.feature.aichat.data.repository.AiRepositoryImpl
import com.ahmetkaragunlu.financeai.feature.aichat.domain.repository.AiRepository
import com.ahmetkaragunlu.financeai.feature.aichat.data.local.AiConversationStore
import com.ahmetkaragunlu.financeai.feature.aichat.data.local.RoomAiConversationStore
import com.ahmetkaragunlu.financeai.feature.aichat.data.remote.FirebaseAiTextGenerator
import com.ahmetkaragunlu.financeai.feature.aichat.data.report.FinancialSnapshotSource
import com.ahmetkaragunlu.financeai.feature.aichat.data.report.RoomFinancialSnapshotSource
import com.ahmetkaragunlu.financeai.feature.aichat.domain.generation.AiTextGenerator
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

    @Binds abstract fun bindGenerator(implementation: FirebaseAiTextGenerator): AiTextGenerator
    @Binds abstract fun bindConversations(implementation: RoomAiConversationStore): AiConversationStore
    @Binds abstract fun bindSnapshots(implementation: RoomFinancialSnapshotSource): FinancialSnapshotSource
}
