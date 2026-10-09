package com.ahmetkaragunlu.financeai.feature.schedule.domain.model

private const val COMPLETED_PREFIX = "completed_"

fun completedTransactionId(planId: String): String = COMPLETED_PREFIX + planId

/** Null denotes a normal transaction; suffixes keep their existing persisted meaning. */
fun planIdOf(completedId: String): String? =
    if (completedId.startsWith(COMPLETED_PREFIX)) completedId.removePrefix(COMPLETED_PREFIX) else null
