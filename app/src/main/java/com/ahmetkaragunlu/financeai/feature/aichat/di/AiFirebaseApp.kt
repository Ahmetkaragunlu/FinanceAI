package com.ahmetkaragunlu.financeai.feature.aichat.di

import javax.inject.Qualifier

/**
 * Qualifies the secondary Firebase app used exclusively for AI inference.
 * The demo uses a separate Spark project for no-cost AI access within its quota.
 * Authentication, Firestore, Storage, FCM, Functions, and persisted chat
 * history remain on the default Firebase app.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AiFirebaseApp
