package com.ahmetkaragunlu.financeai.roomdb.database

import TransactionDao
import androidx.room.Database
import androidx.room.RoomDatabase
import com.ahmetkaragunlu.financeai.roomdb.model.TransactionEntity


    @Database(
        entities = [TransactionEntity::class],
        version = 1,
        exportSchema = false
    )
    abstract class FinanceDatabase : RoomDatabase() {
        abstract fun TransactionDao(): TransactionDao
    }

