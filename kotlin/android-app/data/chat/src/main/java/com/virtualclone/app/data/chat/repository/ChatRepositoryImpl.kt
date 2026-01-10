package com.virtualclone.app.data.chat.repository

import com.virtualclone.app.core.common.Logger
import com.virtualclone.app.core.common.Result
import com.virtualclone.app.core.domain.di.IoDispatcher
import com.virtualclone.app.core.domain.model.ChatMessage
import com.virtualclone.app.core.domain.model.Document
import com.virtualclone.app.core.domain.model.DocumentChunk
import com.virtualclone.app.core.domain.repository.ChatRepository
import com.virtualclone.app.data.chat.mapper.ChatMessageMapper
import com.virtualclone.app.data.chat.mapper.ChunkMapper
import com.virtualclone.app.data.chat.mapper.DocumentMapper
import com.virtualclone.app.data.db.VirtualCloneDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

@Singleton
class ChatRepositoryImpl @Inject constructor(
    database: VirtualCloneDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ChatRepository {

    private val tag = ChatRepositoryImpl::class.java.simpleName
    private val messageDao = database.messageDao()
    private val documentDao = database.documentDao()
    private val chunkDao = database.chunkDao()

    override suspend fun saveMessage(
        conversationId: String,
        message: ChatMessage
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            Logger.d(
                "Saving message: conversationId=$conversationId author=${message.author}",
                tag
            )

            val entity = ChatMessageMapper.toEntity(message, conversationId)
            messageDao.insertMessage(entity)

            Logger.i("Message saved successfully", tag)
            Result.Success(Unit)

        } catch (e: Exception) {
            Logger.e("Failed to save message", e, tag)
            Result.Error(e)
        }
    }

    override fun getMessages(
        conversationId: String
    ): Flow<List<ChatMessage>> {
        Logger.d("Observing messages for conversationId=$conversationId", tag)

        return messageDao
            .getMessagesByConversation(conversationId)
            .map { entities ->
                entities.map(ChatMessageMapper::toDomain)
            }
    }

    override suspend fun clearConversation(
        conversationId: String
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            Logger.i("Clearing conversation: conversationId=$conversationId", tag)

            messageDao.deleteMessagesByConversation(conversationId)

            Logger.i("Conversation cleared successfully", tag)
            Result.Success(Unit)

        } catch (e: Exception) {
            Logger.e("Failed to clear conversation", e, tag)
            Result.Error(e)
        }
    }

    override suspend fun getDocumentContext(
        documentId: String
    ): Result<Pair<Document?, List<DocumentChunk>>> =
        withContext(ioDispatcher) {
            try {
                Logger.d("Fetching document context: documentId=$documentId", tag)

                val documentEntity = documentDao.getDocumentById(documentId)
                val chunkEntities = chunkDao.getChunksByDocumentSync(documentId)

                val document = documentEntity?.let(DocumentMapper::toDomain)
                val chunks = chunkEntities.map(ChunkMapper::toDomain)

                Logger.i(
                    "Document context loaded: chunks=${chunks.size}",
                    tag
                )

                Result.Success(document to chunks)

            } catch (e: Exception) {
                Logger.e("Failed to load document context", e, tag)
                Result.Error(e)
            }
        }

    override suspend fun getRelevantChunks(
        query: String,
        limit: Int
    ): Result<List<DocumentChunk>> =
        withContext(ioDispatcher) {
            try {
                Logger.d(
                    "Searching relevant chunks: query='$query' limit=$limit",
                    tag
                )

                val chunks =
                    chunkDao.searchChunks(query, limit).map(ChunkMapper::toDomain)

                Logger.i(
                    "Relevant chunks found: count=${chunks.size}",
                    tag
                )

                Result.Success(chunks)

            } catch (e: Exception) {
                Logger.e("Failed to search relevant chunks", e, tag)
                Result.Error(e)
            }
        }
}