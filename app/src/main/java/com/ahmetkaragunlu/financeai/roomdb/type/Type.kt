package com.ahmetkaragunlu.financeai.roomdb.type

import com.ahmetkaragunlu.financeai.R

enum class TransactionType {
    INCOME,
    EXPENSE
}

enum class CategoryType(val type: TransactionType) {
    // Expense categories
    FOOD(TransactionType.EXPENSE),
    GROCERIES(TransactionType.EXPENSE),
    COFFEE_TEA(TransactionType.EXPENSE),
    DESSERT_SWEETS(TransactionType.EXPENSE),
    TRANSPORT(TransactionType.EXPENSE),
    RENT(TransactionType.EXPENSE),
    ENTERTAINMENT(TransactionType.EXPENSE),
    HEALTH(TransactionType.EXPENSE),
    BILLS(TransactionType.EXPENSE),
    CLOTHING(TransactionType.EXPENSE),
    EDUCATION(TransactionType.EXPENSE),
    HOME_DECORATION(TransactionType.EXPENSE),
    GIFTS_DONATION(TransactionType.EXPENSE),
    OTHER(TransactionType.EXPENSE),

    // Income categories
    SALARY(TransactionType.INCOME),
    SCHOLARSHIP(TransactionType.INCOME),
    FREELANCE(TransactionType.INCOME),
    INVESTMENT_INCOME(TransactionType.INCOME),
    RENTAL_INCOME(TransactionType.INCOME),
    GIFTS(TransactionType.INCOME),
    SALES_INCOME(TransactionType.INCOME),
    OTHER_INCOME(TransactionType.INCOME);


}

