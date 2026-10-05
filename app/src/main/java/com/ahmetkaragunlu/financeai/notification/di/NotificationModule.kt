package com.ahmetkaragunlu.financeai.notification.di

import com.ahmetkaragunlu.financeai.notification.AndroidReminderPresenter
import com.ahmetkaragunlu.financeai.notification.ReminderPresenter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {
    @Binds abstract fun presenter(implementation: AndroidReminderPresenter): ReminderPresenter
}
