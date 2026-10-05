package com.ahmetkaragunlu.financeai.feature.schedule.domain.error

sealed class ScheduleException : Exception() {
    /** Retryable: a queued action is not acknowledged merely because its upload succeeded. */
    class AwaitingAcknowledgement : ScheduleException()
}
