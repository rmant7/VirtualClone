package com.virtualclone.app.core.domain.usecase.download

import com.virtualclone.app.core.domain.model.ModelDownloadInfo
import com.virtualclone.app.core.domain.repository.ModelDownloadRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Returns the flow from ModelDownloadRepository filtered/forwarded for a modelId.
 * This does not use BaseUseCase (dispatcher) because Flow is asynchronous — consumers
 * can collect it on any dispatcher they prefer.
 */
class ObserveModelDownloadProgressUseCase @Inject constructor(
    private val repo: ModelDownloadRepository
) {
    operator fun invoke(modelId: String): Flow<ModelDownloadInfo> =
        repo.observe(modelId)
}