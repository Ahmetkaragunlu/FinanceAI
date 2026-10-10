package com.ahmetkaragunlu.financeai.feature.aichat.data.report

import androidx.room.withTransaction
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.session.ActiveAccount
import com.ahmetkaragunlu.financeai.core.time.FinancePeriods
import com.ahmetkaragunlu.financeai.feature.aichat.domain.model.FinancialSnapshot
import com.ahmetkaragunlu.financeai.feature.budget.data.mapper.toDomain
import com.ahmetkaragunlu.financeai.feature.transaction.data.mapper.toDomain
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class RoomFinancialSnapshotSource @Inject constructor(
    private val database: FinanceDatabase,
    private val session: AccountSession,
    private val clock: Clock
) : FinancialSnapshotSource {
    override suspend fun read(account: ActiveAccount): FinancialSnapshot =
        session.withAccount { current ->
            if (current != account) throw CancellationException("Stale account")
            val at = clock.millis()
            val zone = ZoneId.systemDefault()
            // Same device-zone calendar month as Home/Budget; reminder account zone is a separate policy.
            val month = FinancePeriods.month(Clock.fixed(Instant.ofEpochMilli(at), zone))
            database.withTransaction {
                FinancialSnapshot(
                    account.currencyCode, at, month.start, month.endExclusive,
                    database.transactionDao()
                        .getTransactionsByDateRangeOneShot(month.start, month.endExclusive)
                        .map { it.toDomain() },
                    database.budgetDao().getAllBudgetsOneShot().map { it.toDomain() }, zone.id
                )
            }
        }
}
