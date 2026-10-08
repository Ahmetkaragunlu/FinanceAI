package com.ahmetkaragunlu.financeai.feature.schedule.data.sync

import com.ahmetkaragunlu.financeai.core.sync.reconciliation.Reconciliation
import com.ahmetkaragunlu.financeai.core.sync.reconciliation.SyncAcknowledgement
import com.ahmetkaragunlu.financeai.core.sync.contract.SyncPayload
import com.ahmetkaragunlu.financeai.core.sync.local.entity.SyncRecord
import com.ahmetkaragunlu.financeai.core.sync.reconciliation.reconcile

/** Rebase only edits made after the optimistic completion onto the first remote winner. */
internal fun acknowledgeCompletion(current: SyncRecord, sentPayload: String?, remote: String?, revision: Long): SyncAcknowledgement {
    val local = if (current.pendingDelete) null else current.pendingPayload
    fun canonical() = SyncAcknowledgement(SyncRecord(current.ownerId, current.collection, current.remoteId, remote, revision), true)
    if (SyncPayload.equivalent(local, sentPayload)) return canonical()
    return when (val decision = reconcile(sentPayload, local, remote)) {
        Reconciliation.Conflict -> SyncAcknowledgement(current.copy(conflictPayload = remote, conflictRevision = revision), false)
        Reconciliation.Equal -> canonical()
        is Reconciliation.Write -> SyncAcknowledgement(current.copy(basePayload = remote, baseRevision = revision,
            pendingPayload = decision.payload, pendingDelete = decision.payload == null), false)
    }
}
