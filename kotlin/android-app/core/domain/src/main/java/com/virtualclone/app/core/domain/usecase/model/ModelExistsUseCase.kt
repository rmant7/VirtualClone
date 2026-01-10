package com.virtualclone.app.core.domain.usecase.model

import com.virtualclone.app.core.domain.di.IoDispatcher
import com.virtualclone.app.core.domain.repository.ModelRepository
import com.virtualclone.app.core.domain.usecase.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ModelExistsUseCase @Inject constructor(
    private val repo: ModelRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : BaseUseCase(dispatcher) {

    suspend operator fun invoke(modelId: String): Boolean = runOnDispatcher {
        repo.modelExists(modelId)
    }
}