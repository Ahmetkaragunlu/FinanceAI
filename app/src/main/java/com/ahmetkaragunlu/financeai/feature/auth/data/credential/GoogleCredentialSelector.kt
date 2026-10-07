package com.ahmetkaragunlu.financeai.feature.auth.data.credential

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.ahmetkaragunlu.financeai.feature.auth.domain.error.AuthException
import com.ahmetkaragunlu.financeai.feature.auth.domain.model.GoogleIdentity
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/** Explicit button flow: the current Activity is supplied only for the provider UI call. */
class GoogleCredentialSelector(private val manager: CredentialManager) {
    suspend fun select(activityContext: Context, webClientId: String): GoogleIdentity {
        val option = GetSignInWithGoogleOption.Builder(webClientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = manager.getCredential(activityContext, request).credential
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw AuthException.InvalidCredentials()
        }
        val token = GoogleIdTokenCredential.createFrom(credential.data)
        return GoogleIdentity(token.id, token.idToken)
    }
}
