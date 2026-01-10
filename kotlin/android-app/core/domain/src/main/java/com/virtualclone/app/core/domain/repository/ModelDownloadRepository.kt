package com.virtualclone.app.core.domain.repository

import com.virtualclone.app.core.domain.model.ModelDownloadInfo
import kotlinx.coroutines.flow.Flow
import com.virtualclone.app.core.common.Result

interface ModelDownloadRepository {
    suspend fun start(modelId: String): Result<Unit>
    suspend fun pause(modelId: String): Result<Unit>
    suspend fun resume(modelId: String): Result<Unit>
    suspend fun cancel(modelId: String): Result<Unit>

    /** Observe progressive updates for a single model id. */
    fun observe(modelId: String): Flow<ModelDownloadInfo>

    /** Observe progressive updates for all models (stream of per-model updates). */
    fun observeAll(): Flow<List<ModelDownloadInfo>>

    suspend fun getAll(): Result<List<ModelDownloadInfo>>
    suspend fun refreshModelSizeIfNeeded(modelId: String)
}