package com.virtualclone.app.core.domain.repository

import com.google.common.util.concurrent.ListenableFuture
import com.google.mediapipe.tasks.genai.llminference.ProgressListener
import com.virtualclone.app.core.common.Result
import com.virtualclone.app.core.domain.model.ChatMessage
import com.virtualclone.app.core.domain.model.LLMModel
import java.io.File

interface ModelRepository {

    // ─────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────
    suspend fun initializeModel(modelId: String): Result<Unit>
    suspend fun closeModel(): Result<Unit>
    suspend fun resetSession(): Result<Unit>

    // ─────────────────────────────────────────────────────────────
    // Active model
    // ─────────────────────────────────────────────────────────────
    suspend fun setActiveModel(modelId: String): Result<Unit>
    suspend fun getActiveModel(): Result<LLMModel>

    // ─────────────────────────────────────────────────────────────
    // Catalog
    // ─────────────────────────────────────────────────────────────
    suspend fun getAvailableModels(): Result<List<LLMModel>>

    // ─────────────────────────────────────────────────────────────
    // File / Validation
    // ─────────────────────────────────────────────────────────────
    fun getModelLocalFile(modelId: String): File
    fun modelExists(modelId: String): Boolean

    // ─────────────────────────────────────────────────────────────
    // Inference (used by Chat)
    // ─────────────────────────────────────────────────────────────
    fun generateResponseAsync(
        prompt: String,
        listener: ProgressListener<String>
    ): ListenableFuture<String>

    fun estimateTokensRemaining(
        prompt: String,
        messages: List<ChatMessage>
    ): Int
}