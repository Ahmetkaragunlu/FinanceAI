package com.ahmetkaragunlu.financeai.core.sync.contract

import com.ahmetkaragunlu.financeai.core.sync.local.entity.SyncRecord
import com.ahmetkaragunlu.financeai.core.session.ActiveAccount

/** Feature-owned durable commands can hold related row writes until their atomic remote result. */
interface AccountSyncParticipant {
    suspend fun heldRecords(account: ActiveAccount): Set<Pair<String, String>>
    suspend fun synchronize(account: ActiveAccount)

    /** Network/media preparation happens before the caller enters its guarded Room transaction. */
    suspend fun prepareResolution(
        account: ActiveAccount, conflict: SyncRecord, keepLocal: Boolean,
        remoteDocument: Map<String, Any?>
    ): LocalConflictResolution? = null

    /** Caller owns the session guard and Room transaction; this hook must not perform network work. */
    suspend fun resolve(
        account: ActiveAccount, conflict: SyncRecord, keepLocal: Boolean,
        remotePayload: String?, prepared: Map<String, Any?>?, revision: Long
    ): Boolean = false
}

fun interface LocalConflictResolution {
    suspend fun apply(current: SyncRecord): Boolean
}
