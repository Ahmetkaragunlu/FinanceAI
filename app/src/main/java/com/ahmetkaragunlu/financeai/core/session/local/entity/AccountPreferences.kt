package com.ahmetkaragunlu.financeai.core.session.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "account_preferences")
data class AccountPreferences(
    @PrimaryKey val ownerId: String,
    val currencyCode: String,
    val timeZoneId: String? = null
)
