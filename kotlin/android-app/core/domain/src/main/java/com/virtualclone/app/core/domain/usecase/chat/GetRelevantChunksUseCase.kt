package com.virtualclone.app.core.domain.usecase.chat

import com.virtualclone.app.core.domain.repository.ChatRepository
import javax.inject.Inject

class GetRelevantChunksUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(query: String, limit: Int = 5) =
        repository.getRelevantChunks(query, limit)
}
