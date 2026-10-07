package com.ahmetkaragunlu.financeai.feature.auth.di

import com.ahmetkaragunlu.financeai.feature.auth.data.repository.AuthRepositoryImpl
import com.ahmetkaragunlu.financeai.feature.auth.data.local.session.CredentialSessionCleaner
import com.ahmetkaragunlu.financeai.feature.auth.data.local.session.CredentialSessionManager
import com.ahmetkaragunlu.financeai.feature.auth.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(implementation: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindCredentialSessionCleaner(manager: CredentialSessionManager): CredentialSessionCleaner
}
