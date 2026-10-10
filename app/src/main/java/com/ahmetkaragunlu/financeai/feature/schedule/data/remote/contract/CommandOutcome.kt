package com.ahmetkaragunlu.financeai.feature.schedule.data.remote.contract

/** Unknown/failure receipts must stay pending failures, never implicitly become success. */
enum class CommandOutcome(val wireValue: String, val acknowledgesCommand: Boolean) {
    APPLIED("applied", true), ALREADY_APPLIED("already_applied", true),
    STALE("stale", true), EXPIRED("expired", true), EARLY("early", true), TERMINAL(
        "terminal",
        true
    ),
    CONFLICT("conflict", false);

    companion object {
        fun fromWire(value: String?): CommandOutcome? =
            entries.firstOrNull { it.wireValue == value }
    }
}
