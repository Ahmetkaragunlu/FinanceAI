package com.ahmetkaragunlu.financeai.feature.transaction.presentation.history

import androidx.annotation.StringRes
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.time.DateFilter

@StringRes
fun DateFilter.labelRes(): Int = when (this) {
    DateFilter.TODAY -> R.string.today
    DateFilter.YESTERDAY -> R.string.yesterday
    DateFilter.LAST_WEEK -> R.string.last_week
    DateFilter.LAST_MONTH -> R.string.last_month
    DateFilter.ALL -> R.string.date
}
