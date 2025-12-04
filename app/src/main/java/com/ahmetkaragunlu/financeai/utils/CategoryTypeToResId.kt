package com.ahmetkaragunlu.financeai.utils


import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType

fun CategoryType.toResId(): Int {
    return when (this) {
        CategoryType.FOOD -> R.string.category_food
        CategoryType.GROCERIES -> R.string.category_groceries
        CategoryType.COFFEE_TEA -> R.string.category_coffee_tea
        CategoryType.DESSERT_SWEETS -> R.string.category_dessert_sweets
        CategoryType.TRANSPORT -> R.string.category_transport
        CategoryType.RENT -> R.string.category_rent
        CategoryType.ENTERTAINMENT -> R.string.category_entertainment
        CategoryType.HEALTH -> R.string.category_health
        CategoryType.BILLS -> R.string.category_bills
        CategoryType.CLOTHING -> R.string.category_clothing
        CategoryType.EDUCATION -> R.string.category_education
        CategoryType.HOME_DECORATION -> R.string.category_home_decoration
        CategoryType.GIFTS_DONATION -> R.string.category_gifts_donation
        CategoryType.OTHER -> R.string.category_other
        CategoryType.SALARY -> R.string.category_salary
        CategoryType.SCHOLARSHIP -> R.string.category_scholarship
        CategoryType.FREELANCE -> R.string.category_freelance
        CategoryType.INVESTMENT_INCOME -> R.string.category_investment_income
        CategoryType.RENTAL_INCOME -> R.string.category_rental_income
        CategoryType.GIFTS -> R.string.category_gifts
        CategoryType.SALES_INCOME -> R.string.category_sales_income
        CategoryType.OTHER_INCOME -> R.string.category_other_income
    }

}