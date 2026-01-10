package com.virtualclone.app.data.chat.mapper

import com.virtualclone.app.core.domain.model.ChatMessage
import com.virtualclone.app.core.domain.model.ModelPhase
import com.virtualclone.app.data.db.entity.MessageEntity
import java.util.Date

object ChatMessageMapper {

    fun toDomain(entity: MessageEntity): ChatMessage =
        ChatMessage(
            id = entity.id,
            author = entity.author,
            text = entity.rawMessage,
            phase = runCatching {
                ModelPhase.valueOf(entity.phase)
            }.getOrElse {
                ModelPhase.DONE
            },
            isLoading = false // ❗ never restored from DB
        )

    fun toEntity(
        message: ChatMessage,
        conversationId: String
    ): MessageEntity =
        MessageEntity(
            id = message.id,
            rawMessage = message.text,
            author = message.author,
            phase = message.phase.name,
            timestamp = Date(),
            conversationId = conversationId
        )
}