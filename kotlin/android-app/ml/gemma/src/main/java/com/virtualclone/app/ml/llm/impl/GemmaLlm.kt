package com.virtualclone.app.ml.llm.impl

import android.content.Context
import android.util.Log
import com.google.common.util.concurrent.ListenableFuture
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession.LlmInferenceSessionOptions
import com.google.mediapipe.tasks.genai.llminference.ProgressListener
import java.io.File

class GemmaLlm(private val context: Context) {

    private val tag = GemmaLlm::class.java.simpleName

    private var llmInference: LlmInference? = null
    private var llmInferenceSession: LlmInferenceSession? = null

    companion object {
        private const val MAX_TOKENS = 1024

        // Gemma chat template
        fun formatPrompt(userMessage: String): String =
            "<start_of_turn>user\n$userMessage<end_of_turn>\n<start_of_turn>model\n"
    }

    fun initialize(
        modelFile: File,
        temperature: Float = 0.9f,
        topK: Int = 64,
        topP: Float = 1.0f
    ): Boolean {
        return try {
            close()

            Log.d(tag, "Initializing Gemma: ${modelFile.absolutePath}")

            val inferenceOptions = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(MAX_TOKENS)
                .build()

            llmInference = LlmInference.createFromOptions(context, inferenceOptions)

            val sessionOptions = LlmInferenceSessionOptions.builder()
                .setTemperature(temperature)
                .setTopK(topK)
                .setTopP(topP)
                .build()

            llmInferenceSession = LlmInferenceSession.createFromOptions(llmInference!!, sessionOptions)

            Log.i(tag, "Gemma ready")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize Gemma", e)
            false
        }
    }

    fun resetSession(
        temperature: Float = 0.9f,
        topK: Int = 64,
        topP: Float = 1.0f
    ): Boolean {
        return try {
            llmInferenceSession?.close()
            val sessionOptions = LlmInferenceSessionOptions.builder()
                .setTemperature(temperature)
                .setTopK(topK)
                .setTopP(topP)
                .build()
            llmInferenceSession = LlmInferenceSession.createFromOptions(llmInference!!, sessionOptions)
            Log.d(tag, "Gemma session reset")
            true
        } catch (e: Exception) {
            Log.e(tag, "Gemma session reset failed", e)
            false
        }
    }

    fun close() {
        try {
            llmInferenceSession?.close()
            llmInferenceSession = null
            llmInference?.close()
            llmInference = null
        } catch (e: Exception) {
            Log.e(tag, "Error closing Gemma", e)
        }
    }

    val isReady: Boolean
        get() = llmInferenceSession != null

    fun generateResponseAsync(
        userMessage: String,
        listener: ProgressListener<String>
    ): ListenableFuture<String> {
        val session = llmInferenceSession
            ?: throw IllegalStateException("Gemma session not initialized")

        session.addQueryChunk(formatPrompt(userMessage))
        return session.generateResponseAsync(listener)
    }
}