package com.ahmetkaragunlu.financeai.di.roommodule



import android.content.Context
import androidx.room.Room
import com.ahmetkaragunlu.financeai.roomdb.dao.TransactionDao
import com.ahmetkaragunlu.financeai.roomdb.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepositoryImpl
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
            .fallbackToDestructiveMigration()
            .build()
    }

    // DAO instance
    @Provides
    @Singleton
    fun provideTransactionDao(database: FinanceDatabase) : TransactionDao {
        return database.transactionDao()
    }
    // Repository instance (Interface + Impl)
    @Provides
    @Singleton
    fun provideFinanceRepository(
        transactionDao: TransactionDao
    ): FinanceRepository {
        return FinanceRepositoryImpl(transactionDao)
    }
}
