package com.ahmetkaragunlu.financeai.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoRecordTypeTest {
    @Test fun persistedValuesStillResolveToTheirOriginalRecordTypes() {
        assertEquals(PhotoRecordType.TRANSACTION, PhotoRecordType.fromWire("transactions"))
        assertEquals(PhotoRecordType.SCHEDULED, PhotoRecordType.fromWire("scheduled"))
        assertEquals("transactions", PhotoRecordType.TRANSACTION.wireValue)
        assertEquals("scheduled", PhotoRecordType.SCHEDULED.wireValue)
    }

    @Test fun enumNamesAndRemoteScheduleCollectionAreNotAcceptedAsPhotoWireValues() {
        listOf("", "TRANSACTION", "SCHEDULED", "scheduled_transactions", "transactions/other", " scheduled").forEach {
            assertNull(it, PhotoRecordType.fromWire(it))
        }
    }
}
