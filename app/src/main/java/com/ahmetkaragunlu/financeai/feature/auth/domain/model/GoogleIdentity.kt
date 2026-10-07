package com.ahmetkaragunlu.financeai.feature.auth.domain.model

/** SDK-free credential selected by the user; never saved in SavedState or logs. */
data class GoogleIdentity(val email: String, val idToken: String)
