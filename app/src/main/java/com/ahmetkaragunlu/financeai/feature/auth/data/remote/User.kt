package com.ahmetkaragunlu.financeai.feature.auth.data.remote

data class User(
    val firstName : String = "",
    val lastName : String = "",
    val email : String = "",
    val uid : String = "",
    val fcmTokens: List<String> = emptyList()
)
