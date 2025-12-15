package com.ahmetkaragunlu.financeai.roomdb.converters

import androidx.room.TypeConverter
import com.ahmetkaragunlu.financeai.roomdb.type.BudgetType
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import java.util.Date

class Converters {

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name
    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromCategoryType(value: CategoryType): String = value.name

    @TypeConverter
    fun toCategoryType(value: String): CategoryType = CategoryType.valueOf(value)

    @TypeConverter
    fun fromBudgetType(value: BudgetType): String = value.name

    @TypeConverter
    fun toBudgetType(value: String): BudgetType = BudgetType.valueOf(value)

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }


    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

}