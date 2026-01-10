package com.virtualclone.app.core.domain.repository

import com.virtualclone.app.core.common.Result
import com.virtualclone.app.core.domain.model.ChatMessage
import com.virtualclone.app.core.domain.model.Document
import com.virtualclone.app.core.domain.model.DocumentChunk
import kotlinx.coroutines.flow.Flow

interface ChatRepository {

    suspend fun saveMessage(
        conversationId: String,
        message: ChatMessage
    ): Result<Unit>

    fun getMessages(
        conversationId: String
    ): Flow<List<ChatMessage>>

    suspend fun clearConversation(
        conversationId: String
    ): Result<Unit>

    suspend fun getDocumentContext(
        documentId: String
    ): Result<Pair<Document?, List<DocumentChunk>>>

    suspend fun getRelevantChunks(
        query: String,
        limit: Int = 5
    ): Result<List<DocumentChunk>>
}
