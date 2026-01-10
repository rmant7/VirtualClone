package com.virtualclone.app.ml.llm.impl

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.virtualclone.app.ml.llm.model.LlmModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GemmaLlm(private val context: Context) : LlmModel {

    private var llm: LlmInference? = null

    override suspend fun initialize(): Boolean {
        return try {
            val taskOptions = LlmInference.LlmInferenceOptions.builder()
                .setModelPath("models/Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.task")
                .setMaxTopK(64)
                .build()
            llm = LlmInference.createFromOptions(context, taskOptions)
            Log.d("GemmaLlm", "Model initialized successfully")
            true
        } catch (e: Exception) {
            Log.e("GemmaLlm", "Error initializing model", e)
            false
        }
    }

    override suspend fun generate(prompt: String): Flow<String> = flow {
        try {
            val response = llm?.generateResponse(prompt) ?: ""
            emit(response)
        } catch (e: Exception) {
            Log.e("GemmaLlm", "Error generating response", e)
            emit("Error: ${e.message}")
        }
    }
}
