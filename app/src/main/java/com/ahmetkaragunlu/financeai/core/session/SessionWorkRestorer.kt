package com.ahmetkaragunlu.financeai.core.session

/** The application rebuilds its disposable workers from account-owned durable records. */
interface SessionWorkRestorer {
    suspend fun restore(account: ActiveAccount)
}
