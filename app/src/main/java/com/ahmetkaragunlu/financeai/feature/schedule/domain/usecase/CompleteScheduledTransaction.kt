package com.ahmetkaragunlu.financeai.feature.schedule.domain.usecase

import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction

interface CompleteScheduledTransaction {
    suspend operator fun invoke(value: ScheduledTransaction): Transaction?
}
