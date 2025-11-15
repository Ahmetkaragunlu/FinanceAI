
package com.ahmetkaragunlu.financeai.di.module

import android.content.Context
import com.ahmetkaragunlu.financeai.fcm.FCMTokenManager
import com.ahmetkaragunlu.financeai.firebaserepo.AuthRepository
import com.ahmetkaragunlu.financeai.firebaserepo.AuthRepositoryImpl
import com.ahmetkaragunlu.financeai.firebasesync.FirebaseSyncService
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
            .requestIdToken("1048183018405-9vic0j0akq3punlpeuc7926qq43erict.apps.googleusercontent.com")
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
