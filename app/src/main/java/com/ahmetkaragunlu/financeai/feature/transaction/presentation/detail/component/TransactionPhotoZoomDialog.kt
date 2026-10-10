package com.ahmetkaragunlu.financeai.feature.transaction.presentation.detail.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors
import com.ahmetkaragunlu.financeai.core.ui.theme.Spacing

@Composable
internal fun TransactionPhotoZoomDialog(
    photoUri: String,
    onDismiss: () -> Unit,
    onChangePhoto: () -> Unit,
    onDeletePhoto: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            Image(
                painter = rememberAsyncImagePainter(photoUri),
                contentDescription = stringResource(R.string.full_screen_photo_desc),
                modifier = Modifier.fillMaxSize().clickable(onClick = onDismiss),
                contentScale = ContentScale.Fit,
            )

            IconButton(
                onClick = onDismiss,
                modifier =
                    Modifier.align(Alignment.TopStart)
                        .padding(Spacing.screenPadding)
                        .background(Color.Black.copy(0.5f), CircleShape),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Row(
                modifier =
                    Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(
                    onClick = onChangePhoto,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = FinanceColors.mutedText,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.change))
                }
                Button(
                    onClick = onDeletePhoto,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = FinanceColors.expense,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.delete))
                }
            }
        }
    }
}
