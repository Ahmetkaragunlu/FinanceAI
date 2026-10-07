package com.ahmetkaragunlu.financeai.feature.auth.data.local.session

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialSessionManager @Inject constructor(
    @ApplicationContext context: Context
) : CredentialSessionCleaner {
    private val credentialManager = CredentialManager.create(context)

    // The repository retains the existing timeout, cancellation and best-effort error policy.
    override suspend fun clear() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
}
