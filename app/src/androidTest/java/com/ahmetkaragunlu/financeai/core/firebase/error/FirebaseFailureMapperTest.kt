package com.ahmetkaragunlu.financeai.core.firebase.error

import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.storage.StorageException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class FirebaseFailureMapperTest {
    @Test
    fun recognisedFirestoreAndFunctionsCodesKeepTheSameFailureFamiliesAndUnknownIdentity() {
        val firestoreCases = mapOf(
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED to DataAccessException.TimedOut::class.java,
            FirebaseFirestoreException.Code.UNAVAILABLE to DataAccessException.ServiceUnavailable::class.java,
            FirebaseFirestoreException.Code.PERMISSION_DENIED to DataAccessException.AccessDenied::class.java,
            FirebaseFirestoreException.Code.UNAUTHENTICATED to DataAccessException.AccessDenied::class.java,
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED to DataAccessException.RateLimited::class.java,
        )
        firestoreCases.forEach { (code, expected) ->
            val original = FirebaseFirestoreException("same diagnostic", code)
            assertTrue(expected.isInstance(original.toDataAccessFailure()))
            assertSame(original, original.toDataAccessFailure().cause)
        }
        val functionsCases = mapOf(
            FirebaseFunctionsException.Code.DEADLINE_EXCEEDED to DataAccessException.TimedOut::class.java,
            FirebaseFunctionsException.Code.UNAVAILABLE to DataAccessException.ServiceUnavailable::class.java,
            FirebaseFunctionsException.Code.PERMISSION_DENIED to DataAccessException.AccessDenied::class.java,
            FirebaseFunctionsException.Code.UNAUTHENTICATED to DataAccessException.AccessDenied::class.java,
            FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED to DataAccessException.RateLimited::class.java,
        )
        functionsCases.forEach { (code, expected) ->
            val original = mock(FirebaseFunctionsException::class.java)
            `when`(original.code).thenReturn(code)
            assertTrue(expected.isInstance(original.toDataAccessFailure()))
            assertSame(original, original.toDataAccessFailure().cause)
        }
        val unknown = mock(FirebaseFunctionsException::class.java)
        `when`(unknown.code).thenReturn(FirebaseFunctionsException.Code.INTERNAL)
        assertSame(unknown, unknown.toDataAccessFailure())
        val unrecognised = FirebaseFirestoreException("PERMISSION_DENIED", FirebaseFirestoreException.Code.FAILED_PRECONDITION)
        assertSame(unrecognised, unrecognised.toDataAccessFailure())
    }

    @Test
    fun storageCodesAndNetworkTimeoutsPreserveCauseRatherThanMatchingMessage() {
        mapOf(StorageException.ERROR_NOT_AUTHENTICATED to DataAccessException.AccessDenied::class.java,
            StorageException.ERROR_NOT_AUTHORIZED to DataAccessException.AccessDenied::class.java,
            StorageException.ERROR_QUOTA_EXCEEDED to DataAccessException.RateLimited::class.java,
            StorageException.ERROR_RETRY_LIMIT_EXCEEDED to DataAccessException.TimedOut::class.java).forEach { (code, expected) ->
            val original = mock(StorageException::class.java)
            `when`(original.errorCode).thenReturn(code)
            assertTrue(expected.isInstance(original.toDataAccessFailure()))
            assertSame(original, original.toDataAccessFailure().cause)
        }
        val unknown = mock(StorageException::class.java)
        `when`(unknown.errorCode).thenReturn(StorageException.ERROR_OBJECT_NOT_FOUND)
        assertSame(unknown, unknown.toDataAccessFailure())
        val timeout = SocketTimeoutException("quota")
        val network = FirebaseNetworkException("permission")
        val quota = FirebaseTooManyRequestsException("network")
        assertTrue(timeout.toDataAccessFailure() is DataAccessException.TimedOut)
        assertTrue(network.toDataAccessFailure() is DataAccessException.NetworkUnavailable)
        assertTrue(quota.toDataAccessFailure() is DataAccessException.RateLimited)
        assertSame(network, network.toDataAccessFailure().cause)
        val typed = DataAccessException.AccessDenied()
        assertSame(typed, typed.toDataAccessFailure())
    }
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
