package com.ahmetkaragunlu.financeai.core.database.di

import android.content.Context
import androidx.room.Room
import com.ahmetkaragunlu.financeai.core.database.AccountMigration
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.feature.aichat.data.local.AiMessageDao
import com.ahmetkaragunlu.financeai.feature.budget.data.local.BudgetDao
import com.ahmetkaragunlu.financeai.feature.schedule.data.local.ScheduledTransactionDao
import com.ahmetkaragunlu.financeai.feature.transaction.data.local.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {
    //  Room Database instance
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FinanceDatabase {
        return Room.databaseBuilder(
            context,
            FinanceDatabase::class.java,
            "finance_db"
        )
            .addMigrations(AccountMigration.MIGRATION_13_14)
            .build()
    }

    // DAO instance
    @Provides
    @Singleton
    fun provideTransactionDao(database: FinanceDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    @Singleton
    fun provideBudgetDao(database: FinanceDatabase): BudgetDao {
        return database.budgetDao()
    }

    @Provides
    @Singleton
    fun provideAiMessageDao(database: FinanceDatabase): AiMessageDao {
        return database.aiMessageDao()
    }

    @Provides
    @Singleton
    fun provideScheduledTransactionDao(database: FinanceDatabase): ScheduledTransactionDao {
        return database.scheduledTransactionDao()
    }

}
