package com.virtualclone.app.core.domain.usecase.download

import com.virtualclone.app.core.domain.di.IoDispatcher
import com.virtualclone.app.core.domain.repository.ModelDownloadRepository
import com.virtualclone.app.core.domain.usecase.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class PauseModelDownloadUseCase @Inject constructor(
    private val repo: ModelDownloadRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : BaseUseCase(dispatcher) {
    suspend operator fun invoke(modelId: String) = runOnDispatcher { repo.pause(modelId) }
}