package com.ahmetkaragunlu.financeai.core.firebase.error

import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseFailureMapperTest {
    @Test
    fun firestoreCodesAreMappedByCodeRatherThanDiagnosticText() {
        val timeout =
            FirebaseFirestoreException(
                "same diagnostic",
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
            )
        val denied =
            FirebaseFirestoreException(
                "same diagnostic",
                FirebaseFirestoreException.Code.PERMISSION_DENIED,
            )
        val unavailable =
            FirebaseFirestoreException(
                "same diagnostic",
                FirebaseFirestoreException.Code.UNAVAILABLE,
            )
        assertTrue(timeout.toDataAccessFailure() is DataAccessException.TimedOut)
        assertTrue(denied.toDataAccessFailure() is DataAccessException.AccessDenied)
        assertTrue(unavailable.toDataAccessFailure() is DataAccessException.ServiceUnavailable)
        assertSame(timeout, timeout.toDataAccessFailure().cause)
    }

    @Test
    fun cancellationAndUnrecognisedExceptionsRemainUntouched() {
        val cancelled = CancellationException("cancelled")
        val unknown = IllegalStateException("diagnostic")
        assertSame(cancelled, cancelled.toDataAccessFailure())
        assertSame(unknown, unknown.toDataAccessFailure())
    }
}
