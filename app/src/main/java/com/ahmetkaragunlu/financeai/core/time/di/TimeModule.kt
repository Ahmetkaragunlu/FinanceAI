package com.ahmetkaragunlu.financeai.core.time.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides fun clock(): Clock = Clock.systemDefaultZone()
}
