package com.ahmetkaragunlu.financeai.core.security

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

object AppCheckInstaller {
    fun install(app: FirebaseApp = FirebaseApp.getInstance()) {
        FirebaseAppCheck.getInstance(app).installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
    }
}
