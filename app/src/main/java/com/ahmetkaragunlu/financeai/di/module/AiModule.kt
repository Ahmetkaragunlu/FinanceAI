package com.ahmetkaragunlu.financeai.di.module

import android.content.Context
import com.ahmetkaragunlu.financeai.BuildConfig
import com.ahmetkaragunlu.financeai.ai_repository.AiRepository
import com.ahmetkaragunlu.financeai.ai_repository.AiRepositoryImpl
import com.ahmetkaragunlu.financeai.roomdb.dao.AiMessageDao
import com.ahmetkaragunlu.financeai.roomdb.dao.TransactionDao
import com.google.ai.client.generativeai.GenerativeModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel {
        return GenerativeModel(
            modelName ="gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    @Provides
    @Singleton
    fun provideAiRepository(
        generativeModel: GenerativeModel,
        aiMessageDao: AiMessageDao,
        transactionDao: TransactionDao,
        @ApplicationContext context: Context
    ): AiRepository {
        return AiRepositoryImpl(generativeModel, aiMessageDao, transactionDao, context)
    }
}

