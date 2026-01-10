package com.virtualclone.app.core.domain.usecase.model

import com.virtualclone.app.core.common.Result
import com.virtualclone.app.core.domain.di.IoDispatcher
import com.virtualclone.app.core.domain.usecase.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * Filters a list of modelIds down to those that are
 * available locally AND valid.
 *
 * This keeps ViewModels free from repository logic.
 */
class GetDownloadedModelsUseCase @Inject constructor(
    private val modelExistsUseCase: ModelExistsUseCase,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : BaseUseCase(dispatcher) {

    /**
     * Returns only modelIds that both exist locally and are valid.
     * Checks are executed in parallel.
     */
    suspend operator fun invoke(
        modelIds: List<String>
    ): Result<List<String>> =
        runOnDispatcher {
            try {
                coroutineScope {
                    val deferred = modelIds.map { id ->
                        async {
                            val exists = modelExistsUseCase(id)
                            if (exists) id else null
                        }
                    }

                    val filtered = deferred.mapNotNull { it.await() }
                    Result.Success(filtered)
                }
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
}