package com.ahmetkaragunlu.financeai.core.session.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "active_account")
data class ActiveAccountRow(@PrimaryKey val id: Int = 0, val ownerId: String)
