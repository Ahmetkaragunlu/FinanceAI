package com.ahmetkaragunlu.financeai.feature.transaction.presentation.add

import android.net.Uri
import com.ahmetkaragunlu.financeai.feature.location.domain.model.LocationData
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import java.time.ZoneId

data class AddTransactionUiState(
    val type: TransactionType,
    val category: CategoryType?,
    val amount: String,
    val note: String,
    val date: Long,
    val reminderEnabled: Boolean,
    val photoUri: Uri?,
    val location: LocationData?,
    val zone: ZoneId,
)
