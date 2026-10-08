package com.ahmetkaragunlu.financeai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.ahmetkaragunlu.financeai.app.navigation.FinanceNavigation
import com.ahmetkaragunlu.financeai.app.presentation.MainViewModel
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceAITheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** Single activity; retains the existing launcher and PendingIntent component identity. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { viewModel.onForeground() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.acceptInitialLink(
            intent.deepLinkUri(),
            previouslyConsumed =
                savedInstanceState?.getBoolean("finance_deep_link_consumed") == true,
        )
        enableEdgeToEdge()
        setContent {
            val pendingDeepLink by viewModel.pendingDeepLink.collectAsStateWithLifecycle()
            FinanceAITheme {
                FinanceNavigation(
                    deepLink = pendingDeepLink,
                    onDeepLinkConsumed = ::consumeDeepLink,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.acceptNewLink(intent.deepLinkUri())
    }

    private fun consumeDeepLink(id: String) {
        if (viewModel.consumeDeepLink(id)) intent.data = null
    }
}

private fun Intent.deepLinkUri(): String? = dataString.takeIf { action == Intent.ACTION_VIEW }
