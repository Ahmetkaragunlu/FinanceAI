package com.ahmetkaragunlu.financeai.feature.schedule.data.mapper

import com.ahmetkaragunlu.financeai.feature.schedule.data.local.ScheduledTransactionEntity
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction

fun ScheduledTransactionEntity.toDomain(): ScheduledTransaction = ScheduledTransaction(
    id = id,
    firestoreId = firestoreId,
    amount = amount,
    type = type,
    category = category,
    note = note,
    scheduledDate = scheduledDate,
    expirationNotificationSent = expirationNotificationSent,
    notificationSent = notificationSent,
    photoUri = photoUri,
    locationFull = locationFull,
    locationShort = locationShort,
    latitude = latitude,
    longitude = longitude,
    syncedToFirebase = syncedToFirebase
)

fun ScheduledTransaction.toEntity(): ScheduledTransactionEntity = ScheduledTransactionEntity(
    id = id,
    firestoreId = firestoreId,
    amount = amount,
    type = type,
    category = category,
    note = note,
    scheduledDate = scheduledDate,
    expirationNotificationSent = expirationNotificationSent,
    notificationSent = notificationSent,
    photoUri = photoUri,
    locationFull = locationFull,
    locationShort = locationShort,
    latitude = latitude,
    longitude = longitude,
    syncedToFirebase = syncedToFirebase
)
