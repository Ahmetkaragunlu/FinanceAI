package com.ahmetkaragunlu.financeai.core.deeplink

/** Stable external notification/reset URI contract; independent of feature destinations. */
object FinanceLinkContract {
    const val SCHEME = "financeai"
    const val FINANCE_HOST = "main"
    const val RESET_HOST = "resetPassword"
    const val SCHEDULE_PATH = "/schedule"
    const val SCHEDULE_URI = "financeai://main/schedule"
    const val OWNER_QUERY = "owner"
    const val RECORD_QUERY = "record"
}
