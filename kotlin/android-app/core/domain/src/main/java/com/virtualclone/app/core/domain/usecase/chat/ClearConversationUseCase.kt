package com.virtualclone.app.core.domain.usecase.chat

import com.virtualclone.app.core.domain.di.IoDispatcher
import com.virtualclone.app.core.domain.repository.ChatRepository
import com.virtualclone.app.core.domain.usecase.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ClearConversationUseCase @Inject constructor(
    private val repository: ChatRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : BaseUseCase(dispatcher) {

    suspend operator fun invoke(conversationId: String) =
        runOnDispatcher {
            repository.clearConversation(conversationId)
        }
}