package com.ahmetkaragunlu.financeai.core.session

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ActiveAccount(
    val ownerId: String,
    val currencyCode: String,
    val generation: Long,
    val timeZoneId: String = "UTC"
)

@Singleton
class AccountSession @Inject constructor() {
    private val mutableAccount = MutableStateFlow<ActiveAccount?>(null)
    val account = mutableAccount.asStateFlow()
    private val mutex = Mutex()
    private var generation = 0L

    fun requireAccount(): ActiveAccount = checkNotNull(account.value) { "Account not ready" }
    fun isCurrent(account: ActiveAccount): Boolean = this.account.value == account
    internal fun activate(ownerId: String, currencyCode: String, timeZoneId: String = "UTC") {
        mutableAccount.value = ActiveAccount(ownerId, currencyCode, ++generation, timeZoneId)
    }

    internal fun deactivate() {
        generation++; mutableAccount.value = null
    }

    /** Preparation/logout may use the same guard while no active account exists. */
    internal suspend fun <T> withStateLock(block: suspend () -> T): T = mutex.withLock { block() }

    suspend fun <T> withAccount(block: suspend (ActiveAccount) -> T): T = withStateLock {
        block(requireAccount())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun <T> observe(empty: T, source: (ActiveAccount) -> Flow<T>): Flow<T> =
        account.flatMapLatest { owner ->
            if (owner == null) flowOf(empty)
            else source(owner).filter { isCurrent(owner) }
        }
}
