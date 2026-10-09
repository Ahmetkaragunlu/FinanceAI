package com.ahmetkaragunlu.financeai.feature.schedule.data.remote

import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.ahmetkaragunlu.financeai.feature.schedule.domain.usecase.RestoreScheduleState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

class FirebaseScheduleStateRestorer @Inject constructor(
    private val functions: FirebaseFunctions,
    private val auth: FirebaseAuth,
) : RestoreScheduleState {
    override suspend fun invoke(ownerId: String, deviceToken: String) {
        var cursor: String? = null
        do {
            if (auth.currentUser?.uid != ownerId) return
            val result = functions.getHttpsCallable("restoreScheduleState")
                .call(mapOf("deviceToken" to deviceToken, "cursor" to cursor)).await().getData() as? Map<*, *>
                ?: throw DataAccessException.InvalidRemoteData()
            val next = result["nextCursor"] as? String
            if (next != null && next == cursor) throw DataAccessException.InvalidRemoteData()
            cursor = next
        } while (cursor != null)
    }
}
