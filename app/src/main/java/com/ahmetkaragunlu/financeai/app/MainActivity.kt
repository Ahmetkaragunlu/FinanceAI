package com.ahmetkaragunlu.financeai.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ahmetkaragunlu.financeai.app.navigation.FinanceNavigation
import com.ahmetkaragunlu.financeai.core.time.FinanceCalendar
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceAITheme
import javax.inject.Inject

open class MainActivity : ComponentActivity() {
    @Inject lateinit var calendar: FinanceCalendar
    override fun onResume() { super.onResume(); calendar.refresh() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinanceAITheme {
                FinanceNavigation()
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
