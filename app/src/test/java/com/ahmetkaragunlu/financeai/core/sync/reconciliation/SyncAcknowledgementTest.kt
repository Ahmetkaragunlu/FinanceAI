package com.ahmetkaragunlu.financeai.core.sync.reconciliation

import com.ahmetkaragunlu.financeai.core.sync.local.entity.SyncRecord
import org.junit.Assert.*
import org.junit.Test

class SyncAcknowledgementTest {
    @Test fun `server deletion acknowledges only the exact previous local mutation`() {
        assertTrue(acknowledgesDeletion(true, "created", "created"))
        assertFalse(acknowledgesDeletion(true, "created", "new-edit"))
        assertFalse(acknowledgesDeletion(false, "created", "created"))
        assertFalse(acknowledgesDeletion(true, null, null))
    }
    private val current = SyncRecord("A", "transactions", "receipt", basePayload = "old",
        pendingPayload = "latest", mutationId = "new")

    @Test fun `old success rebases without erasing newer local edit`() {
        val result = acknowledgeSync(current, "previous", "sent", 4, false)
        assertFalse(result.applyRemote)
        assertEquals("latest", result.record.pendingPayload)
        assertEquals("new", result.record.mutationId)
        assertEquals("sent", result.record.basePayload)
    }

    @Test fun `old conflict cannot overwrite a newer resolution`() {
        assertEquals(SyncAcknowledgement(current, false), acknowledgeSync(current, "previous", "remote", 4, true))
    }

    @Test fun `acknowledged tombstone clears only its own pending mutation`() {
        val result = acknowledgeSync(current.copy(pendingPayload = null, pendingDelete = true), "new", null, 4, false)
        assertTrue(result.applyRemote)
        assertNull(result.record.mutationId)
        assertNull(result.record.basePayload)
        assertEquals(4, result.record.baseRevision)
    }

    @Test fun `real conflict preserves both versions and pending identity`() {
        val result = acknowledgeSync(current, "new", "remote", 4, true)
        assertFalse(result.applyRemote)
        assertEquals("latest", result.record.pendingPayload)
        assertEquals("remote", result.record.conflictPayload)
        assertEquals("new", result.record.mutationId)
    }
}
