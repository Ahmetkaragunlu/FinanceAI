package com.ahmetkaragunlu.financeai.feature.aichat.data.repository

import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.feature.aichat.data.local.AiConversationStore
import com.ahmetkaragunlu.financeai.feature.aichat.data.report.FinancialSnapshotSource
import com.ahmetkaragunlu.financeai.feature.aichat.domain.model.AiRequest
import com.ahmetkaragunlu.financeai.feature.aichat.domain.generation.AiTextGenerator
import com.ahmetkaragunlu.financeai.feature.aichat.domain.repository.AiRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AiRepositoryImpl @Inject constructor(
    private val generator: AiTextGenerator,
    private val conversations: AiConversationStore,
    private val snapshots: FinancialSnapshotSource,
    private val session: AccountSession
) : AiRepository {
    private val sends = Mutex()
    override fun observeChatHistory() = conversations.observe()

    override suspend fun sendMessage(request: AiRequest) = sends.withLock {
        require(request.id.isNotBlank() && '/' !in request.id && request.text.isNotBlank())
        val account = session.requireAccount()
        if (request.ownerId != account.ownerId) throw CancellationException("Stale account")
        val replyId = "${request.id}_reply"
        if (conversations.contains(account, replyId)) return@withLock
        conversations.save(account, request.id, request.text, false)
        val snapshot = snapshots.read(account)
        if (!session.isCurrent(account)) throw CancellationException("Stale account")
        val response = generator.generate(request.text, snapshot)
        if (!session.isCurrent(account)) throw CancellationException("Stale account")
        conversations.save(account, replyId, response, true)
    }
}
