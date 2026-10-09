package com.ahmetkaragunlu.financeai.app.presentation

import android.content.Context
import android.os.Bundle
import android.os.Parcel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.room.Room
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.ahmetkaragunlu.financeai.app.navigation.deeplink.FinanceDeepLink
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.ahmetkaragunlu.financeai.core.time.FinanceCalendar
import com.ahmetkaragunlu.financeai.feature.schedule.data.reminder.ReminderCoordinator
import com.ahmetkaragunlu.financeai.feature.schedule.data.reminder.ReminderScheduler
import com.ahmetkaragunlu.financeai.feature.schedule.data.sync.ScheduleCommandQueue
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder.ReminderKind
import com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder.ReminderPresenter
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MainViewModelTest {
    private lateinit var database: FinanceDatabase
    private lateinit var reminders: ReminderCoordinator
    private lateinit var calendar: FinanceCalendar
    private val stores = mutableListOf<ViewModelStore>()
    private var now = Instant.parse("2026-10-05T12:00:00Z")
    private val clock =
        object : Clock() {
            override fun getZone(): ZoneId = ZoneId.systemDefault()

            override fun withZone(zone: ZoneId): Clock = Clock.fixed(now, zone)

            override fun instant(): Instant = now
        }

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().build(),
        )
        database = Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java).build()
        val workManager = WorkManager.getInstance(context)
        val presenter =
            object : ReminderPresenter {
                override fun show(plan: ScheduledTransaction, kind: ReminderKind, eventId: String) =
                    false

                override fun cancel(ownerId: String, remoteId: String) = Unit
            }
        reminders =
            ReminderCoordinator(
                database,
                AccountSession(),
                ScheduleCommandQueue(database, clock),
                SyncScheduler(workManager),
                ReminderScheduler(workManager, clock),
                presenter,
                clock,
            )
        calendar = FinanceCalendar(clock)
    }

    @After
    fun close() {
        runBlocking(Dispatchers.Main) { stores.forEach(ViewModelStore::clear) }
        database.close()
        WorkManagerTestInitHelper.closeWorkDatabase()
    }

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()): MainViewModel =
        MainViewModel(handle, calendar, reminders).also { vm ->
            stores += ViewModelStore().apply { put("main", vm) }
        }

    private fun viewModel(owner: StateOwner): MainViewModel {
        stores += owner.viewModelStore
        val factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras,
                ): T = MainViewModel(extras.createSavedStateHandle(), calendar, reminders) as T
            }
        return ViewModelProvider(owner.viewModelStore, factory, owner.extras)[
            MainViewModel::class.java]
    }

    @Test
    fun pendingDeliverySurvivesActualSavedStateRegistryAndParcelRoundTrip() =
        runBlocking(Dispatchers.Main) {
            val owner = StateOwner()
            val vm = viewModel(owner)
            vm.acceptInitialLink(null)
            vm.acceptNewLink("financeai://resetPassword?oobCode=a%2Bb")
            yield()
            val pending = checkNotNull(vm.pendingDeepLink.value)
            val parcel = Parcel.obtain()
            val saved =
                try {
                    parcel.writeBundle(owner.save())
                    parcel.setDataPosition(0)
                    checkNotNull(parcel.readBundle(javaClass.classLoader))
                } finally {
                    parcel.recycle()
                }
            val restored = viewModel(StateOwner(saved))
            restored.acceptInitialLink("financeai://main/schedule")
            yield()
            assertEquals(pending, restored.pendingDeepLink.value)
            assertTrue(restored.consumeDeepLink(pending.id))
        }

    @Test
    fun coldNotificationWaitsUntilExplicitConsumption() =
        runBlocking(Dispatchers.Main) {
            val vm = viewModel()
            vm.acceptInitialLink("financeai://main/schedule?owner=account-a")
            yield()
            val pending = checkNotNull(vm.pendingDeepLink.value)
            assertEquals(FinanceDeepLink.Schedule("account-a"), pending.destination)
            vm.acceptInitialLink("financeai://main/schedule?owner=account-b")
            yield()
            assertEquals(pending, vm.pendingDeepLink.value)
            assertTrue(vm.consumeDeepLink(pending.id))
            yield()
            assertNull(vm.pendingDeepLink.value)
            assertFalse(vm.consumeDeepLink(pending.id))
        }

    @Test
    fun repeatedUriHasNewIdentityAndOldCallbackCannotConsumeIt() =
        runBlocking(Dispatchers.Main) {
            val vm = viewModel()
            val uri = "financeai://main/schedule?owner=account-a"
            vm.acceptInitialLink(uri)
            yield()
            val first = checkNotNull(vm.pendingDeepLink.value)
            vm.acceptNewLink(uri)
            yield()
            val second = checkNotNull(vm.pendingDeepLink.value)
            assertNotEquals(first.id, second.id)
            assertEquals(first.destination, second.destination)
            assertFalse(vm.consumeDeepLink(first.id))
            assertEquals(second, vm.pendingDeepLink.value)
            assertTrue(vm.consumeDeepLink(second.id))
        }

    @Test
    fun invalidNewIntentDoesNotReplaceAValidPendingLink() =
        runBlocking(Dispatchers.Main) {
            val vm = viewModel()
            vm.acceptInitialLink("financeai://resetPassword?oobCode=a%2Bb")
            yield()
            val pending = checkNotNull(vm.pendingDeepLink.value)
            vm.acceptNewLink("https://main/schedule")
            vm.acceptNewLink(null)
            yield()
            assertEquals(pending, vm.pendingDeepLink.value)
            assertEquals(FinanceDeepLink.PasswordReset("a+b"), pending.destination)
        }

    @Test
    fun restoredPendingRecordKeepsItsIdentityInsteadOfReplayingTheInitialIntent() =
        runBlocking(Dispatchers.Main) {
            val handle = SavedStateHandle()
            val vm = viewModel(handle)
            vm.acceptInitialLink(null)
            vm.acceptNewLink("financeai://main/schedule?owner=account-b")
            yield()
            val pending = checkNotNull(vm.pendingDeepLink.value)
            val restored = viewModel(snapshot(handle))
            restored.acceptInitialLink("financeai://main/schedule?owner=account-a")
            yield()
            assertEquals(pending, restored.pendingDeepLink.value)
        }

    @Test
    fun consumedRecordStaysConsumedAfterRestoration() =
        runBlocking(Dispatchers.Main) {
            val handle = SavedStateHandle()
            val vm = viewModel(handle)
            val uri = "financeai://main/schedule"
            vm.acceptInitialLink(uri)
            yield()
            vm.consumeDeepLink(checkNotNull(vm.pendingDeepLink.value).id)
            val restored = viewModel(snapshot(handle))
            restored.acceptInitialLink(uri)
            yield()
            assertNull(restored.pendingDeepLink.value)
            restored.acceptNewLink(uri)
            yield()
            assertNotNull(restored.pendingDeepLink.value)
        }

    @Test
    fun legacyConsumptionFlagPreventsReplayButDoesNotBlockNewDelivery() =
        runBlocking(Dispatchers.Main) {
            val vm = viewModel()
            val uri = "financeai://main/schedule"
            vm.acceptInitialLink(uri, previouslyConsumed = true)
            yield()
            assertNull(vm.pendingDeepLink.value)
            vm.acceptNewLink(uri)
            yield()
            assertNotNull(vm.pendingDeepLink.value)
        }

    @Test
    fun resumeRefreshesTheMonthWithoutRequiringAnAccountOrNetwork() = runBlocking {
        withTimeout(5_000) {
            val periods = mutableListOf<Long>()
            val observation = launch { calendar.observeMonth().take(2).collect { periods += it.start } }
            while (periods.isEmpty()) yield()
            now = Instant.parse("2026-11-05T12:00:00Z")
            withContext(Dispatchers.Main) { viewModel().onForeground() }
            observation.join()
            assertEquals(2, periods.size)
            assertTrue(periods[1] > periods[0])
        }
    }

    // Simulate values supplied by SavedStateHandle restoration, without coupling to private keys.
    private fun snapshot(handle: SavedStateHandle): SavedStateHandle =
        SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) })

    private class StateOwner(saved: Bundle? = null) : SavedStateRegistryOwner, ViewModelStoreOwner {
        override val lifecycle = LifecycleRegistry(this)
        override val viewModelStore = ViewModelStore()
        private val controller = SavedStateRegistryController.create(this)
        override val savedStateRegistry
            get() = controller.savedStateRegistry

        val extras =
            MutableCreationExtras().apply {
                set(SAVED_STATE_REGISTRY_OWNER_KEY, this@StateOwner)
                set(VIEW_MODEL_STORE_OWNER_KEY, this@StateOwner)
            }

        init {
            controller.performAttach()
            controller.performRestore(saved)
            enableSavedStateHandles()
            lifecycle.currentState = Lifecycle.State.CREATED
        }

        fun save(): Bundle = Bundle().also(controller::performSave)
    }
}
