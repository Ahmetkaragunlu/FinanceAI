package com.ahmetkaragunlu.financeai.feature.schedule.data.sync

import com.ahmetkaragunlu.financeai.core.media.PhotoFields
import com.ahmetkaragunlu.financeai.core.sync.contract.SyncPayload
import com.ahmetkaragunlu.financeai.core.sync.reconciliation.Reconciliation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CompletedPlanEditsTest {
    private fun plan(amount: Long = 5000, note: String = "before", date: Long = 100) =
        SyncPayload.encode(mapOf("amountMinor" to amount, "type" to "EXPENSE", "note" to note, "scheduledDate" to date))
    private fun financial(amount: Long = 5000, note: String = "before") =
        SyncPayload.encode(mapOf("amountMinor" to amount, "transaction" to "EXPENSE", "note" to note, "date" to 9999L))
    @Test fun localPlanEditsTargetTheSameFinancialRecordAndPreserveCompletionDate() {
        val result = projectCompletedPlanEdit(plan(), plan(7500, "local", 999999), financial())
        val values = SyncPayload.decode((result.decision as Reconciliation.Write).payload!!)
        assertEquals(7500L, values["amountMinor"])
        assertEquals("local", values["note"])
        assertEquals(9999L, values["date"])
        assertFalse(values.containsKey("scheduledDate"))
    }
    @Test fun newOverlappingRemoteFinancialChangeRequiresAnotherExplicitChoice() {
        val result = projectCompletedPlanEdit(plan(), plan(7500), financial(9000))
        assertEquals(Reconciliation.Conflict, result.decision)
        assertEquals(7500L, SyncPayload.decode(result.wanted)["amountMinor"])
    }
    @Test fun unrelatedRemoteFinancialChangesAreNotOverwrittenByThePlan() {
        val result = projectCompletedPlanEdit(plan(), plan(7500), financial(note = "remote"))
        val values = SyncPayload.decode((result.decision as Reconciliation.Write).payload!!)
        assertEquals("remote", values["note"])
        assertEquals(7500L, values["amountMinor"])
    }
    @Test fun existingIndependentLocalFinancialEditAlsoSurvives() {
        val result = projectCompletedPlanEdit(plan(), plan(7500), financial(), financial(), financial(note = "own edit"))
        val values = SyncPayload.decode((result.decision as Reconciliation.Write).payload!!)
        assertEquals("own edit", values["note"])
        assertEquals(7500L, values["amountMinor"])
    }

    @Test fun onlyPersistedPhotoMetadataTransfersAndCompletionDateRemainsCanonical() {
        val before = SyncPayload.decode(plan()) + mapOf("photoStorageUrl" to "https://old",
            "photoRemoved" to false, "photoVersion" to "old", "photoIntent" to "old")
        val changes = mapOf("photoStorageUrl" to "https://new", "photoRemoved" to true,
            "photoVersion" to "new", "photoIntent" to "new")
        val wanted = before + changes + mapOf("localPhotoUri" to "/private.jpg", "scheduledDate" to 5000L,
            "completedFrom" to "injected")
        val canonical = SyncPayload.decode(financial()) + before.filterKeys { it in changes }
        val result = projectCompletedPlanEdit(SyncPayload.encode(before), SyncPayload.encode(wanted), SyncPayload.encode(canonical))
        val projected = SyncPayload.decode(result.wanted)
        changes.forEach { (key, value) -> assertEquals(value, projected[key]) }
        assertEquals(9999L, projected["date"])
        assertFalse(projected.containsKey("localPhotoUri"))
        assertFalse(projected.containsKey("completedFrom"))
        assertFalse(projected.containsKey("scheduledDate"))
        assertEquals(listOf("photoStorageUrl", "photoRemoved", "photoVersion", "photoIntent"), PhotoFields.PERSISTED_METADATA.toList())
    }
}
