package com.ahmetkaragunlu.financeai.feature.auth.data.local.session

fun interface CredentialSessionCleaner {
    suspend fun clear()
}
