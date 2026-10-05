package com.ahmetkaragunlu.financeai.feature.schedule.data.remote

import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.media.PhotoRemoteCache
import com.ahmetkaragunlu.financeai.core.media.RemotePhoto
import com.ahmetkaragunlu.financeai.core.money.MoneyAmounts
import com.ahmetkaragunlu.financeai.core.session.ActiveAccount
import com.ahmetkaragunlu.financeai.core.sync.RemoteRecordStore
import com.ahmetkaragunlu.financeai.core.sync.SyncPayload
import com.ahmetkaragunlu.financeai.feature.schedule.data.mapper.*
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.*
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.*
import javax.inject.Inject

class ScheduledTransactionRemoteStore @Inject constructor(private val database: FinanceDatabase, private val photos: PhotoRemoteCache) : RemoteRecordStore {
    override val collection = "scheduled_transactions"
    private fun model(account: ActiveAccount, remoteId: String, data: Map<String, Any?>): ScheduledTransaction {
        val currency = data["currencyCode"] as? String ?: account.currencyCode
        require(currency == account.currencyCode) { "Currency mismatch" }
        return ScheduledTransaction(
            ownerId = account.ownerId, currencyCode = currency, firestoreId = remoteId, amount = (data["amountMinor"] as? Number)?.let { MoneyAmounts.toMajor(MoneyAmounts.readMinor(it), currency) } ?: (data["amount"] as? Number)?.toDouble() ?: 0.0,
            type = TransactionType.valueOf(data["type"] as? String ?: "EXPENSE"),
            category = CategoryType.valueOf(data["category"] as? String ?: "OTHER"),
            note = data["note"] as? String,
            scheduledDate = (data["scheduledDate"] as? Number)?.toLong() ?: 0L,
            expirationNotificationSent = data["expirationNotificationSent"] as? Boolean ?: false,
            notificationSent = data["notificationSent"] as? Boolean ?: false,
            locationFull = data["locationFull"] as? String,
            locationShort = data["locationShort"] as? String,
            latitude = (data["latitude"] as? Number)?.toDouble(),
            longitude = (data["longitude"] as? Number)?.toDouble(),
            syncedToFirebase = true
        )
    }
    override fun normalize(data: Map<String, Any?>, account: ActiveAccount): Map<String, Any?> =
        model(account, "", data).toFirebaseMap() + mapOf("photoStorageUrl" to data["photoStorageUrl"], "photoRemoved" to (data["photoRemoved"] == true), "photoVersion" to data["photoVersion"])

    override suspend fun prepare(account: ActiveAccount, remoteId: String, data: Map<String, Any?>): Map<String, Any?> {
        if (data["photoRemoved"] == true) return data
        val existing = database.scheduledTransactionDao().getScheduledTransactionByFirestoreId(remoteId)
        val baseline = database.syncRecordDao().get(account.ownerId, collection, remoteId)?.basePayload?.let(SyncPayload::decode).orEmpty()
        val localPhoto = photos.prepare(account, remoteId, RemotePhoto(data["photoStorageUrl"] as? String, data["photoVersion"] as? String), existing?.photoUri, RemotePhoto(baseline["photoStorageUrl"] as? String, baseline["photoVersion"] as? String))
        return data + mapOf("localPhotoUri" to localPhoto)
    }

    override suspend fun apply(account: ActiveAccount, remoteId: String, data: Map<String, Any?>?) {
        val dao = database.scheduledTransactionDao()
        val existing = dao.getScheduledTransactionByFirestoreId(remoteId)
        if (data == null) {
            if (existing != null) dao.deleteScheduledTransaction(existing)
            return
        }
        val previousUrl = database.syncRecordDao().get(account.ownerId, collection, remoteId)?.basePayload?.let(SyncPayload::decode)?.get("photoStorageUrl")
        val incomingUrl = data["photoStorageUrl"] as? String
        val localPhoto = existing?.photoUri?.takeUnless { it.startsWith("http://") || it.startsWith("https://") }
        val photo = if (data["photoRemoved"] == true) null else data["localPhotoUri"] as? String ?: if (localPhoto != null && (incomingUrl == null || incomingUrl == previousUrl)) localPhoto else incomingUrl ?: existing?.photoUri
        val next = model(account, remoteId, data).toEntity().copy(id = existing?.id ?: 0L, photoUri = photo, notificationSent = existing?.notificationSent ?: false, expirationNotificationSent = existing?.expirationNotificationSent ?: false)
        dao.insertScheduledTransaction(next)
    }
}
