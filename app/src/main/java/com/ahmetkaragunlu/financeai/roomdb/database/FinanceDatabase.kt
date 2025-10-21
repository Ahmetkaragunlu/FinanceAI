package com.ahmetkaragunlu.financeai.roomdb.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ahmetkaragunlu.financeai.roomdb.converters.Converters
import com.ahmetkaragunlu.financeai.roomdb.dao.TransactionDao
import com.ahmetkaragunlu.financeai.roomdb.entitiy.TransactionEntity


@Database(
    entities = [TransactionEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}