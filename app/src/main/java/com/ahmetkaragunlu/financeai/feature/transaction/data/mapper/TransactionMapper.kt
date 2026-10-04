package com.ahmetkaragunlu.financeai.feature.transaction.data.mapper

import com.ahmetkaragunlu.financeai.feature.transaction.data.local.TransactionEntity
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    firestoreId = firestoreId,
    amount = amount,
    transaction = transaction,
    note = note,
    date = date,
    category = category,
    photoUri = photoUri,
    locationFull = locationFull,
    locationShort = locationShort,
    latitude = latitude,
    longitude = longitude,
    syncedToFirebase = syncedToFirebase
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    firestoreId = firestoreId,
    amount = amount,
    transaction = transaction,
    note = note,
    date = date,
    category = category,
    photoUri = photoUri,
    locationFull = locationFull,
    locationShort = locationShort,
    latitude = latitude,
    longitude = longitude,
    syncedToFirebase = syncedToFirebase
)
