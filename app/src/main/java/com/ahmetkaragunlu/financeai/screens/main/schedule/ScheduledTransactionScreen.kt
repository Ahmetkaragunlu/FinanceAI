package com.ahmetkaragunlu.financeai.screens.main.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.roomdb.entitiy.ScheduledTransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.utils.formatAsCurrency
import com.ahmetkaragunlu.financeai.utils.formatScheduleDate // DİKKAT: Doğru fonksiyon bu!
import com.ahmetkaragunlu.financeai.utils.toIconResId
import com.ahmetkaragunlu.financeai.utils.toResId
import com.ahmetkaragunlu.financeai.viewmodel.ScheduleViewModel

@Composable
fun ScheduledTransactionScreen(
    modifier: Modifier = Modifier,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val scheduledTransactions by viewModel.scheduledTransactions.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.background))
            .padding(16.dp)
    ) {
        if (scheduledTransactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.no_scheduled_transactions),
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(
                    items = scheduledTransactions,
                    key = { it.id }
                ) { transaction ->
                    ScheduledTransactionItem(
                        transaction = transaction,
                        onExecuteClick = { viewModel.executeScheduledTransaction(transaction) }
                    )
                }
            }
        }
    }
}

@Composable
fun ScheduledTransactionItem(
    transaction: ScheduledTransactionEntity,
    modifier: Modifier = Modifier,
    onExecuteClick: () -> Unit
) {
    // 1. Context'i buraya geri ekledik çünkü "formatScheduleDate" fonksiyonu context istiyor.
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF404349)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = transaction.category.toIconResId()),
                        contentDescription = null,
                        tint = Color.Unspecified
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = transaction.category.toResId()),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessAlarm,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))

                    // 2. İŞTE DÜZELTME BURADA: formatAsShortDate YERİNE formatScheduleDate KULLANILDI
                    Text(
                        text = transaction.scheduledDate.formatScheduleDate(context),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                }

                if (!transaction.note.isNullOrBlank()) {
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        maxLines = 1
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = transaction.amount.formatAsCurrency(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (transaction.type == TransactionType.INCOME) Color(0xFF4CAF50) else Color(0xFFEF5350),
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                IconButton(
                    onClick = onExecuteClick,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.confirm),
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}