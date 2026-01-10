package com.virtualclone.app.core.domain.usecase.download

import com.virtualclone.app.core.domain.model.ModelDownloadInfo
import com.virtualclone.app.core.domain.repository.ModelDownloadRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveModelMetadataUseCase @Inject constructor(
    private val repository: ModelDownloadRepository
) {
    operator fun invoke(): Flow<List<ModelDownloadInfo>> =
        repository.observeAll()
}
