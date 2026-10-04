package com.ahmetkaragunlu.financeai.feature.budget.di

import com.ahmetkaragunlu.financeai.feature.budget.data.repository.BudgetRepositoryImpl
import com.ahmetkaragunlu.financeai.feature.budget.domain.repository.BudgetRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BudgetModule {
    @Binds
    @Singleton
    abstract fun bindBudgetRepository(implementation: BudgetRepositoryImpl): BudgetRepository
}
