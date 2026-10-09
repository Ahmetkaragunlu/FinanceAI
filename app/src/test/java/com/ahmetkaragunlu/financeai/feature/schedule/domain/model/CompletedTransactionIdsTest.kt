package com.ahmetkaragunlu.financeai.feature.schedule.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CompletedTransactionIdsTest {
    @Test fun completedIdsPreserveExistingRecordsAndRoundTripTheirPlanSuffix() {
        listOf("p1", "photo-plan", "plan_with_underscores", "completed_plan", "").forEach { plan ->
            assertEquals("completed_$plan", completedTransactionId(plan))
            assertEquals(plan, planIdOf(completedTransactionId(plan)))
        }
    }

    @Test fun normalTransactionIdsAreNotMistakenForCompletedPlans() {
        listOf("p1", "", "completed", "prefix_completed_p1", "Completed_p1").forEach {
            assertNull(it, planIdOf(it))
        }
        assertEquals("completed_p1", planIdOf("completed_completed_p1"))
    }
}
