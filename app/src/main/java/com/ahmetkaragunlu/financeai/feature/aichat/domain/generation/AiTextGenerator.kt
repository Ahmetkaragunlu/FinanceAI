package com.ahmetkaragunlu.financeai.feature.aichat.domain.generation

import com.ahmetkaragunlu.financeai.feature.aichat.domain.model.FinancialSnapshot

/** External model boundary; SDK and prompt representation stay in data. */
interface AiTextGenerator {
    suspend fun generate(question: String, snapshot: FinancialSnapshot): String
}
