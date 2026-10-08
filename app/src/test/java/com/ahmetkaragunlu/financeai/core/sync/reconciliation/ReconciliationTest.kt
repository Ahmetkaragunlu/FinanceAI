package com.ahmetkaragunlu.financeai.core.sync.reconciliation

import com.ahmetkaragunlu.financeai.core.sync.contract.SyncPayload
import org.junit.Assert.*
import org.junit.Test

class ReconciliationTest {
    private fun values(amount: Int, note: String = "old") = SyncPayload.encode(mapOf("amountMinor" to amount, "note" to note))
    @Test fun `only local edit is written`() {
        assertEquals(Reconciliation.Write(values(150)), reconcile(values(100), values(150), values(100)))
    }
    @Test fun `same result does not ask a question`() {
        assertEquals(Reconciliation.Equal, reconcile(values(100), values(150), values(150)))
    }
    @Test fun `different changes to same field require explicit resolution`() {
        assertEquals(Reconciliation.Conflict, reconcile(values(100), values(150), values(200)))
    }
    @Test fun `different fields can merge without data loss`() {
        assertEquals(Reconciliation.Write(values(150, "remote")), reconcile(values(100), values(150), values(100, "remote")))
    }
    @Test fun `remote delete against local edit does not resurrect silently`() {
        assertEquals(Reconciliation.Conflict, reconcile(values(100), values(150), null))
        assertEquals(Reconciliation.Conflict, reconcile(values(100), null, values(150)))
    }
    @Test fun `retry and unchanged delete remain idempotent`() {
        assertEquals(Reconciliation.Write(null), reconcile(values(100), null, values(100)))
        assertEquals(Reconciliation.Equal, reconcile(values(100), null, null))
    }
    @Test fun `large long monetary payload is not narrowed to double`() {
        val amount = 9_007_199_254_740_993L
        val encoded = SyncPayload.encode(mapOf("amountMinor" to amount))
        assertEquals(amount, SyncPayload.decode(encoded)["amountMinor"])
    }
}
