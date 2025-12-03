package com.ahmetkaragunlu.financeai.roomdb.converters

import androidx.room.TypeConverter
import com.ahmetkaragunlu.financeai.roomdb.type.BudgetType
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType

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
    fun fromBudgetType(value: BudgetType): String {
        return value.name
    }

    @TypeConverter
    fun toBudgetType(value: String): BudgetType {
        return try {
            BudgetType.valueOf(value)
        } catch (e: Exception) {
            BudgetType.GENERAL_MONTHLY
        }
    }
}