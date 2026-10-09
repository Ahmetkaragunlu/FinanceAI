package com.ahmetkaragunlu.financeai.core.media

/** Stable values shared by photo_operations, WorkManager input and Storage paths. */
enum class PhotoRecordType(val wireValue: String) {
    TRANSACTION("transactions"),
    SCHEDULED("scheduled");

    companion object {
        fun fromWire(value: String): PhotoRecordType? = entries.firstOrNull { it.wireValue == value }
    }
}
