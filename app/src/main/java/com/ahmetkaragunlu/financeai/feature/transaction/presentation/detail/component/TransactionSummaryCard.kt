package com.ahmetkaragunlu.financeai.feature.transaction.presentation.detail.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.format.formatRelativeDate
import com.ahmetkaragunlu.financeai.core.ui.component.formatAsAccountCurrency
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceGradients
import com.ahmetkaragunlu.financeai.core.ui.theme.Spacing
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.transaction.localization.toLabelResId
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.mapper.toIconResId
import java.io.File

@Composable
internal fun TransactionSummaryCard(
    tx: Transaction,
    onPhotoClick: () -> Unit,
    onAddPhotoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Card(
        modifier = modifier.widthIn(max = 450.dp).fillMaxWidth().padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .background(
                        brush = FinanceGradients.financialCard,
                        shape = RoundedCornerShape(16.dp),
                    )
        ) {
            Row(
                modifier = modifier.fillMaxWidth().padding(Spacing.screenPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(shape = CircleShape, color = FinanceColors.onAccent.copy(alpha = 0.2f)) {
                    Icon(
                        painter = painterResource(tx.category.toIconResId()),
                        contentDescription = null,
                        tint = Color.Unspecified,
                    )
                }
                Spacer(modifier = modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(tx.category.toLabelResId()),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = tx.date.formatRelativeDate(context),
                        color = FinanceColors.mutedText,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }

                Spacer(modifier = modifier.weight(1f))

                Text(
                    text = tx.amount.formatAsAccountCurrency(),
                    color =
                        if (tx.transaction == TransactionType.INCOME) FinanceColors.income
                        else FinanceColors.expense,
                )
            }

            // Optional Info Section
            Column(
                modifier = modifier.fillMaxWidth().padding(Spacing.screenPadding),
                verticalArrangement = Arrangement.spacedBy(Spacing.itemGap),
            ) {
                if (tx.note.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.note_with_value, tx.note),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }

                if (tx.locationShort != null) {
                    Text(
                        text = stringResource(R.string.location_with_value, tx.locationShort),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }

                // Photo Section
                if (
                    tx.photoUri != null &&
                        (tx.photoUri.startsWith("https://") || File(tx.photoUri).exists())
                ) {
                    Spacer(modifier = modifier.height(8.dp))
                    Card(
                        modifier =
                            modifier.fillMaxWidth().height(180.dp).clickable { onPhotoClick() },
                        shape = RoundedCornerShape(12.dp),
                        colors =
                            CardDefaults.cardColors(containerColor = FinanceColors.photoSurface),
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                painter = rememberAsyncImagePainter(tx.photoUri),
                                contentDescription =
                                    stringResource(R.string.transaction_photo_desc),
                                modifier = modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop,
                            )
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier =
                                    Modifier.align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        .padding(4.dp),
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { onAddPhotoClick() },
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                contentColor = FinanceColors.onAccent
                            ),
                    ) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.add_photo))
                    }
                }
            }
        }
    }
}
