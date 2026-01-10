package com.virtualclone.app.data.modelinference.repository

import android.content.Context
import com.google.common.util.concurrent.ListenableFuture
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession.LlmInferenceSessionOptions
import com.google.mediapipe.tasks.genai.llminference.ProgressListener
import com.virtualclone.app.core.common.Logger
import com.virtualclone.app.core.common.Result
import com.virtualclone.app.core.domain.di.IoDispatcher
import com.virtualclone.app.core.domain.model.ChatMessage
import com.virtualclone.app.core.domain.model.LLMModel
import com.virtualclone.app.core.domain.model.LLMModelsCatalog
import com.virtualclone.app.core.domain.repository.HfTokenProvider
import com.virtualclone.app.core.domain.repository.ModelRepository
import com.virtualclone.app.data.modelinference.legacy.ModelLoadFailException
import com.virtualclone.app.data.modelinference.legacy.ModelSessionCreateFailException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class ModelInferenceDataRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val hfTokenProvider: HfTokenProvider,
    private val client: OkHttpClient,
) : ModelRepository {

    private val tag = ModelInferenceDataRepositoryImpl::class.java.simpleName

    private var llmInference: LlmInference? = null
    private var llmInferenceSession: LlmInferenceSession? = null
    private var currentModel: LLMModel? = null

    companion object {
        private const val MAX_TOKENS = 1024
        private const val DECODE_TOKEN_OFFSET = 256
    }

    private fun resolve(modelId: String): LLMModel =
        LLMModelsCatalog.find(modelId)

    // ─────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────
    override suspend fun initializeModel(modelId: String): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                val model = resolve(modelId)
                Logger.i("Initializing model=$modelId", tag)

                val file = getModelLocalFile(modelId)
                if (!file.exists()) {
                    throw IllegalStateException("Model file missing: ${file.absolutePath}")
                }

                currentModel = model
                createEngine(model)
                createSession(model)

                Logger.i("Model initialized successfully: $modelId", tag)
                Result.Success(Unit)

            } catch (e: Exception) {
                Logger.e("Model initialization failed", e, tag)
                Result.Error(e)
            }
        }

    override suspend fun closeModel(): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                llmInferenceSession?.close()
                llmInferenceSession = null

                llmInference?.close()
                llmInference = null

                Logger.i("Model closed", tag)
                Result.Success(Unit)

            } catch (e: Exception) {
                Logger.e("Failed to close model", e, tag)
                Result.Error(e)
            }
        }

    override suspend fun resetSession(): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                llmInferenceSession?.close()
                currentModel?.let { createSession(it) }

                Logger.i("Session reset", tag)
                Result.Success(Unit)

            } catch (e: Exception) {
                Logger.e("Session reset failed", e, tag)
                Result.Error(e)
            }
        }

    // ─────────────────────────────────────────────────────────────
    // Active model
    // ─────────────────────────────────────────────────────────────

    override suspend fun setActiveModel(modelId: String): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                val model = resolve(modelId)
                currentModel = model
                Logger.i("Active model set: ${model.id}", tag)
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }

    override suspend fun getActiveModel(): Result<LLMModel> =
        withContext(ioDispatcher) {
            currentModel?.let {
                Result.Success(it)
            } ?: Result.Error(
                IllegalStateException("No active model selected")
            )
        }

    // ─────────────────────────────────────────────────────────────
    // Catalog
    // ─────────────────────────────────────────────────────────────
    override suspend fun getAvailableModels(): Result<List<LLMModel>> =
        try {
            Result.Success(LLMModelsCatalog.all)
        } catch (e: Exception) {
            Result.Error(e)
        }

    // ─────────────────────────────────────────────────────────────
    // Files
    // ─────────────────────────────────────────────────────────────

    private suspend fun probeRemoteSize(
        model: LLMModel
    ): Long {
        val requestBuilder = Request.Builder()
            .url(model.url)
            .addHeader("User-Agent", "VirtualClone-Android")
            .addHeader("Range", "bytes=0-0")

        if (model.needsAuth) {
            hfTokenProvider.getToken()?.let {
                requestBuilder.addHeader("Authorization", "Bearer $it")
            }
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (response.code !in listOf(200, 206)) return -1L

            val contentRange = response.header("Content-Range") ?: return -1L
            return contentRange.substringAfter("/").toLongOrNull() ?: -1L
        }
    }

    override fun getModelLocalFile(modelId: String): File {
        val model = resolve(modelId)
        val fileName = model.url.substringAfterLast("/")
        return File(context.filesDir, fileName)
    }

    override fun modelExists(modelId: String): Boolean {
        val file = getModelLocalFile(modelId)
        return file.exists() && file.length() > 0
    }

    // ─────────────────────────────────────────────────────────────
    // Inference
    // ─────────────────────────────────────────────────────────────
    override fun generateResponseAsync(
        prompt: String,
        listener: ProgressListener<String>
    ): ListenableFuture<String> {
        val session =
            llmInferenceSession ?: throw IllegalStateException("Session not initialized")
        session.addQueryChunk(prompt)
        return session.generateResponseAsync(listener)
    }

    override fun estimateTokensRemaining(
        prompt: String,
        messages: List<ChatMessage>
    ): Int {
        val session = llmInferenceSession ?: return -1
        val context = messages.joinToString { it.text } + prompt
        val used = session.sizeInTokens(context)
        return max(0, MAX_TOKENS - used - DECODE_TOKEN_OFFSET)
    }

    // ─────────────────────────────────────────────────────────────
    // Internal
    // ─────────────────────────────────────────────────────────────
    private fun createEngine(model: LLMModel) {
        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(getModelLocalFile(model.id).absolutePath)
                .setMaxTokens(MAX_TOKENS)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
        } catch (e: Exception) {
            throw ModelLoadFailException()
        }
    }

    private fun createSession(model: LLMModel) {
        try {
            val opts = LlmInferenceSessionOptions.builder()
                .setTemperature(model.defaultTemperature)
                .setTopK(model.defaultTopK)
                .setTopP(model.defaultTopP)
                .build()

            llmInferenceSession =
                LlmInferenceSession.createFromOptions(llmInference!!, opts)
        } catch (e: Exception) {
            throw ModelSessionCreateFailException()
        }
    }
}