package com.ahmetkaragunlu.financeai.feature.transaction.data.mapper

import com.ahmetkaragunlu.financeai.core.money.MoneyAmounts
import com.ahmetkaragunlu.financeai.feature.transaction.data.local.entity.TransactionEntity
import com.ahmetkaragunlu.financeai.feature.transaction.data.local.model.CategoryExpenseRow
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryExpense
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction

fun CategoryExpenseRow.toDomain(currencyCode: String): CategoryExpense = CategoryExpense(
    category = CategoryType.entries.firstOrNull { it.name == category },
    totalAmount = MoneyAmounts.toMajor(totalMinor, currencyCode),
)

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    firestoreId = firestoreId,
    amount = MoneyAmounts.toMajor(amountMinor, currencyCode),
    transaction = transaction,
    note = note,
    date = date,
    category = category,
    photoUri = photoUri,
    locationFull = locationFull,
    locationShort = locationShort,
    latitude = latitude,
    longitude = longitude,
    ownerId = ownerId,
    currencyCode = currencyCode,
    syncedToFirebase = syncedToFirebase
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    firestoreId = firestoreId,
    amountMinor = MoneyAmounts.toMinor(amount, currencyCode),
    transaction = transaction,
    note = note,
    date = date,
    category = category,
    photoUri = photoUri,
    locationFull = locationFull,
    locationShort = locationShort,
    latitude = latitude,
    longitude = longitude,
    ownerId = ownerId,
    currencyCode = currencyCode,
    syncedToFirebase = syncedToFirebase
)
