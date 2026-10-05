package com.ahmetkaragunlu.financeai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.ahmetkaragunlu.financeai.app.navigation.FinanceNavigation
import com.ahmetkaragunlu.financeai.core.time.FinanceCalendar
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceAITheme
import com.ahmetkaragunlu.financeai.feature.schedule.data.reminder.ReminderCoordinator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/** Single activity; retains the existing launcher and PendingIntent component identity. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var calendar: FinanceCalendar
    @Inject lateinit var reminders: ReminderCoordinator

    override fun onResume() {
        super.onResume()
        calendar.refresh()
        lifecycleScope.launch { reminders.restoreCurrent() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinanceAITheme { FinanceNavigation() }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
