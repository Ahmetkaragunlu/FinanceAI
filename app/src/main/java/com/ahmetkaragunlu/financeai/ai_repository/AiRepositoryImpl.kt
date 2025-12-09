package com.ahmetkaragunlu.financeai.ai_repository

import android.annotation.SuppressLint
import android.content.Context
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.roomdb.dao.AiMessageDao
import com.ahmetkaragunlu.financeai.roomdb.dao.TransactionDao
import com.ahmetkaragunlu.financeai.roomdb.entitiy.AiMessageEntity
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
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
    @ApplicationContext private val context: Context // Stringleri XML'den almak için
) : AiRepository {

    override fun getChatHistory(): Flow<List<AiMessageEntity>> {
        // UI tarafında sürekli güncel kalsın diye Flow kullanıyoruz
        return aiMessageDao.getAllMessages()
    }

    override suspend fun sendMessage(userMessage: String) {
        withContext(Dispatchers.IO) {
            // 1. ADIM: Kullanıcının yazdığı mesajı önce veritabanına kaydet
            // (Henüz sunucuya gitmediği için isSynced = false)
            aiMessageDao.insertMessage(
                AiMessageEntity(
                    text = userMessage,
                    isAi = false,
                    isSynced = false
                )
            )

            try {
                // 2. ADIM: Finansal verileri hazırla (Context Injection)
                // Yapay zeka senin ne kadar harcadığını bilmeli.
                val contextPrompt = createFinancialContext()

                // 3. ADIM: Promptları XML'den çek ve birleştir
                // Kullanıcı sorusu: "Bu ay ne kadar harcadım?" -> XML şablonuna girer
                val finalUserPrompt = context.getString(R.string.ai_prompt_user_prefix, userMessage)

                // Gemini'ye gidecek nihai metin: [Finansal Veriler] + [Kullanıcı Sorusu]
                val fullRequest = "$contextPrompt\n\n$finalUserPrompt"

                // 4. ADIM: Gemini'ye istek gönder
                val response = generativeModel.generateContent(fullRequest)

                // 5. ADIM: Cevabı al (Eğer boş dönerse XML'deki hata mesajını kullan)
                val responseText = response.text ?: context.getString(R.string.ai_response_error_empty)

                // 6. ADIM: AI cevabını veritabanına kaydet
                aiMessageDao.insertMessage(
                    AiMessageEntity(
                        text = responseText,
                        isAi = true,
                        isSynced = false
                    )
                )

            } catch (e: Exception) {
                // Hata oluşursa (İnternet yok vs.) bunu da sohbet baloncuğu olarak ekle
                val errorText = context.getString(R.string.ai_response_error_generic, e.localizedMessage ?: "Bilinmeyen Hata")

                aiMessageDao.insertMessage(
                    AiMessageEntity(
                        text = errorText,
                        isAi = true,
                        isSynced = false
                    )
                )
            }
        }
    }

    /**
     * Yapay zekaya "Bağlam" (Context) oluşturmak için harcamaları analiz eder.
     * Flow yerine OneShot (Suspend) fonksiyon kullanırız.
     */
    @SuppressLint("StringFormatMatches")
    private suspend fun createFinancialContext(): String {
        // Veritabanından o anki harcamaların "fotoğrafını" çekiyoruz
        val transactions = transactionDao.getAllTransactionsOneShot()

        if (transactions.isEmpty()) {
            return context.getString(R.string.ai_prompt_no_data)
        }

        // --- DÜZELTİLEN KISIM BURASI ---
        val totalIncome = transactions
            .filter { it.transaction == TransactionType.INCOME }
            .sumOf { it.amount }

        val totalExpense = transactions
            .filter { it.transaction == TransactionType.EXPENSE }
            .sumOf { it.amount }
        // -------------------------------

        // Son 5 işlemi listele
        val recentTransactions = transactions.take(5).joinToString("\n") {
            "- ${it.category}: ${it.amount} (${context.getString(R.string.currency_symbol)})"
        }

        // XML'deki "Sistem Talimatı" stringini doldurarak geri döndür
        return context.getString(
            R.string.ai_prompt_system_instruction,
            totalIncome.toString(),
            totalExpense.toString(),
            recentTransactions
        )
    }
}