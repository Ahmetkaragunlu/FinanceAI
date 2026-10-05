package com.ahmetkaragunlu.financeai.feature.budget.data.remote

import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.money.MoneyAmounts
import com.ahmetkaragunlu.financeai.core.session.ActiveAccount
import com.ahmetkaragunlu.financeai.core.sync.RemoteRecordStore
import com.ahmetkaragunlu.financeai.feature.budget.data.mapper.*
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.*
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.*
import javax.inject.Inject

class BudgetRemoteStore @Inject constructor(private val database: FinanceDatabase) : RemoteRecordStore {
    override val collection = "budgets"
    private fun model(account: ActiveAccount, remoteId: String, data: Map<String, Any?>): Budget {
        val currency = data["currencyCode"] as? String ?: account.currencyCode
        require(currency == account.currencyCode) { "Currency mismatch" }
        return Budget(
            ownerId = account.ownerId, currencyCode = currency, firestoreId = remoteId, amount = (data["amountMinor"] as? Number)?.let { MoneyAmounts.toMajor(MoneyAmounts.readMinor(it), currency) } ?: (data["amount"] as? Number)?.toDouble() ?: 0.0,
            budgetType = BudgetType.valueOf(data["budgetType"] as? String ?: "CATEGORY_AMOUNT"),
            category = (data["category"] as? String)?.let(CategoryType::valueOf),
            limitPercentage = (data["limitPercentage"] as? Number)?.toDouble(),
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
