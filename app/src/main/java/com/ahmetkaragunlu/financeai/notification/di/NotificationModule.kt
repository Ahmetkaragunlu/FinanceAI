package com.ahmetkaragunlu.financeai.notification.di

import com.ahmetkaragunlu.financeai.notification.presentation.AndroidReminderPresenter
import com.ahmetkaragunlu.financeai.notification.presentation.ReminderPresenter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {
    @Binds abstract fun presenter(implementation: AndroidReminderPresenter): ReminderPresenter
}
