package com.ahmetkaragunlu.financeai.core.sync.reconciliation

import com.ahmetkaragunlu.financeai.core.sync.contract.SyncPayload

sealed interface Reconciliation {
    data class Write(val payload: String?) : Reconciliation
    data object Equal : Reconciliation
    data object Conflict : Reconciliation
}

/** Three-way merge permits disjoint edits, but never silently chooses an overlapping edit. */
fun reconcile(base: String?, local: String?, remote: String?): Reconciliation {
    if (SyncPayload.equivalent(local, remote)) return Reconciliation.Equal
    if (SyncPayload.equivalent(base, remote)) return Reconciliation.Write(local)
    if (local == null || remote == null || base == null) return Reconciliation.Conflict
    val baseline = SyncPayload.decode(base)
    val left = SyncPayload.decode(local)
    val right = SyncPayload.decode(remote)
    val merged = right.toMutableMap()
    for (key in baseline.keys + left.keys + right.keys) {
        val old = baseline[key]
        val mine = left[key]
        val theirs = right[key]
        if (mine != old && theirs != old && mine != theirs) return Reconciliation.Conflict
        if (mine != old) merged[key] = mine
    }
    return Reconciliation.Write(SyncPayload.encode(merged))
}
