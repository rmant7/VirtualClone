package com.virtualclone.app.core.domain.usecase.chat

import com.virtualclone.app.core.domain.repository.ChatRepository
import javax.inject.Inject

class ObserveChatMessagesUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(conversationId: String) =
        repository.getMessages(conversationId)
}