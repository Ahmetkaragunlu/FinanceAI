
package com.ahmetkaragunlu.financeai.di.financeapplication

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.ahmetkaragunlu.financeai.R
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.ahmetkaragunlu.financeai.worker.NotificationWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FinanceApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var workManager: WorkManager

    // firebaseSyncService artık gerekli değil - çünkü AuthRepository içinde yönetiliyor

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // ÖNEMLİ: Sync'i burada BAŞLATMA!
        // Kullanıcı giriş yaptığında AuthRepository otomatik başlatacak
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
