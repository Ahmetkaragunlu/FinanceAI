package com.ahmetkaragunlu.financeai

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/** Room/worker tests must not start the real authenticated Firebase sync coordinator. */
class FinanceTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application =
        super.newApplication(cl, Application::class.java.name, context)
}
