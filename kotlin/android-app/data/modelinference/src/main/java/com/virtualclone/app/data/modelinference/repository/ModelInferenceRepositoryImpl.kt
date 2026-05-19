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
import com.virtualclone.app.core.domain.model.WhisperModel
import com.virtualclone.app.core.domain.model.WhisperModelsCatalog
import com.virtualclone.app.data.modelinference.legacy.ModelLoadFailException
import com.virtualclone.app.data.modelinference.legacy.ModelSessionCreateFailException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
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
    private val modelDownloadDao: com.virtualclone.app.data.db.dao.ModelDownloadDao
) : ModelRepository {

    private val tag = ModelInferenceDataRepositoryImpl::class.java.simpleName

    private var llmInference: LlmInference? = null
    private var llmInferenceSession: LlmInferenceSession? = null
    private var currentModel: LLMModel? = null

    private var activeWhisperModelId: String = WhisperModelsCatalog.tiny.id

    companion object {
        private const val MAX_TOKENS = 1024
        private const val DECODE_TOKEN_OFFSET = 256
    }

    private suspend fun resolve(modelId: String): LLMModel {
        val catalogModel = LLMModelsCatalog.all.find { it.id == modelId }
        if (catalogModel != null) return catalogModel

        // Check if it's a custom model from the database
        val entity = withContext(ioDispatcher) { modelDownloadDao.getByName(modelId) }
        if (entity != null) {
            return LLMModel(
                id = entity.modelName,
                path = File(context.filesDir, entity.fileName).absolutePath,
                url = entity.url,
                licenseUrl = "",
                needsAuth = entity.url.contains("huggingface.co"),
                defaultTemperature = 0.7f,
                defaultTopK = 40,
                defaultTopP = 0.95f,
                preferredBackend = "CPU",
                thinking = false
            )
        }

        throw IllegalArgumentException("Unknown modelId=$modelId")
    }

    // ─────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────
    override suspend fun initializeModel(modelId: String): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                val model = resolve(modelId)
                Logger.i("Initializing model=$modelId", tag)

                val file = getModelLocalFile(modelId)
                // If the guessed file doesn't exist, we might need to find it by name if it's a custom model
                val actualFile = if (file.exists()) file else {
                    context.filesDir.listFiles()?.find { 
                        it.name.contains(modelId, ignoreCase = true) || it.name == model.path.substringAfterLast("/") 
                    } ?: file
                }

                if (!actualFile.exists()) {
                    throw IllegalStateException("Model file missing: ${actualFile.absolutePath}")
                }

                currentModel = model
                createEngineWithFile(actualFile)
                createSession(model)

                Logger.i("Model initialized successfully: $modelId", tag)
                Result.Success(Unit)

            } catch (e: Exception) {
                Logger.e("Model initialization failed", e, tag)
                Result.Error(e)
            }
        }

    override suspend fun initializeWhisper(
        modelId: String,
        onProgress: (Float) -> Unit
    ): com.virtualclone.app.core.common.Result<Unit> =
        withContext(ioDispatcher) {
            try {
                val model = WhisperModelsCatalog.find(modelId)
                val finalFile = getWhisperModelFile(model.id)

                if (!finalFile.exists()) {
                    Logger.i("Whisper model needs download: ${model.id} to ${finalFile.absolutePath}", tag)
                    finalFile.parentFile?.mkdirs()
                    downloadFile(model.url, finalFile, model.needsAuth, onProgress)
                }
                
                activeWhisperModelId = model.id
                com.virtualclone.app.core.common.Result.Success(Unit)
            } catch (e: Exception) {
                Logger.e("Whisper initialization failed", e, tag)
                com.virtualclone.app.core.common.Result.Error(e)
            }
        }

    override fun getWhisperModelFile(modelId: String): File {
        val model = WhisperModelsCatalog.find(modelId)
        val fileName = model.url.substringAfterLast("/")
        val normalizedName = model.name.lowercase()
        val version = model.version

        val possibleDirs = context.getExternalFilesDirs(null).filterNotNull()
        val expectedWorkerPath = File(possibleDirs[0], "$normalizedName/$version/$fileName")

        // Check if file already exists in expected path or anywhere in app files
        if (expectedWorkerPath.exists()) return expectedWorkerPath

        // Search for it in fallback locations
        context.filesDir.listFiles()?.find { it.name == fileName }?.let { return it }
        
        possibleDirs.forEach { dir ->
            dir.listFiles()?.find { it.name == fileName }?.let { return it }
        }

        return expectedWorkerPath
    }

    override suspend fun getActiveWhisperModel(): WhisperModel? {
        return WhisperModelsCatalog.find(activeWhisperModelId)
    }

    override suspend fun getAvailableWhisperModels(): List<WhisperModel> = WhisperModelsCatalog.all

    override suspend fun setWhisperModel(modelId: String): com.virtualclone.app.core.common.Result<Unit> {
        activeWhisperModelId = modelId
        return com.virtualclone.app.core.common.Result.Success(Unit)
    }

    private suspend fun downloadFile(
        url: String, 
        target: File,
        needsAuth: Boolean = false,
        onProgress: (Float) -> Unit = {}
    ) {
        val requestBuilder = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "VirtualClone-Android")
            .addHeader("Accept", "*/*")
        
        if (needsAuth && url.contains("huggingface.co")) {
            val token = hfTokenProvider.getToken()
            if (token != null) {
                Logger.d("Adding HF token to request: ${url.take(50)}...", tag)
                requestBuilder.addHeader("Authorization", "Bearer $token")
            } else {
                Logger.w("needsAuth is true but no HF token found for url: $url", tag)
            }
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Download failed: ${response.code} ${response.message}")
            
            val body = response.body ?: throw Exception("Response body is null")
            val totalBytes = body.contentLength()
            
            body.byteStream().use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            onProgress(totalRead.toFloat() / totalBytes)
                        }
                    }
                }
            }
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

    override suspend fun getAvailableModels(): Result<List<LLMModel>> =
        withContext(ioDispatcher) {
            try {
                val catalogModels = LLMModelsCatalog.all
                val customModels = modelDownloadDao.getAll().filter { entity ->
                    catalogModels.none { it.id == entity.modelName }
                }.map { entity ->
                    LLMModel(
                        id = entity.modelName,
                        path = File(context.filesDir, entity.fileName).absolutePath,
                        url = entity.url,
                        licenseUrl = "",
                        needsAuth = entity.url.contains("huggingface.co"),
                        defaultTemperature = 0.7f,
                        defaultTopK = 40,
                        defaultTopP = 0.95f,
                        preferredBackend = "CPU",
                        thinking = false
                    )
                }
                Result.Success(catalogModels + customModels)
            } catch (e: Exception) {
                Logger.e("Failed to get available models", e, tag)
                Result.Error(e)
            }
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
            .addHeader("Accept", "*/*")
            .addHeader("Range", "bytes=0-0")

        if (model.needsAuth && model.url.contains("huggingface.co")) {
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
        val catalogModel = LLMModelsCatalog.all.find { it.id == modelId }
        val fileName = if (catalogModel != null) {
            catalogModel.url.substringAfterLast("/")
        } else {
            // For custom models, we can't easily get the filename synchronously.
            // But we know that startWithUrl in ModelDownloadRepositoryImpl uses url.substringAfterLast("/")
            // and stores it in the DB.
            // If we are here, we might just have to guess or check the DB (which we shouldn't do sync).
            // However, the ModelDownloadRepositoryImpl also uses resolveModel which fails for custom models.
            // Wait, I should fix that too.
            modelId
        }
        return File(context.filesDir, fileName)
    }

    override fun modelExists(modelId: String): Boolean {
        val catalogModel = LLMModelsCatalog.all.find { it.id == modelId }
        if (catalogModel != null) {
            val file = File(context.filesDir, catalogModel.url.substringAfterLast("/"))
            return file.exists() && file.length() > 0
        }

        // Check if any file in context.filesDir matches common model extensions and contains modelId
        // or just look for the modelId file if we saved it as such.
        val file = File(context.filesDir, modelId)
        if (file.exists() && file.length() > 0) return true

        // Last resort: list files and see if any match the modelId (which is the modelName in DB)
        val files = context.filesDir.listFiles()
        return files?.any { it.name.contains(modelId, ignoreCase = true) && it.length() > 0 } ?: false
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
    private fun createEngineWithFile(file: File) {
        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(file.absolutePath)
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