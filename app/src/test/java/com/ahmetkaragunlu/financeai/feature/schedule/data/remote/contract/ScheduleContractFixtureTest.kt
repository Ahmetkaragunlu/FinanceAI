package com.ahmetkaragunlu.financeai.feature.schedule.data.remote.contract

import com.ahmetkaragunlu.financeai.core.firebase.FirestoreCollections
import com.ahmetkaragunlu.financeai.core.media.PhotoFields
import com.ahmetkaragunlu.financeai.core.sync.contract.FinancialFields
import com.ahmetkaragunlu.financeai.core.sync.contract.SyncFields
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.completedTransactionId
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.planIdOf
import com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder.ScheduleCommandType
import com.ahmetkaragunlu.financeai.feature.schedule.testing.scheduleContractFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScheduleContractFixtureTest {
    @Test
    fun wireNamesAndReceiptAcknowledgementMatchTheIndependentJsFixture() {
        val fixture = scheduleContractFixture()
        val collections = fixture.getAsJsonObject("collections").entrySet()
            .associate { it.key to it.value.asString }
        assertEquals(
            collections, mapOf(
                "plans" to FirestoreCollections.SCHEDULED_TRANSACTIONS,
                "states" to FirestoreCollections.SCHEDULE_STATES,
                "commands" to FirestoreCollections.SCHEDULE_COMMANDS,
                "financial" to FirestoreCollections.TRANSACTIONS
            )
        )
        val fields =
            fixture.getAsJsonObject("fields").entrySet().associate { it.key to it.value.asString }
        assertEquals(
            fields, mapOf(
                "amountMinor" to FinancialFields.AMOUNT_MINOR,
                "currencyCode" to FinancialFields.CURRENCY_CODE,
                "scheduledDate" to ScheduleFields.DATE,
                "type" to ScheduleCommandFields.TYPE,
                "userId" to SyncFields.USER_ID,
                "remoteId" to ScheduleCommandFields.TRANSACTION_ID,
                "requestedAt" to ScheduleCommandFields.REQUESTED_AT,
                "outcome" to ScheduleCommandFields.OUTCOME,
                "status" to ScheduleFields.STATUS,
                "snoozeAt" to ScheduleFields.SNOOZE_AT,
                "deleteAt" to ScheduleFields.DELETE_AT
            )
        )
        assertEquals(
            fixture.getAsJsonArray("photoMetadata").map { it.asString },
            PhotoFields.PERSISTED_METADATA.toList()
        )
        assertEquals(
            fixture.getAsJsonArray("commandTypes").map { it.asString }.sorted(),
            ScheduleCommandType.entries.map { it.wireValue }.sorted()
        )
        assertEquals(
            fixture.getAsJsonArray("statuses").map { it.asString }.sorted(),
            ScheduleStatus.entries.map { it.wireValue }.sorted()
        )
        val outcomes = fixture.getAsJsonArray("outcomes").map { it.asJsonObject }
        assertEquals(outcomes.filter { it["androidKnown"].asBoolean }.map { it["wire"].asString }
            .sorted(), CommandOutcome.entries.map { it.wireValue }.sorted())
        for (expected in outcomes) {
            val outcome = CommandOutcome.fromWire(expected["wire"].asString)
            if (!expected["androidKnown"].asBoolean) assertNull(outcome)
            assertEquals(expected["acknowledged"].asBoolean, outcome?.acknowledgesCommand ?: false)
        }
        val identity = fixture.getAsJsonObject("identity")
        assertEquals(
            identity["completed"].asString,
            completedTransactionId(identity["plan"].asString)
        )
        assertEquals(identity["plan"].asString, planIdOf(identity["completed"].asString))
    }
}
