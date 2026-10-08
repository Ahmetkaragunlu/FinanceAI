package com.ahmetkaragunlu.financeai.feature.schedule.data.sync

import com.ahmetkaragunlu.financeai.core.sync.contract.FinancialFields
import com.ahmetkaragunlu.financeai.core.media.PhotoFields
import com.ahmetkaragunlu.financeai.feature.transaction.data.remote.TransactionFields
import com.ahmetkaragunlu.financeai.feature.schedule.data.remote.contract.ScheduleFields
import com.ahmetkaragunlu.financeai.core.sync.reconciliation.Reconciliation
import com.ahmetkaragunlu.financeai.core.sync.contract.SyncPayload
import com.ahmetkaragunlu.financeai.core.sync.reconciliation.reconcile

internal data class CompletedEdit(val base: String, val wanted: String, val decision: Reconciliation)

/** Only editable plan fields transfer; the original completion date/provenance never transfer from a plan. */
internal fun projectCompletedPlanEdit(planBase: String?, planWanted: String, remoteFinancial: String,
    financialBase: String? = null, financialWanted: String? = null): CompletedEdit {
    val oldPlan = planBase?.let(SyncPayload::decode).orEmpty()
    val newPlan = SyncPayload.decode(planWanted)
    val remote = SyncPayload.decode(remoteFinancial)
    val base = financialBase?.let(SyncPayload::decode)?.toMutableMap() ?: remote.toMutableMap()
    val wanted = financialWanted?.let(SyncPayload::decode)?.toMutableMap() ?: remote.toMutableMap()
    val fields = listOf(FinancialFields.AMOUNT_MINOR, FinancialFields.LEGACY_AMOUNT, FinancialFields.CURRENCY_CODE, ScheduleFields.TYPE, FinancialFields.CATEGORY, FinancialFields.NOTE,
        FinancialFields.LOCATION_FULL, FinancialFields.LOCATION_SHORT, FinancialFields.LATITUDE, FinancialFields.LONGITUDE, PhotoFields.STORAGE_URL, PhotoFields.REMOVED, PhotoFields.VERSION, PhotoFields.INTENT)
    for (source in fields) {
        val old = if (source == FinancialFields.NOTE) oldPlan[source] ?: "" else oldPlan[source]
        val new = if (source == FinancialFields.NOTE) newPlan[source] ?: "" else newPlan[source]
        if (old == new) continue
        val target = if (source == ScheduleFields.TYPE) TransactionFields.TYPE else source
        base[target] = old
        wanted[target] = new
    }
    base[TransactionFields.DATE] = remote[TransactionFields.DATE]
    wanted[TransactionFields.DATE] = remote[TransactionFields.DATE]
    val encodedBase = SyncPayload.encode(base)
    val encodedWanted = SyncPayload.encode(wanted)
    return CompletedEdit(encodedBase, encodedWanted, reconcile(encodedBase, encodedWanted, remoteFinancial))
}
