package com.ahmetkaragunlu.financeai.app.di

import com.ahmetkaragunlu.financeai.app.AccountWorkRestorer
import com.ahmetkaragunlu.financeai.core.session.SessionWorkRestorer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AccountWorkModule {
    @Binds
    abstract fun bindWorkRestorer(implementation: AccountWorkRestorer): SessionWorkRestorer
}
