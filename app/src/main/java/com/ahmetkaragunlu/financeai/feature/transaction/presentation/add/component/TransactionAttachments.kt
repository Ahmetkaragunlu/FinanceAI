package com.ahmetkaragunlu.financeai.feature.transaction.presentation.add.component

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors
import com.ahmetkaragunlu.financeai.feature.location.domain.model.LocationData

@Composable
internal fun TransactionAttachments(
    location: LocationData?,
    photoUri: Uri?,
    onLocationClick: () -> Unit,
    onPhotoClick: () -> Unit,
    onClearLocation: () -> Unit,
    onClearPhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.widthIn(max = 450.dp).fillMaxWidth().padding(bottom = 16.dp)) {
        // Location Card
        Card(
            onClick = { onLocationClick() },
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Box(modifier = Modifier.fillMaxSize().background(FinanceColors.fieldSurface)) {
                if (location == null) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = FinanceColors.mutedText,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.location_optional),
                            color = FinanceColors.mutedText,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp).size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = location.addressShort,
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { onClearLocation() },
                            modifier = Modifier.size(20.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.remove_photo),
                                tint = FinanceColors.mutedText,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = modifier.width(8.dp))

        // Photo Card
        Card(
            onClick = {
                if (photoUri == null) {
                    onPhotoClick()
                }
            },
            modifier = modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Box(modifier = modifier.fillMaxSize().background(FinanceColors.fieldSurface)) {
                if (photoUri == null) {
                    Row(
                        modifier = modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = FinanceColors.mutedText,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                        Spacer(modifier = modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.photo_optional),
                            color = FinanceColors.mutedText,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    Image(
                        painter = rememberAsyncImagePainter(photoUri),
                        contentDescription = stringResource(R.string.selected_photo),
                        modifier = modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )

                    IconButton(
                        onClick = { onClearPhoto() },
                        modifier =
                            modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .padding(2.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(50)),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.remove_photo),
                            tint = FinanceColors.onAccent,
                            modifier = modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}
