package com.ahmetkaragunlu.financeai.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.session.SessionCoordinator
import com.ahmetkaragunlu.financeai.notification.NotificationWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FinanceApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    @Inject
    lateinit var workManager: WorkManager

    @Inject lateinit var sessionCoordinator: SessionCoordinator

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        sessionCoordinator.start()
    }
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NotificationWorker.CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.notification_channel_description)
            enableVibration(true)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)
    }
}
