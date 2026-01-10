package com.virtualclone.app.ml.onnx.model

import com.virtualclone.app.core.common.Result
import kotlinx.coroutines.flow.Flow

/**
 * Interface for Small Language Model operations
 */
interface Slm {
    /**
     * Generate text based on a prompt with streaming output
     * @param prompt The input prompt
     * @param maxTokens Maximum number of tokens to generate
     * @param temperature Controls randomness (0.0 = deterministic, 1.0 = very random)
     * @param topK Number of top tokens to consider for sampling
     * @param topP Nucleus sampling parameter (0.0 to 1.0)
     * @param stop List of stop sequences to halt generation
     * @return Flow of generated text tokens
     */
    fun generate(
        prompt: String,
        maxTokens: Int = 256,
        temperature: Float = 0.9f,
        topK: Int = 40,
        topP: Float = 0.92f,
        stop: List<String> = listOf("\n\nUser:")
    ): Flow<String>
    
    /**
     * Check if the model is ready for inference
     */
    suspend fun isReady(): Boolean
    
    /**
     * Initialize the model
     */
    suspend fun initialize(): Result<Unit>
}
