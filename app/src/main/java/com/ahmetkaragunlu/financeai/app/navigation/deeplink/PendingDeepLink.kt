package com.ahmetkaragunlu.financeai.app.navigation.deeplink

/** Each delivery has its own identity, even when the URI is identical. */
data class PendingDeepLink(val id: String, val destination: FinanceDeepLink)
