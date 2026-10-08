package com.ahmetkaragunlu.financeai.core.sync.contract

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

object SyncPayload {
    private val gson = GsonBuilder().serializeNulls().create()
    fun encode(map: Map<String, Any?>): String = gson.toJson(map.toSortedMap())
    fun decode(json: String): Map<String, Any?> = JsonParser.parseString(json).asJsonObject
        .entrySet().associate { (key, value) ->
            key to when {
                value.isJsonNull -> null
                value.asJsonPrimitive.isBoolean -> value.asBoolean
                value.asJsonPrimitive.isNumber -> value.asBigDecimal.let {
                    if (it.stripTrailingZeros().scale() <= 0) it.longValueExact() else it.toDouble()
                }
                else -> value.asString
            }
        }
    fun equivalent(first: String?, second: String?): Boolean =
        first == second || (first != null && second != null && decode(first) == decode(second))
}
