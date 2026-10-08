package com.ahmetkaragunlu.financeai.feature.transaction.data.remote

import com.ahmetkaragunlu.financeai.core.firebase.FirestoreCollections
import com.ahmetkaragunlu.financeai.core.sync.contract.FinancialFields
import com.ahmetkaragunlu.financeai.core.media.PhotoFields
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.ahmetkaragunlu.financeai.core.media.remote.PhotoRemoteCache
import com.ahmetkaragunlu.financeai.core.media.remote.RemotePhoto
import com.ahmetkaragunlu.financeai.core.money.MoneyAmounts
import com.ahmetkaragunlu.financeai.core.session.ActiveAccount
import com.ahmetkaragunlu.financeai.core.sync.contract.RemoteRecordStore
import com.ahmetkaragunlu.financeai.core.sync.contract.SyncPayload
import com.ahmetkaragunlu.financeai.feature.transaction.data.mapper.*
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.*
import javax.inject.Inject

class TransactionRemoteStore @Inject constructor(private val database: FinanceDatabase, private val photos: PhotoRemoteCache) : RemoteRecordStore {
    override val collection = FirestoreCollections.TRANSACTIONS
    private fun model(account: ActiveAccount, remoteId: String, data: Map<String, Any?>): Transaction {
        val currency = data[FinancialFields.CURRENCY_CODE] as? String ?: account.currencyCode
        if (currency != account.currencyCode) throw DataAccessException.InvalidRemoteData()
        return Transaction(
            ownerId = account.ownerId, currencyCode = currency, firestoreId = remoteId, amount = (data[FinancialFields.AMOUNT_MINOR] as? Number)?.let { MoneyAmounts.toMajor(MoneyAmounts.readMinor(it), currency) } ?: (data[FinancialFields.LEGACY_AMOUNT] as? Number)?.toDouble() ?: 0.0,
            transaction = TransactionType.valueOf(data[TransactionFields.TYPE] as? String ?: "EXPENSE"),
            category = CategoryType.valueOf(data[FinancialFields.CATEGORY] as? String ?: "OTHER"),
            note = data[FinancialFields.NOTE] as? String ?: "",
            date = (data[TransactionFields.DATE] as? Number)?.toLong() ?: 0L,
            locationFull = data[FinancialFields.LOCATION_FULL] as? String,
            locationShort = data[FinancialFields.LOCATION_SHORT] as? String,
            latitude = (data[FinancialFields.LATITUDE] as? Number)?.toDouble(),
            longitude = (data[FinancialFields.LONGITUDE] as? Number)?.toDouble(),
            syncedToFirebase = true
        )
    }
    override fun normalize(data: Map<String, Any?>, account: ActiveAccount): Map<String, Any?> =
        model(account, "", data).toFirebaseMap() + mapOf(PhotoFields.STORAGE_URL to data[PhotoFields.STORAGE_URL], PhotoFields.REMOVED to (data[PhotoFields.REMOVED] == true), PhotoFields.VERSION to data[PhotoFields.VERSION], PhotoFields.INTENT to data[PhotoFields.INTENT])

    override suspend fun prepare(account: ActiveAccount, remoteId: String, data: Map<String, Any?>): Map<String, Any?> {
        if (data[PhotoFields.REMOVED] == true) return data
        val existing = database.transactionDao().getTransactionByFirestoreId(remoteId)
        val baseline = database.syncRecordDao().get(account.ownerId, collection, remoteId)?.basePayload?.let(SyncPayload::decode).orEmpty()
        val localPhoto = photos.prepare(account, remoteId, RemotePhoto(data[PhotoFields.STORAGE_URL] as? String, data[PhotoFields.VERSION] as? String), existing?.photoUri, RemotePhoto(baseline[PhotoFields.STORAGE_URL] as? String, baseline[PhotoFields.VERSION] as? String))
        return data + mapOf(PhotoFields.LOCAL_URI to localPhoto)
    }

    override suspend fun apply(account: ActiveAccount, remoteId: String, data: Map<String, Any?>?) {
        val dao = database.transactionDao()
        val existing = dao.getTransactionByFirestoreId(remoteId)
        if (data == null) {
            if (existing != null) dao.deleteTransaction(existing)
            return
        }
        val previousUrl = database.syncRecordDao().get(account.ownerId, collection, remoteId)?.basePayload?.let(SyncPayload::decode)?.get(PhotoFields.STORAGE_URL)
        val incomingUrl = data[PhotoFields.STORAGE_URL] as? String
        val localPhoto = existing?.photoUri?.takeUnless { it.startsWith("http://") || it.startsWith("https://") }
        val photo = if (data[PhotoFields.REMOVED] == true) null else data[PhotoFields.LOCAL_URI] as? String ?: if (localPhoto != null && (incomingUrl == null || incomingUrl == previousUrl)) localPhoto else incomingUrl ?: existing?.photoUri
        val next = model(account, remoteId, data).toEntity().copy(id = existing?.id ?: 0, photoUri = photo)
        dao.insertTransaction(next)
    }
}
