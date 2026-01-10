package com.virtualclone.app.core.domain.usecase.model

import com.virtualclone.app.core.domain.repository.ModelRepository
import com.virtualclone.app.core.domain.usecase.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import com.virtualclone.app.core.common.Result
import com.virtualclone.app.core.domain.di.IoDispatcher

class CloseModelUseCase @Inject constructor(
    private val repo: ModelRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : BaseUseCase(dispatcher) {
    suspend operator fun invoke(): Result<Unit> = runOnDispatcher { repo.closeModel() }
}