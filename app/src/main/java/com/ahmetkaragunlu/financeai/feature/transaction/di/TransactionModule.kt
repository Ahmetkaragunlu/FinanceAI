package com.ahmetkaragunlu.financeai.feature.transaction.di

import com.ahmetkaragunlu.financeai.feature.transaction.data.repository.TransactionRepositoryImpl
import com.ahmetkaragunlu.financeai.feature.transaction.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TransactionModule {
    @Binds
    @Singleton
    abstract fun bindTransactionRepository(implementation: TransactionRepositoryImpl): TransactionRepository
}
