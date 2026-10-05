package com.ahmetkaragunlu.financeai.feature.aichat.data.repository

import com.ahmetkaragunlu.financeai.core.firebase.FirestoreCollections

import android.content.Context
import androidx.room.withTransaction
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.coroutines.di.DefaultDispatcher
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.format.DateFormatter
import com.ahmetkaragunlu.financeai.core.money.MoneyAmounts
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.session.ActiveAccount
import com.ahmetkaragunlu.financeai.core.sync.PendingChanges
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.ahmetkaragunlu.financeai.feature.aichat.data.local.dao.AiMessageDao
import com.ahmetkaragunlu.financeai.feature.aichat.data.local.entity.AiMessageEntity
import com.ahmetkaragunlu.financeai.feature.aichat.data.mapper.toDomain
import com.ahmetkaragunlu.financeai.feature.aichat.data.remote.toFirebaseMap
import com.ahmetkaragunlu.financeai.feature.aichat.domain.model.AiMessage
import com.ahmetkaragunlu.financeai.feature.aichat.domain.repository.AiRepository
import com.ahmetkaragunlu.financeai.feature.budget.domain.calculation.calculateBudgetUsagePercentage
import com.ahmetkaragunlu.financeai.feature.budget.domain.calculation.calculateCategoryBudgetLimit
import com.ahmetkaragunlu.financeai.feature.budget.domain.repository.BudgetRepository
import com.ahmetkaragunlu.financeai.feature.transaction.data.local.dao.TransactionDao
import com.ahmetkaragunlu.financeai.feature.transaction.data.mapper.toDomain
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.FinancialSummary
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.mapper.toResId
import com.google.ai.client.generativeai.GenerativeModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AiRepositoryImpl @Inject constructor(
    private val generativeModel: GenerativeModel,
    private val aiMessageDao: AiMessageDao,
    private val transactionDao: TransactionDao,
    private val budgetRepository: BudgetRepository,
    private val database: FinanceDatabase,
    private val session: AccountSession,
    private val pendingChanges: PendingChanges,
    private val scheduler: SyncScheduler,
    @ApplicationContext private val context: Context,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : AiRepository {

    override fun observeChatHistory(): Flow<List<AiMessage>> {
        return session.observe(emptyList()) { aiMessageDao.observeMessages().map { messages -> messages.map { it.toDomain() } } }
    }

    override suspend fun sendMessage(userMessage: String) {
        val account = session.requireAccount()
        saveMessage(account, userMessage, false)
        try {
            val financialReport = withContext(defaultDispatcher) { prepareFinancialReport(account) }
            if (!session.isCurrent(account)) throw CancellationException("Stale account")
            val instruction = context.getString(R.string.ai_detailed_system_instruction, financialReport)
            val question = context.getString(R.string.ai_user_question_prefix, userMessage)
            val response = generativeModel.generateContent("$instruction\n\n$question")
            saveMessage(account, response.text ?: context.getString(R.string.ai_response_error_empty), true)
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            if (session.isCurrent(account)) saveMessage(account,
                context.getString(R.string.ai_response_error_generic, ""), true)
        }
    }

    private suspend fun saveMessage(account: ActiveAccount, text: String, isAi: Boolean) {
        session.withAccount { active ->
            check(active == account) { "Stale account" }
            val entity = AiMessageEntity(ownerId = active.ownerId, text = text, isAi = isAi,
                firebaseId = UUID.randomUUID().toString())
            database.withTransaction {
                aiMessageDao.insertMessage(entity)
                pendingChanges.record(active.ownerId, FirestoreCollections.AI_MESSAGES, entity.firebaseId!!, entity.toFirebaseMap())
            }
            scheduler.enqueue(active.ownerId)
        }
    }

    private suspend fun prepareFinancialReport(account: ActiveAccount): String {
        val (allTransactions, allBudgets) = session.withAccount { active ->
            check(active == account) { "Stale account" }
            database.withTransaction {
                transactionDao.getAllTransactionsOneShot().map { it.toDomain() } to budgetRepository.getAllBudgetsOneShot()
            }
        }

        if (allTransactions.isEmpty()) return context.getString(R.string.no_record_found)
        val currentDate = DateFormatter.formatRelativeDate(context, System.currentTimeMillis())

        fun Iterable<Transaction>.totalMoney() =
            MoneyAmounts.sum(map { it.amount }, account.currencyCode)

        val totalIncome =
            allTransactions.filter { it.transaction == TransactionType.INCOME }.totalMoney()
        val totalExpense =
            allTransactions.filter { it.transaction == TransactionType.EXPENSE }.totalMoney()
        val budgetReportBuilder = StringBuilder()

        val month = DateFormatter.getCurrentMonthRange()
        val monthlyTransactions = allTransactions.filter { it.date >= month.first && it.date < month.second }
        val monthlyExpense = monthlyTransactions.filter { it.transaction == TransactionType.EXPENSE }.totalMoney()
        val expenseByCategory = monthlyTransactions
            .filter { it.transaction == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.totalMoney() }
        budgetReportBuilder.append(context.getString(R.string.report_section_budget)).append("\n")

        val generalBudget = allBudgets.find { it.category == null }
        if (generalBudget != null) {
            val limit = generalBudget.amount
            val percentage = calculateBudgetUsagePercentage(monthlyExpense, limit)
            budgetReportBuilder.append(
                context.getString(
                    R.string.report_general_budget_item,
                    limit.toString(),
                    monthlyExpense.toString(),
                    percentage.toInt()
                )
            ).append("\n")
        }

        allBudgets.forEach { budget ->
            if (budget.category != null) {
                val localizedCatName = context.getString(budget.category.toResId())

                val spent = expenseByCategory[budget.category] ?: 0.0
                val limit = calculateCategoryBudgetLimit(budget, generalBudget)
                val percentage = calculateBudgetUsagePercentage(spent, limit)

                budgetReportBuilder.append(
                    context.getString(
                        R.string.report_category_budget_item,
                        localizedCatName,
                        limit.toString(),
                        spent.toString(),
                        percentage.toInt()
                    )
                ).append("\n")
            }
        }

        val transactionListString = allTransactions.joinToString(separator = "\n") { t ->
            val localType = if (t.transaction == TransactionType.INCOME)
                context.getString(R.string.income)
            else
                context.getString(R.string.expense)
            val localCat = context.getString(t.category.toResId())
            val dateStr = DateFormatter.formatRelativeDate(context, t.date)
            context.getString(
                R.string.report_transaction_item_format,
                dateStr,
                localType,
                localCat,
                t.amount.toString(),
                t.note
            )
        }

        return """
            ${account.currencyCode}
            ${context.getString(R.string.report_header_date, currentDate)}
            ${context.getString(R.string.report_general_status_title)}
            ${context.getString(R.string.report_total_income, totalIncome.toString())}
            ${context.getString(R.string.report_total_expense, totalExpense.toString())}
            ${
            context.getString(
                R.string.report_net_status,
                FinancialSummary(totalIncome, totalExpense).remainingBalance.toString()
            )
        }
            $budgetReportBuilder
            ${context.getString(R.string.report_section_history)}
            $transactionListString
        """.trimIndent()
    }
}
