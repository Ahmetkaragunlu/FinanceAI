package com.ahmetkaragunlu.financeai.feature.transaction.presentation.history.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ahmetkaragunlu.financeai.core.format.formatRelativeDate
import com.ahmetkaragunlu.financeai.core.ui.component.formatAsAccountCurrency
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceGradients
import com.ahmetkaragunlu.financeai.core.ui.theme.Spacing
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.transaction.localization.toLabelResId
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.mapper.toIconResId

@Composable
internal fun TransactionCard(
    transaction: Transaction,
    modifier: Modifier = Modifier,
    onTransactionClick: (Int) -> Unit,
) {
    val context = LocalContext.current

    Card(
        onClick = { onTransactionClick(transaction.id) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Row(
            modifier =
                modifier
                    .fillMaxWidth()
                    .background(
                        brush = FinanceGradients.financialCard,
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(Spacing.screenPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = Color.Transparent) {
                Icon(
                    painter = painterResource(transaction.category.toIconResId()),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = modifier.padding(8.dp),
                )
            }

            Spacer(modifier = modifier.width(16.dp))

            Column {
                Text(
                    text = stringResource(transaction.category.toLabelResId()),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = transaction.date.formatRelativeDate(context),
                    color = FinanceColors.mutedText,
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            Spacer(modifier = modifier.weight(1f))

            val amountColor =
                if (transaction.transaction == TransactionType.INCOME) FinanceColors.income
                else FinanceColors.expense

            Text(text = transaction.amount.formatAsAccountCurrency(), color = amountColor)
            Spacer(modifier = modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color.LightGray,
            )
        }
    }
}
