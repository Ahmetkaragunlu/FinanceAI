package com.ahmetkaragunlu.financeai.utils


import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType

fun CategoryType.toIconResId(): Int {
    return when (this) {
        CategoryType.FOOD -> R.drawable.food
        CategoryType.GROCERIES -> R.drawable.grocery
        CategoryType.COFFEE_TEA -> R.drawable.coffee
        CategoryType.DESSERT_SWEETS -> R.drawable.dessert
        CategoryType.TRANSPORT -> R.drawable.transport
        CategoryType.RENT -> R.drawable.rent
        CategoryType.ENTERTAINMENT -> R.drawable.entertainment
        CategoryType.HEALTH -> R.drawable.health
        CategoryType.BILLS -> R.drawable.bills
        CategoryType.CLOTHING -> R.drawable.clothing
        CategoryType.EDUCATION -> R.drawable.education
        CategoryType.HOME_DECORATION -> R.drawable.home_decoration
        CategoryType.GIFTS_DONATION -> R.drawable.gifts
        CategoryType.OTHER -> R.drawable.other
        CategoryType.SALARY -> R.drawable.salary
        CategoryType.SCHOLARSHIP -> R.drawable.scholarship
        CategoryType.FREELANCE -> R.drawable.freelancer
        CategoryType.INVESTMENT_INCOME -> R.drawable.investment
        CategoryType.RENTAL_INCOME -> R.drawable.rent
        CategoryType.GIFTS -> R.drawable.gifts
        CategoryType.SALES_INCOME -> R.drawable.sales
        CategoryType.OTHER_INCOME -> R.drawable.other
    }
}