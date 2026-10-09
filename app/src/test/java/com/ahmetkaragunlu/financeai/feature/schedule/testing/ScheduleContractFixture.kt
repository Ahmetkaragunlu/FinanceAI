package com.ahmetkaragunlu.financeai.feature.schedule.testing

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.time.Instant

/** Test-only, independent expected values shared with JS; never packaged as a runtime resource. */
fun scheduleContractFixture(): JsonObject = checkNotNull(
    JsonObject::class.java.classLoader?.getResourceAsStream("contracts/schedule-contract.json")
).bufferedReader(Charsets.UTF_8).use { JsonParser.parseReader(it).asJsonObject }

fun JsonObject.fixtureTime(key: String): Long = Instant.parse(get(key).asString).toEpochMilli()
