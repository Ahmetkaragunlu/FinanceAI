package com.ahmetkaragunlu.financeai.feature.budget.data.remote

import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.ahmetkaragunlu.financeai.core.firebase.FirestoreCollections
import com.ahmetkaragunlu.financeai.core.money.MoneyAmounts
import com.ahmetkaragunlu.financeai.core.session.ActiveAccount
import com.ahmetkaragunlu.financeai.core.sync.contract.FinancialFields
import com.ahmetkaragunlu.financeai.core.sync.contract.RemoteRecordStore
import com.ahmetkaragunlu.financeai.feature.budget.data.mapper.toEntity
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.Budget
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.BudgetType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import javax.inject.Inject

class BudgetRemoteStore @Inject constructor(private val database: FinanceDatabase) : RemoteRecordStore {
    override val collection = FirestoreCollections.BUDGETS
    private fun model(account: ActiveAccount, remoteId: String, data: Map<String, Any?>): Budget {
        val currency = data[FinancialFields.CURRENCY_CODE] as? String ?: account.currencyCode
        if (currency != account.currencyCode) throw DataAccessException.InvalidRemoteData()
        return Budget(
            ownerId = account.ownerId, currencyCode = currency, firestoreId = remoteId, amount = (data[FinancialFields.AMOUNT_MINOR] as? Number)?.let { MoneyAmounts.toMajor(MoneyAmounts.readMinor(it), currency) } ?: (data[FinancialFields.LEGACY_AMOUNT] as? Number)?.toDouble() ?: 0.0,
            budgetType = BudgetType.valueOf(data[BudgetFields.TYPE] as? String ?: "CATEGORY_AMOUNT"),
            category = (data[FinancialFields.CATEGORY] as? String)?.let(CategoryType::valueOf),
            limitPercentage = (data[BudgetFields.LIMIT_PERCENTAGE] as? Number)?.toDouble(),
            syncedToFirebase = true
        )
    }
    override fun normalize(data: Map<String, Any?>, account: ActiveAccount): Map<String, Any?> =
        model(account, "", data).toFirebaseMap()

    override suspend fun apply(account: ActiveAccount, remoteId: String, data: Map<String, Any?>?) {
        val dao = database.budgetDao()
        val existing = dao.getBudgetByFirestoreId(remoteId)
        if (data == null) {
            if (existing != null) dao.deleteBudget(existing)
            return
        }
        val next = model(account, remoteId, data).toEntity().copy(id = existing?.id ?: 0)
        dao.insertBudget(next)
    }
}
