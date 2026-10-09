package com.ahmetkaragunlu.financeai.feature.schedule.domain.usecase

/** Restore only after registration of the supplied device token has been flushed. */
interface RestoreScheduleState {
    suspend operator fun invoke(ownerId: String, deviceToken: String)
}
