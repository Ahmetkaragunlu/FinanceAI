package com.ahmetkaragunlu.financeai.core.time

import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

@Singleton
class FinanceCalendar @Inject constructor(private val clock: Clock) {
    private val refreshes = MutableStateFlow(0L)
    fun refresh() { refreshes.value++ }
    private fun ticks() = flow {
        while (true) {
            emit(clock.millis())
            val range = FinancePeriods.month(clock.withZone(ZoneId.systemDefault()))
            delay(minOf(60_000L, (range.endExclusive - clock.millis()).coerceAtLeast(1L)))
        }
    }
    fun observeFilterRange(filter: DateFilter) = combine(ticks(), refreshes) { _, _ ->
        FinancePeriods.filter(filter, clock.withZone(ZoneId.systemDefault()))
    }.distinctUntilChanged()
    fun observeMonth() = combine(ticks(), refreshes) { _, _ ->
        FinancePeriods.month(clock.withZone(ZoneId.systemDefault()))
    }.distinctUntilChanged()
}
