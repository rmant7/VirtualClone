package com.virtualclone.app.core.domain.usecase.download

import com.virtualclone.app.core.domain.model.ModelDownloadInfo
import com.virtualclone.app.core.domain.repository.ModelDownloadRepository
import com.virtualclone.app.core.domain.usecase.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import com.virtualclone.app.core.common.Result
import com.virtualclone.app.core.domain.di.IoDispatcher

class GetAllModelDownloadsUseCase @Inject constructor(
    private val repo: ModelDownloadRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : BaseUseCase(dispatcher) {
    suspend operator fun invoke(): Result<List<ModelDownloadInfo>> = runOnDispatcher { repo.getAll() }
}