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


    fun getDisplayNameRes(): Int {
        return when (this) {
            FOOD -> R.string.category_food
            GROCERIES -> R.string.category_groceries
            COFFEE_TEA -> R.string.category_coffee_tea
            DESSERT_SWEETS -> R.string.category_dessert_sweets
            TRANSPORT -> R.string.category_transport
            RENT -> R.string.category_rent
            ENTERTAINMENT -> R.string.category_entertainment
            HEALTH -> R.string.category_health
            BILLS -> R.string.category_bills
            CLOTHING -> R.string.category_clothing
            EDUCATION -> R.string.category_education
            HOME_DECORATION -> R.string.category_home_decoration
            GIFTS_DONATION -> R.string.category_gifts_donation
            OTHER -> R.string.category_other

            SALARY -> R.string.category_salary
            SCHOLARSHIP -> R.string.category_scholarship
            FREELANCE -> R.string.category_freelance
            INVESTMENT_INCOME -> R.string.category_investment_income
            RENTAL_INCOME -> R.string.category_rental_income
            GIFTS -> R.string.category_gifts
            SALES_INCOME -> R.string.category_sales_income
            OTHER_INCOME -> R.string.category_other_income
        }
    }
}

