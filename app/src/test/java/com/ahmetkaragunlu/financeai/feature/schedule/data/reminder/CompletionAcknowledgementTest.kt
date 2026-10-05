package com.ahmetkaragunlu.financeai.feature.schedule.data.reminder

import com.ahmetkaragunlu.financeai.core.sync.SyncPayload
import com.ahmetkaragunlu.financeai.core.sync.local.entity.SyncRecord
import org.junit.Assert.*
import org.junit.Test

class CompletionAcknowledgementTest {
    private fun payload(amount: Long = 100, date: Long = 20, note: String = "before") =
        SyncPayload.encode(mapOf("amountMinor" to amount, "date" to date, "note" to note))
    private fun record(value: String?) = SyncRecord("A", "transactions", "completed_p1", pendingPayload = value,
        pendingDelete = value == null, mutationId = "latest")
    @Test fun unchangedOptimisticCompletionAdoptsTheCanonicalFirstWinner() {
        val result = acknowledgeCompletion(record(payload()), payload(), payload(date = 10), 7)
        assertTrue(result.applyRemote)
        assertNull(result.record.mutationId)
        assertEquals(payload(date = 10), result.record.basePayload)
    }
    @Test fun laterNoteEditSurvivesAndIsRebasedOntoTheCanonicalDate() {
        val result = acknowledgeCompletion(record(payload(note = "after")), payload(), payload(date = 10), 7)
        assertFalse(result.applyRemote)
        assertEquals("latest", result.record.mutationId)
        assertEquals(payload(date = 10, note = "after"), result.record.pendingPayload)
    }
    @Test fun unequalOverlappingEditsKeepBothVersionsForTheExistingChooser() {
        val local = payload(amount = 200)
        val remote = payload(amount = 300)
        val result = acknowledgeCompletion(record(local), payload(), remote, 7)
        assertFalse(result.applyRemote)
        assertEquals(local, result.record.pendingPayload)
        assertEquals(remote, result.record.conflictPayload)
        assertEquals(7L, result.record.conflictRevision)
    }
    @Test fun explicitLaterDeletionRemainsPendingInsteadOfRecreatingTheRow() {
        val result = acknowledgeCompletion(record(null), payload(), payload(), 7)
        assertFalse(result.applyRemote)
        assertTrue(result.record.pendingDelete)
        assertEquals("latest", result.record.mutationId)
    }
}
