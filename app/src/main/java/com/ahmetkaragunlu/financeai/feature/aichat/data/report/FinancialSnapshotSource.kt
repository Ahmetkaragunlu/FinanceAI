package com.ahmetkaragunlu.financeai.feature.aichat.data.report

import com.ahmetkaragunlu.financeai.core.session.ActiveAccount
import com.ahmetkaragunlu.financeai.feature.aichat.domain.model.FinancialSnapshot

interface FinancialSnapshotSource {
    suspend fun read(account: ActiveAccount): FinancialSnapshot
}
