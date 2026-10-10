package com.ahmetkaragunlu.financeai.app

import com.ahmetkaragunlu.financeai.core.media.PhotoRecordType
import com.ahmetkaragunlu.financeai.core.media.work.PhotoWorkScheduler
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.feature.schedule.data.RoomScheduledCompletion
import com.ahmetkaragunlu.financeai.feature.schedule.data.reminder.ReminderScheduler
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder.ReminderPresenter
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions

class ScheduledCompletionCoordinatorTest {
    @Test
    fun workDispatchFollowsSuccessfulCompletionAndNeverRepeatsForMissingOrStaleAccountResults() =
        runTest {
            val session = AccountSession().apply { activate("A", "USD") }
            val local = mock(RoomScheduledCompletion::class.java)
            val reminders = mock(ReminderScheduler::class.java)
            val presenter = mock(ReminderPresenter::class.java)
            val photos = mock(PhotoWorkScheduler::class.java)
            val plan = ScheduledTransaction(
                id = 1,
                firestoreId = "plan",
                ownerId = "A",
                currencyCode = "USD",
                amount = 10.0,
                type = TransactionType.EXPENSE,
                category = CategoryType.FOOD,
                note = null,
                scheduledDate = 100
            )
            val financial = Transaction(
                firestoreId = "completed_plan",
                ownerId = "A",
                currencyCode = "USD",
                amount = 10.0,
                transaction = TransactionType.EXPENSE,
                category = CategoryType.FOOD,
                note = "",
                date = 200
            )
            val events = mutableListOf<String>()
            var result: Transaction? = financial
            var switchAccount = false
            doAnswer {
                events += "complete"
                if (switchAccount) session.activate("B", "EUR")
                result
            }.`when`(local).invoke(plan)
            doAnswer { events += "cancel-work"; null }.`when`(reminders).cancel("A", "plan", 1)
            doAnswer { events += "cancel-display"; null }.`when`(presenter).cancel("A", "plan")
            doAnswer { events += "upload-financial"; }.`when`(photos)
                .upload("A", PhotoRecordType.TRANSACTION, "completed_plan", null)
            val coordinator =
                ScheduledCompletionCoordinator(local, session, reminders, presenter, photos)
            assertSame(financial, coordinator(plan))
            assertEquals(
                listOf("complete", "cancel-work", "cancel-display", "upload-financial"),
                events
            )
            events.clear(); result = null
            assertNull(coordinator(plan))
            assertEquals(listOf("complete"), events)
            events.clear(); result = financial; switchAccount = true
            assertSame(financial, coordinator(plan))
            assertEquals(listOf("complete"), events)
            verify(local, times(3)).invoke(plan)
            verify(reminders).cancel("A", "plan", 1)
            verify(presenter).cancel("A", "plan")
            verify(photos).upload("A", PhotoRecordType.TRANSACTION, "completed_plan", null)
            verifyNoMoreInteractions(local, reminders, presenter, photos)
        }
}
