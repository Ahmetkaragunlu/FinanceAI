package com.ahmetkaragunlu.financeai.app.presentation.sync

import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.media.PhotoFields
import com.ahmetkaragunlu.financeai.core.sync.contract.FinancialFields
import com.ahmetkaragunlu.financeai.core.sync.contract.SyncPayload
import com.ahmetkaragunlu.financeai.feature.aichat.data.remote.message.AiMessageFields
import com.ahmetkaragunlu.financeai.feature.budget.data.remote.BudgetFields
import com.ahmetkaragunlu.financeai.feature.schedule.data.remote.contract.ScheduleFields
import com.ahmetkaragunlu.financeai.feature.transaction.data.remote.TransactionFields
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ConflictSummaryMapperTest {
    @Test
    fun deletionIsDistinctFromAnExistingEmptyPayload() {
        assertEquals(
            listOf(ConflictSummaryLine.Label(R.string.sync_deleted_record)),
            mapConflictSummary(null),
        )
        assertTrue(mapConflictSummary("{}").isEmpty())
    }

    @Test
    fun mixedFinancialFieldsKeepTheirDisplayOrderAndBothTypeLabels() {
        val payload = SyncPayload.encode(
            mapOf(
                BudgetFields.LIMIT_PERCENTAGE to 85,
                TransactionFields.DATE to 1_000L,
                ScheduleFields.DATE to 2_000L,
                FinancialFields.LOCATION_FULL to "Synthetic address",
                TransactionFields.TYPE to "INCOME",
                ScheduleFields.TYPE to "EXPENSE",
                PhotoFields.REMOVED to true,
                PhotoFields.STORAGE_URL to "synthetic-photo",
                FinancialFields.LEGACY_AMOUNT to 12.5,
                FinancialFields.CURRENCY_CODE to "USD",
                FinancialFields.NOTE to "Financial note",
                AiMessageFields.TEXT to "AI text",
                FinancialFields.CATEGORY to "FOOD",
            )
        )
        assertEquals(
            listOf(
                ConflictSummaryLine.Percentage("85"),
                ConflictSummaryLine.Date(1_000L),
                ConflictSummaryLine.FormattedLabel(
                    R.string.location_with_value,
                    "Synthetic address"
                ),
                ConflictSummaryLine.Label(R.string.income),
                ConflictSummaryLine.Label(R.string.expense),
                ConflictSummaryLine.Label(R.string.sync_photo_removed),
                ConflictSummaryLine.Value("12.5 USD"),
                ConflictSummaryLine.Value("Financial note"),
                ConflictSummaryLine.Label(R.string.category_food),
            ), mapConflictSummary(payload)
        )
    }

    @Test
    fun scheduleDateAndAiTextAreFallbacksAndMissingCurrencyKeepsTheOriginalAmountText() {
        assertEquals(
            listOf(
                ConflictSummaryLine.Date(2_000L),
                ConflictSummaryLine.Label(R.string.income),
                ConflictSummaryLine.Value("10 "),
                ConflictSummaryLine.Value("AI text"),
            ), mapConflictSummary(
                SyncPayload.encode(
                    mapOf(
                        ScheduleFields.DATE to 2_000L,
                        ScheduleFields.TYPE to "INCOME",
                        FinancialFields.LEGACY_AMOUNT to 10,
                        FinancialFields.NOTE to null,
                        AiMessageFields.TEXT to "AI text",
                    )
                )
            )
        )
        // Minor-unit-only fields were not displayed by the original summary.
        assertTrue(
            mapConflictSummary(
                SyncPayload.encode(
                    mapOf(FinancialFields.AMOUNT_MINOR to 1_000L)
                )
            ).isEmpty()
        )
    }

    @Test
    fun blankFinancialNoteDoesNotFallThroughToAiTextButOtherValuesKeepTheirStringConversion() {
        assertTrue(
            mapConflictSummary(
                SyncPayload.encode(
                    mapOf(
                        FinancialFields.NOTE to "   ", AiMessageFields.TEXT to "AI text"
                    )
                )
            ).isEmpty()
        )
        assertEquals(
            listOf(ConflictSummaryLine.Value("0")), mapConflictSummary(
                SyncPayload.encode(mapOf(FinancialFields.NOTE to 0))
            )
        )
    }

    @Test
    fun photoRemovalWinsWhileAnyNonNullStorageValueStillMeansAttached() {
        assertEquals(
            listOf(ConflictSummaryLine.Label(R.string.sync_photo_removed)), mapConflictSummary(
                SyncPayload.encode(
                    mapOf(
                        PhotoFields.REMOVED to true,
                        PhotoFields.STORAGE_URL to "photo"
                    )
                )
            )
        )
        assertEquals(
            listOf(ConflictSummaryLine.Label(R.string.sync_photo_attached)), mapConflictSummary(
                SyncPayload.encode(
                    mapOf(
                        PhotoFields.REMOVED to false,
                        PhotoFields.STORAGE_URL to ""
                    )
                )
            )
        )
        assertTrue(mapConflictSummary(SyncPayload.encode(mapOf(PhotoFields.REMOVED to false))).isEmpty())
    }

    @Test
    fun unknownTypesKeepTheExpenseFallbackAndInvalidCategoriesOrDatesAreNotSilentlyReinterpreted() {
        assertEquals(
            listOf(
                ConflictSummaryLine.Label(R.string.expense),
                ConflictSummaryLine.Label(R.string.expense),
            ), mapConflictSummary(
                SyncPayload.encode(
                    mapOf(
                        TransactionFields.TYPE to "income", ScheduleFields.TYPE to "unknown"
                    )
                )
            )
        )
        assertThrows(IllegalArgumentException::class.java) {
            mapConflictSummary(SyncPayload.encode(mapOf(FinancialFields.CATEGORY to "unknown")))
        }
        assertThrows(ClassCastException::class.java) {
            mapConflictSummary(SyncPayload.encode(mapOf(TransactionFields.DATE to "not-a-number")))
        }
    }
}
