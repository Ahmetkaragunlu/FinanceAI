package com.ahmetkaragunlu.financeai.di.roommodule

import TransactionDao
import android.content.Context
import androidx.room.Room
import com.ahmetkaragunlu.financeai.roomdb.database.FinanceDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton



@InstallIn(SingletonComponent::class)
@Module
object RoomDatabase{
    @Provides
    @Singleton
    fun providesDatabase(@ApplicationContext context: Context): FinanceDatabase =
        Room.databaseBuilder(context, FinanceDatabase::class.java, "finance_db")
            .fallbackToDestructiveMigration()
            .build()
}



@InstallIn(SingletonComponent::class)
@Module
object RoomDao{
  @Provides
  @Singleton
  fun providesCategoryDao(financeDatabase: FinanceDatabase) : TransactionDao =
      financeDatabase.TransactionDao()
}
