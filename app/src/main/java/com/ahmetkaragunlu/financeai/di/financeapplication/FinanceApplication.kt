package com.ahmetkaragunlu.financeai.di.financeapplication

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.firebaserepo.FirebaseSyncService
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.ahmetkaragunlu.financeai.worker.NotificationWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FinanceApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var workManager: WorkManager

    @Inject
    lateinit var firebaseSyncService: FirebaseSyncService

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Firebase senkronizasyonunu başlat
        initializeFirebaseSync()
    }

    private fun initializeFirebaseSync() {
        applicationScope.launch {
            // 1. Önce ilk yüklemeyi yap (Firebase'deki tüm verileri çek)
            firebaseSyncService.performInitialSync()

            // 2. Sonra listener'ları başlat (canlı değişiklikleri dinle)
            firebaseSyncService.startListeningToTransactions()
            firebaseSyncService.startListeningToScheduledTransactions()
        }
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