package com.virtualclone.app.core.domain.usecase.download

import com.virtualclone.app.core.domain.repository.ModelDownloadRepository
import javax.inject.Inject

class InitializeModelMetadataUseCase @Inject constructor(
    private val repository: ModelDownloadRepository
) {
    suspend operator fun invoke(modelIds: List<String>) {
        modelIds.forEach { id ->
            repository.refreshModelSizeIfNeeded(id)
        }
    }
}
