package com.ahmetkaragunlu.financeai.app

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.ahmetkaragunlu.financeai.core.work.di.WorkModule
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito.mock

/** Checks the production configuration without running authenticated application startup. */
class FinanceApplicationConfigurationTest {
    @Test
    fun configurationKeepsTheInjectedHiltFactoryAndTheSharedManagerProvider() {
        val factory = mock(HiltWorkerFactory::class.java)
        val application = FinanceApplication().apply { workerFactory = factory }
        val configuration = application.workManagerConfiguration
        assertSame(factory, configuration.workerFactory)
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration)
        try {
            assertSame(WorkManager.getInstance(context), WorkModule.provideWorkManager(context))
        } finally {
            WorkManagerTestInitHelper.closeWorkDatabase()
        }
    }
}
