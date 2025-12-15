package com.ahmetkaragunlu.financeai.ai_repository

import android.content.Context
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.di.module.IoDispatcher
import com.ahmetkaragunlu.financeai.firebasesync.FirebaseSyncService
import com.ahmetkaragunlu.financeai.roomdb.dao.AiMessageDao
import com.ahmetkaragunlu.financeai.roomdb.dao.TransactionDao
import com.ahmetkaragunlu.financeai.roomdb.entitiy.AiMessageEntity
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.roomrepository.budgetrepositroy.BudgetRepository
import com.ahmetkaragunlu.financeai.utils.DateFormatter
import com.ahmetkaragunlu.financeai.utils.toResId
import com.google.ai.client.generativeai.GenerativeModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AiRepositoryImpl @Inject constructor(
    private val generativeModel: GenerativeModel,
    private val aiMessageDao: AiMessageDao,
    private val transactionDao: TransactionDao,
    private val budgetRepository: BudgetRepository,
    private val firebaseSyncService: FirebaseSyncService,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AiRepository {

    override fun getChatHistory(): Flow<List<AiMessageEntity>> {
        return aiMessageDao.getAllMessages()
    }

    override suspend fun sendMessage(userMessage: String) {
        withContext(ioDispatcher) {
            val userEntity = AiMessageEntity(text = userMessage, isAi = false, isSynced = false)
            val userRowId = aiMessageDao.insertMessage(userEntity)

            val userEntityWithId = userEntity.copy(id = userRowId)
            firebaseSyncService.syncAiMessageToFirebase(userEntityWithId).onSuccess { firebaseId ->
                aiMessageDao.updateSyncStatus(userRowId, firebaseId)
            }
            try {
                val financialReport = prepareFinancialReport()
                val systemInstructionTemplate =
                    context.getString(R.string.ai_detailed_system_instruction, financialReport)
                val userQuestionPrefix =
                    context.getString(R.string.ai_user_question_prefix, userMessage)
                val fullPrompt = "$systemInstructionTemplate\n\n$userQuestionPrefix"
                val response = generativeModel.generateContent(fullPrompt)
                val responseText =
                    response.text ?: context.getString(R.string.ai_response_error_empty)

                val aiEntity = AiMessageEntity(text = responseText, isAi = true, isSynced = false)
                val aiRowId = aiMessageDao.insertMessage(aiEntity)

                val aiEntityWithId = aiEntity.copy(id = aiRowId)
                firebaseSyncService.syncAiMessageToFirebase(aiEntityWithId)
                    .onSuccess { firebaseId ->
                        aiMessageDao.updateSyncStatus(aiRowId, firebaseId)
                    }
            } catch (e: Exception) {
                val errorEntity = AiMessageEntity(
                    text = context.getString(
                        R.string.ai_response_error_generic,
                        e.localizedMessage
                    ),
                    isAi = true,
                    isSynced = false
                )
                aiMessageDao.insertMessage(errorEntity)
            }
        }
    }

    private suspend fun prepareFinancialReport(): String {
        val allTransactions = transactionDao.getAllTransactionsOneShot()
        val allBudgets = budgetRepository.getAllBudgetsOneShot()

        if (allTransactions.isEmpty()) return context.getString(R.string.no_record_found)
        val currentDate = DateFormatter.formatRelativeDate(context, System.currentTimeMillis())

        val totalIncome =
            allTransactions.filter { it.transaction == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense =
            allTransactions.filter { it.transaction == TransactionType.EXPENSE }.sumOf { it.amount }
        val budgetReportBuilder = StringBuilder()

        val expenseByCategory = allTransactions
            .filter { it.transaction == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        budgetReportBuilder.append(context.getString(R.string.report_section_budget)).append("\n")

        val generalBudget = allBudgets.find { it.category == null }
        if (generalBudget != null) {
            val limit = generalBudget.amount
            val percentage = if (limit > 0) (totalExpense / limit) * 100 else 0.0
            budgetReportBuilder.append(
                context.getString(
                    R.string.report_general_budget_item,
                    limit.toString(),
                    totalExpense.toString(),
                    percentage.toInt()
                )
            ).append("\n")
        }

        allBudgets.forEach { budget ->
            if (budget.category != null) {
                val localizedCatName = context.getString(budget.category.toResId())

                val spent = expenseByCategory[budget.category] ?: 0.0
                val limit = budget.amount
                val percentage = if (limit > 0) (spent / limit) * 100 else 0.0

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
            ${context.getString(R.string.report_header_date, currentDate)}
            ${context.getString(R.string.report_general_status_title)}
            ${context.getString(R.string.report_total_income, totalIncome.toString())}
            ${context.getString(R.string.report_total_expense, totalExpense.toString())}
            ${
            context.getString(
                R.string.report_net_status,
                (totalIncome - totalExpense).toString()
            )
        }
            $budgetReportBuilder
            ${context.getString(R.string.report_section_history)}
            $transactionListString
        """.trimIndent()
    }
}