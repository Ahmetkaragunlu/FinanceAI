package com.ahmetkaragunlu.financeai.core.security

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

object AppCheckInstaller {
    fun install(app: FirebaseApp = FirebaseApp.getInstance()) {
        FirebaseAppCheck.getInstance(app).installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
    }
}
