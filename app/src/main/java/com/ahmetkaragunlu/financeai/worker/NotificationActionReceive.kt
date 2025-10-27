package com.ahmetkaragunlu.financeai.worker

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.ahmetkaragunlu.financeai.roomdb.entitiy.TransactionEntity
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CONFIRM = "com.ahmetkaragunlu.financeai.ACTION_CONFIRM"
        const val ACTION_CANCEL = "com.ahmetkaragunlu.financeai.ACTION_CANCEL"
    }

    @Inject
    lateinit var repository: FinanceRepository

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        val transactionId = intent.getLongExtra(NotificationWorker.TRANSACTION_ID_KEY, -1L)
        if (transactionId == -1L) return

        // Bildirimi kapat
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(transactionId.toInt())

        when (intent.action) {
            ACTION_CONFIRM -> {
                scope.launch {
                    try {
                        val scheduledTransactions = repository.getAllScheduledTransactions().first()
                        val scheduledTransaction = scheduledTransactions.find { it.id == transactionId }

                        scheduledTransaction?.let { scheduled ->
                            // TransactionEntity olarak ekle
                            val transaction = TransactionEntity(
                                amount = scheduled.amount,
                                transaction = scheduled.type,
                                note = scheduled.note ?: "",
                                date = System.currentTimeMillis(),
                                category = scheduled.category
                            )
                            repository.insertTransaction(transaction)
                            repository.deleteScheduledTransaction(scheduled)

                            launch(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "İşlem başarıyla kaydedildi!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            ACTION_CANCEL -> {
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "İşlem beklemede kaldı",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
