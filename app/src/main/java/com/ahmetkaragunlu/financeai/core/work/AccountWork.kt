package com.ahmetkaragunlu.financeai.core.work

/** Shared cancellation tag; unique work names remain owned by their schedulers. */
object AccountWork {
    fun tag(ownerId: String): String = "account_$ownerId"
}
