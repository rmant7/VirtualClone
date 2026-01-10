package com.virtualclone.app.data.modeldownload.repository

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.virtualclone.app.core.domain.model.ModelDownloadInfo
import com.virtualclone.app.core.domain.repository.ModelDownloadRepository
import com.virtualclone.app.core.domain.repository.ModelRepository
import com.virtualclone.app.data.db.dao.ModelDownloadDao
import com.virtualclone.app.data.modeldownload.datasource.ResumableDownloadManager
import com.virtualclone.app.data.modeldownload.mapper.ModelDownloadMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import com.virtualclone.app.core.common.Result
import com.virtualclone.app.core.domain.hftoken.HfTokenException
import com.virtualclone.app.core.domain.model.LLMModel
import com.virtualclone.app.core.domain.repository.HfTokenProvider
import com.virtualclone.app.data.db.entity.ModelDownloadEntity
import com.virtualclone.app.data.modeldownload.service.ModelDownloadService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class ModelDownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadManager: ResumableDownloadManager,
    private val dao: ModelDownloadDao,
    private val modelRepository: ModelRepository,
    private val hfTokenProvider: HfTokenProvider
) : ModelDownloadRepository {

    private val tag = javaClass.simpleName

    override suspend fun start(modelId: String): Result<Unit> {
        return try {
            val model = resolveModel(modelId)

            val authToken = requireTokenIfNeeded(model)
            startForegroundServiceSafely()

            val fileName = model.url.substringAfterLast("/")

            downloadManager.startOrResume(
                modelName = model.id,
                url = model.url,
                fileName = fileName,
                needsAuth = model.needsAuth,
                authToken = authToken
            )

            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "start() failed", e)
            Result.Error(e)
        }
    }

    override suspend fun pause(modelId: String): Result<Unit> =
        runCatching {
            downloadManager.pause(modelId)
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(it) }
        )

    override suspend fun resume(modelId: String): Result<Unit> =
        start(modelId)

    override suspend fun cancel(modelId: String): Result<Unit> =
        runCatching {
            downloadManager.cancel(modelId)
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(it) }
        )

    override fun observe(modelId: String): Flow<ModelDownloadInfo> =
        downloadManager.observeProgress()
            .filter { it.modelName == modelId }
            .map(ModelDownloadMapper::fromProgress)

    override fun observeAll(): Flow<List<ModelDownloadInfo>>  =
        dao.observeAllFlow()
            .map { list ->
                list.map(ModelDownloadMapper::fromEntity)
            }

    override suspend fun getAll(): Result<List<ModelDownloadInfo>> =
        runCatching {
            dao.getAll().map(ModelDownloadMapper::fromEntity)
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it) }
        )

    // ─────────────────────────────────────────────

    private suspend fun resolveModel(modelId: String) =
        modelRepository.getAvailableModels()
            .let { result ->
                if (result !is Result.Success)
                    throw IllegalStateException("Failed to load model catalog")

                result.data.firstOrNull { it.id == modelId }
                    ?: throw IllegalArgumentException("Unknown modelId=$modelId")
            }

    private suspend fun requireTokenIfNeeded(model: LLMModel): String? {
        if (!model.needsAuth) return null

        return hfTokenProvider.getToken()
            ?: throw HfTokenException.MissingHfTokenException
    }

    private fun startForegroundServiceSafely() {
        try {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ModelDownloadService::class.java)
            )
        } catch (e: Exception) {
            Log.w(tag, "Failed to start foreground service", e)
        }
    }

    override suspend fun refreshModelSizeIfNeeded(modelId: String) =
        withContext(Dispatchers.IO) {

            Log.i(tag, "refreshModelSizeIfNeeded(): model=$modelId")

            val existing = dao.getByName(modelId)
            Log.i(tag, "existing.totalBytes=${existing?.totalBytes}")

            if (existing?.totalBytes != null && existing.totalBytes > 0) {
                return@withContext
            }

            val model = resolveModel(modelId)

            val token = if (model.needsAuth) hfTokenProvider.getToken() else null
            Log.i(tag, "needsAuth=${model.needsAuth}, hfToken=${token != null}")

            if (model.needsAuth && token == null) return@withContext

            val size = try {
                downloadManager.fetchRemoteFileSize(
                    url = model.url,
                    authToken = token
                )
            } catch (e: Exception) {
                Log.e(tag, "HEAD request failed", e)
                return@withContext
            }

            if (size <= 0) return@withContext

            dao.upsert(
                ModelDownloadEntity(
                    modelName = model.id,
                    url = model.url,
                    fileName = model.url.substringAfterLast("/"),
                    totalBytes = size
                )
            )

            Log.i(tag, "Persisted size=$size for model=${model.id}")
        }
}
