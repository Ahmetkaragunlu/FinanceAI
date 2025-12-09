package com.ahmetkaragunlu.financeai.di.module

import android.content.Context
import androidx.room.Room
import com.ahmetkaragunlu.financeai.roomdb.dao.AiMessageDao
import com.ahmetkaragunlu.financeai.roomdb.dao.BudgetDao
import com.ahmetkaragunlu.financeai.roomdb.dao.ScheduledTransactionDao
import com.ahmetkaragunlu.financeai.roomdb.dao.TransactionDao
import com.ahmetkaragunlu.financeai.roomdb.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.roomrepository.budgetrepositroy.BudgetRepository
import com.ahmetkaragunlu.financeai.roomrepository.budgetrepositroy.BudgetRepositoryImpl
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
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
            .fallbackToDestructiveMigration()
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
        return database.BudgetDao()
    }

    @Provides
    @Singleton
    fun provideAiMessageDao(database: FinanceDatabase): AiMessageDao {
        return database.aiMessageDao()
    }

    // BudgetRepository Provider
    @Provides
    @Singleton
    fun provideBudgetRepository(
        budgetDao: BudgetDao,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): BudgetRepository {
        return BudgetRepositoryImpl(budgetDao, ioDispatcher)
    }
    @Provides
    @Singleton
    fun provideScheduledTransactionDao(database: FinanceDatabase): ScheduledTransactionDao {
        return database.scheduledTransactionDao()
    }
    // Repository instance (Interface + Impl)
    @Provides
    @Singleton
    fun provideFinanceRepository(
        transactionDao: TransactionDao,
        scheduledTransactionDao: ScheduledTransactionDao,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): FinanceRepository {
        return FinanceRepositoryImpl(transactionDao, scheduledTransactionDao,ioDispatcher)
    }
}
