package com.ahmetkaragunlu.financeai.app.presentation.sync

import androidx.annotation.StringRes
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.media.PhotoFields
import com.ahmetkaragunlu.financeai.core.sync.contract.FinancialFields
import com.ahmetkaragunlu.financeai.core.sync.contract.SyncPayload
import com.ahmetkaragunlu.financeai.feature.aichat.data.remote.message.AiMessageFields
import com.ahmetkaragunlu.financeai.feature.budget.data.remote.BudgetFields
import com.ahmetkaragunlu.financeai.feature.schedule.data.remote.contract.ScheduleFields
import com.ahmetkaragunlu.financeai.feature.transaction.data.remote.TransactionFields
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.transaction.localization.toLabelResId

internal sealed interface ConflictSummaryLine {
    data class Value(val text: String) : ConflictSummaryLine
    data class Label(@StringRes val resourceId: Int) : ConflictSummaryLine
    data class FormattedLabel(@StringRes val resourceId: Int, val argument: String) : ConflictSummaryLine
    data class Date(val timestamp: Long) : ConflictSummaryLine
    data class Percentage(val value: String) : ConflictSummaryLine
}

/** Preserves the existing conflict field selection, display order and conversion behaviour. */
internal fun mapConflictSummary(payload: String?): List<ConflictSummaryLine> {
    if (payload == null) return listOf(ConflictSummaryLine.Label(R.string.sync_deleted_record))
    val values = SyncPayload.decode(payload)
    return buildList {
        values[BudgetFields.LIMIT_PERCENTAGE]?.let {
            add(ConflictSummaryLine.Percentage(it.toString()))
        }
        (values[TransactionFields.DATE] ?: values[ScheduleFields.DATE])?.let {
            add(ConflictSummaryLine.Date((it as Number).toLong()))
        }
        values[FinancialFields.LOCATION_FULL]?.toString()?.let {
            add(ConflictSummaryLine.FormattedLabel(R.string.location_with_value, it))
        }
        // Both fields remain independent: mixed payloads retain both labels in their original order.
        for (field in listOf(TransactionFields.TYPE, ScheduleFields.TYPE)) {
            values[field]?.let {
                add(ConflictSummaryLine.Label(
                    if (it == TransactionType.INCOME.name) R.string.income else R.string.expense
                ))
            }
        }
        if (values[PhotoFields.REMOVED] == true) {
            add(ConflictSummaryLine.Label(R.string.sync_photo_removed))
        } else if (values[PhotoFields.STORAGE_URL] != null) {
            add(ConflictSummaryLine.Label(R.string.sync_photo_attached))
        }
        values[FinancialFields.LEGACY_AMOUNT]?.let {
            add(ConflictSummaryLine.Value("$it ${values[FinancialFields.CURRENCY_CODE] ?: ""}"))
        }
        (values[FinancialFields.NOTE] ?: values[AiMessageFields.TEXT])
            ?.toString()?.takeIf { it.isNotBlank() }?.let {
                add(ConflictSummaryLine.Value(it))
            }
        values[FinancialFields.CATEGORY]?.toString()?.let {
            add(ConflictSummaryLine.Label(CategoryType.valueOf(it).toLabelResId()))
        }
    }
}
