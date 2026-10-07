package com.ahmetkaragunlu.financeai.feature.aichat.data.remote

import android.os.SystemClock
import android.util.Log
import com.ahmetkaragunlu.financeai.core.coroutines.di.DefaultDispatcher
import com.ahmetkaragunlu.financeai.feature.aichat.data.report.AiPromptFormatter
import com.ahmetkaragunlu.financeai.feature.aichat.domain.error.AiException
import com.ahmetkaragunlu.financeai.feature.aichat.domain.generation.AiTextGenerator
import com.ahmetkaragunlu.financeai.feature.aichat.domain.model.FinancialSnapshot
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.FirebaseNetworkException
import com.ahmetkaragunlu.financeai.feature.aichat.di.AiFirebaseApp
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class FirebaseAiTextGenerator @Inject constructor(
    private val model: GenerativeModel,
    private val formatter: AiPromptFormatter,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
    @AiFirebaseApp private val appCheck: FirebaseAppCheck,
    private val requests: AiRequestExecutor
) : AiTextGenerator {
    override suspend fun generate(question: String, snapshot: FinancialSnapshot): String {
        val prompt = withContext(dispatcher) { formatter.format(question, snapshot) }
        val startedAt = SystemClock.elapsedRealtime()
        return try {
            // Fail explicitly before generation when this installation cannot attest to the AI project.
            withTimeoutOrNull(8_000L) {
                try { appCheck.getAppCheckToken(false).await() }
                catch (e: CancellationException) { throw e }
                catch (e: FirebaseNetworkException) { throw AiException.NetworkUnavailable(e) }
                catch (e: Exception) { throw AiException.AccessVerification(e) }
            } ?: throw AiException.TimedOut()
            requests.execute {
                try {
                    model.generateContent(prompt).text?.takeIf { it.isNotBlank() }
                        ?: throw AiException.EmptyResponse()
                } catch (e: Exception) {
                    currentCoroutineContext().ensureActive()
                    throw e.toAiFailure()
                }
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            Log.w("FirebaseAiTextGenerator",
                "AI request failed: ${e.javaClass.simpleName}, elapsedMs=${SystemClock.elapsedRealtime() - startedAt}")
            throw e
        }
    }
}
