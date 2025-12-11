package com.ahmetkaragunlu.financeai.ai_repository

import android.content.Context
import com.ahmetkaragunlu.financeai.firebasesync.FirebaseSyncService
import com.ahmetkaragunlu.financeai.roomdb.dao.AiMessageDao
import com.ahmetkaragunlu.financeai.roomdb.dao.TransactionDao
import com.ahmetkaragunlu.financeai.roomdb.entitiy.AiMessageEntity
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.roomrepository.budgetrepositroy.BudgetRepository
import com.ahmetkaragunlu.financeai.utils.DateFormatter
import com.google.ai.client.generativeai.GenerativeModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AiRepositoryImpl @Inject constructor(
    private val generativeModel: GenerativeModel,
    private val aiMessageDao: AiMessageDao,
    private val transactionDao: TransactionDao,
    private val budgetRepository: BudgetRepository,
    private val firebaseSyncService: FirebaseSyncService,
    @ApplicationContext private val context: Context
) : AiRepository {

    override fun getChatHistory(): Flow<List<AiMessageEntity>> {
        return aiMessageDao.getAllMessages()
    }

    override suspend fun sendMessage(userMessage: String) {
        withContext(Dispatchers.IO) {
            val userEntity = AiMessageEntity(text = userMessage, isAi = false, isSynced = false)
            val userRowId = aiMessageDao.insertMessage(userEntity)
            val userEntityWithId = userEntity.copy(id = userRowId)
            firebaseSyncService.syncAiMessageToFirebase(userEntityWithId).onSuccess { firebaseId ->
                aiMessageDao.updateSyncStatus(userRowId, firebaseId)
            }

            try {
                val financialReport = prepareFinancialReport()

                val systemInstruction = """
                    SENİN ROLÜN:
                    Sen FinanceAI uygulamasının zeki, yardımsever ve finansal asistanısın.
                    Kullanıcının TÜM verilerine (İşlemler, Bütçeler) aşağıdaki raporda sahipsin.
                    
                    ÖNEMLİ KURALLAR:
                    - Sadece maddelerde * kullan ve cümlenin başında olsun 1 tane olsun cümlenin sonuna koyma her yer de bunu kullanma .
                    - Rolunden farklı bir şey sorulursa Merhaba Ben Finance AI sadece finansla alakalı sorulara cevap verebilirim diye cevap ver.
                    -Kategorilerin basına ve sonuna * koyma
                    - Hangi dilde soru sorulmuşsa kategorileri ve yanıtlarını o dile çevir.
      
                    GÖREVLERİN:
                    1. "Bu ay nereye harcadım?" / Aylık Özet: - Rapor tarihini dikkate alarak içinde bulunulan ayı tespit et.
                       - Kategori bazında toplamları söyle.
                    2. "Risk Analizi" / Bütçe Kontrolü:
                       - "BÜTÇE VE LİMİTLER" kısmına bak.
                       - 'Limit' ile 'Harcanan'ı karşılaştır.
                       - Limitine yaklaşan (%80 üzeri) veya geçenleri uyar.
                    3. Genel Sohbet:
                       - Verilen rapor dışındaki konularda genel finans bilgisi ver.
                       - Asla "veriye erişemiyorum" deme, veri aşağıda.
                    
                    KULLANICI VERİ RAPORU:
                    $financialReport
                """.trimIndent()

                val fullPrompt = "$systemInstruction\n\nKULLANICI SORUSU: $userMessage"
                val response = generativeModel.generateContent(fullPrompt)
                val responseText = response.text ?: "Cevap üretilemedi."

                val aiEntity = AiMessageEntity(text = responseText, isAi = true, isSynced = false)
                val aiRowId = aiMessageDao.insertMessage(aiEntity)

                val aiEntityWithId = aiEntity.copy(id = aiRowId)
                firebaseSyncService.syncAiMessageToFirebase(aiEntityWithId).onSuccess { firebaseId ->
                    aiMessageDao.updateSyncStatus(aiRowId, firebaseId)
                }

            } catch (e: Exception) {
                val errorEntity = AiMessageEntity(text = "Hata oluştu: ${e.localizedMessage}", isAi = true, isSynced = false)
                aiMessageDao.insertMessage(errorEntity)
            }
        }
    }

    private suspend fun prepareFinancialReport(): String {
        val allTransactions = transactionDao.getAllTransactionsOneShot()

        // B. Tüm Bütçeler
        val allBudgets = budgetRepository.getAllBudgetsOneShot()

        if (allTransactions.isEmpty()) return "Kullanıcının henüz hiç işlemi yok."


        val currentDate = DateFormatter.formatRelativeDate(context, System.currentTimeMillis())

        // D. Toplamlar
        val totalIncome = allTransactions.filter { it.transaction == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = allTransactions.filter { it.transaction == TransactionType.EXPENSE }.sumOf { it.amount }

        // E. Bütçe Raporu
        val budgetReportBuilder = StringBuilder()

        // Kategori bazlı harcama hesapla
        val expenseByCategory = allTransactions
            .filter { it.transaction == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        budgetReportBuilder.append("--- BÜTÇE VE LİMİTLER ---\n")

        val generalBudget = allBudgets.find { it.category == null }
        if (generalBudget != null) {
            val limit = generalBudget.amount
            val percentage = if(limit > 0) (totalExpense / limit) * 100 else 0.0
            budgetReportBuilder.append("- GENEL BÜTÇE: Limit $limit TL, Toplam Harcanan: $totalExpense TL, Durum: %${percentage.toInt()} kullanıldı.\n")
        }

        // Kategori Bütçeleri
        allBudgets.forEach { budget ->
            if (budget.category != null) {
                val spent = expenseByCategory[budget.category] ?: 0.0
                val limit = budget.amount
                val percentage = if(limit > 0) (spent / limit) * 100 else 0.0
                budgetReportBuilder.append("- Kategori: ${budget.category}, Limit: $limit TL, Harcanan: $spent TL, Durum: %${percentage.toInt()} kullanıldı.\n")
            }
        }


        val transactionListString = allTransactions.joinToString(separator = "\n") { t ->
            "[${DateFormatter.formatRelativeDate(context, t.date)}] ${t.transaction} - ${t.category}: ${t.amount} TL (${t.note})"
        }

        return """
            RAPOR TARİHİ: $currentDate
            GENEL DURUM:
            Toplam Gelir: $totalIncome TL
            Toplam Gider: $totalExpense TL
            Net Durum: ${totalIncome - totalExpense} TL
            
            $budgetReportBuilder
            
            İŞLEM GEÇMİŞİ:
            $transactionListString
        """.trimIndent()
    }
}