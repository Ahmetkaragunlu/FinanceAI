package com.ahmetkaragunlu.financeai.core.sync.contract

import com.ahmetkaragunlu.financeai.core.session.ActiveAccount

/** Only the storage/mapping seam is shared; each feature owns its persisted record. */
interface RemoteRecordStore {
    val collection: String
    fun normalize(data: Map<String, Any?>, account: ActiveAccount): Map<String, Any?>
    suspend fun prepare(account: ActiveAccount, remoteId: String, data: Map<String, Any?>): Map<String, Any?> = data
    suspend fun apply(account: ActiveAccount, remoteId: String, data: Map<String, Any?>?)
}
