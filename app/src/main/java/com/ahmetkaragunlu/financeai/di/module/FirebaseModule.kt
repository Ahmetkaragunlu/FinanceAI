package com.ahmetkaragunlu.financeai.di.module

import android.content.Context
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.fcm.FCMTokenManager
import com.ahmetkaragunlu.financeai.firebaserepo.AuthRepository
import com.ahmetkaragunlu.financeai.firebaserepo.AuthRepositoryImpl
import com.ahmetkaragunlu.financeai.firebasesync.FirebaseSyncService
import com.ahmetkaragunlu.financeai.photo.PhotoStorageManager
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object FirebaseModule {
    @Provides
    @Singleton
    fun provideFirebaseStore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()

    @Provides
    @Singleton
    fun provideFCMTokenManager(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        messaging: FirebaseMessaging
    ): FCMTokenManager {
        return FCMTokenManager(firestore, auth, messaging)
    }

    @Provides
    @Singleton
    fun providePhotoStorageManager(
        storage: FirebaseStorage,
        auth: FirebaseAuth
    ): PhotoStorageManager {
        return PhotoStorageManager(storage, auth)
    }

    @Provides
    @Singleton
    fun provideFirebaseSyncService(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        localRepository: FinanceRepository,
        photoStorageManager: PhotoStorageManager,
        @ApplicationContext context: Context
    ): FirebaseSyncService {
        return FirebaseSyncService(
            firestore,
            auth,
            localRepository,
            photoStorageManager,
            context
        )
    }
}

@InstallIn(SingletonComponent::class)
@Module
object RepositoryModule {
    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore,
        firebaseSyncService: FirebaseSyncService,
        fcmTokenManager: FCMTokenManager
    ): AuthRepository = AuthRepositoryImpl(
        auth,
        firestore,
        firebaseSyncService,
        fcmTokenManager
    )
}

@InstallIn(SingletonComponent::class)
@Module
object SignInWithGoogle {
    @Provides
    @Singleton
    fun provideGoogleSignInOptions(
        @ApplicationContext context: Context
    ): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }

    @Provides
    @Singleton
    fun provideGoogleSignInClient(
        @ApplicationContext context: Context,
        googleSignInOptions: GoogleSignInOptions
    ): GoogleSignInClient {
        return GoogleSignIn.getClient(context, googleSignInOptions)
    }
}