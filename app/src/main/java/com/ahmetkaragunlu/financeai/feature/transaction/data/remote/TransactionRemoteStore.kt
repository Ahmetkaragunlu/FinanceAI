package com.ahmetkaragunlu.financeai.feature.transaction.data.remote

import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.ahmetkaragunlu.financeai.core.firebase.FirestoreCollections
import com.ahmetkaragunlu.financeai.core.media.PhotoFields
import com.ahmetkaragunlu.financeai.core.media.remote.PhotoRemoteCache
import com.ahmetkaragunlu.financeai.core.media.remote.PhotoRemotePolicy
import com.ahmetkaragunlu.financeai.core.money.MoneyAmounts
import com.ahmetkaragunlu.financeai.core.session.ActiveAccount
import com.ahmetkaragunlu.financeai.core.sync.contract.FinancialFields
import com.ahmetkaragunlu.financeai.core.sync.contract.RemoteRecordStore
import com.ahmetkaragunlu.financeai.core.sync.contract.SyncPayload
import com.ahmetkaragunlu.financeai.feature.transaction.data.mapper.toEntity
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
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
        model(account, "", data).toFirebaseMap() + PhotoRemotePolicy.normalize(data)

    override suspend fun prepare(account: ActiveAccount, remoteId: String, data: Map<String, Any?>): Map<String, Any?> {
        if (data[PhotoFields.REMOVED] == true) return data
        val existing = database.transactionDao().getTransactionByFirestoreId(remoteId)
        val baseline = database.syncRecordDao().get(account.ownerId, collection, remoteId)?.basePayload?.let(SyncPayload::decode).orEmpty()
        return PhotoRemotePolicy.prepare(photos, account, remoteId, data, existing?.photoUri, baseline)
    }

    override suspend fun apply(account: ActiveAccount, remoteId: String, data: Map<String, Any?>?) {
        val dao = database.transactionDao()
        val existing = dao.getTransactionByFirestoreId(remoteId)
        if (data == null) {
            if (existing != null) dao.deleteTransaction(existing)
            return
        }
        val previousUrl = database.syncRecordDao().get(account.ownerId, collection, remoteId)?.basePayload?.let(SyncPayload::decode)?.get(PhotoFields.STORAGE_URL)
        val photo = PhotoRemotePolicy.select(data, existing?.photoUri, previousUrl)
        val next = model(account, remoteId, data).toEntity().copy(id = existing?.id ?: 0, photoUri = photo)
        dao.insertTransaction(next)
    }
}
