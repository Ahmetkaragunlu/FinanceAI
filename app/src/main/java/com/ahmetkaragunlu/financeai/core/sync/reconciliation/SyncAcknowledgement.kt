package com.ahmetkaragunlu.financeai.core.sync.reconciliation

import com.ahmetkaragunlu.financeai.core.sync.local.entity.SyncRecord

internal fun acknowledgesDeletion(
    deleted: Boolean,
    previousMutationId: String?,
    pendingMutationId: String?
): Boolean =
    deleted && pendingMutationId != null && previousMutationId == pendingMutationId

internal data class SyncAcknowledgement(val record: SyncRecord, val applyRemote: Boolean)

/** An old in-flight success may advance the baseline, but must never erase a newer local intention. */
internal fun acknowledgeSync(
    current: SyncRecord, sentMutationId: String?, remote: String?, revision: Long,
    conflict: Boolean
): SyncAcknowledgement = when {
    current.mutationId != sentMutationId -> SyncAcknowledgement(
        if (conflict) current else current.copy(basePayload = remote, baseRevision = revision),
        false
    )

    conflict -> SyncAcknowledgement(
        current.copy(
            conflictPayload = remote,
            conflictRevision = revision
        ), false
    )

    else -> SyncAcknowledgement(
        SyncRecord(
            current.ownerId,
            current.collection,
            current.remoteId,
            remote,
            revision
        ), true
    )
}
