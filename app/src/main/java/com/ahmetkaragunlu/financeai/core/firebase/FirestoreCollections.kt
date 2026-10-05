package com.ahmetkaragunlu.financeai.core.firebase

/** Shared persisted collection names. Changing them is a data-contract migration, not a rename. */
object FirestoreCollections {
    const val USERS = "users"
    const val TRANSACTIONS = "transactions"
    const val SCHEDULED_TRANSACTIONS = "scheduled_transactions"
    const val BUDGETS = "budgets"
    const val AI_MESSAGES = "ai_messages"
    const val SCHEDULE_STATES = "schedule_states"
    const val SCHEDULE_COMMANDS = "schedule_commands"
}
