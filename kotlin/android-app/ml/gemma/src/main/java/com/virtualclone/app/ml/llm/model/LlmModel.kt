package com.virtualclone.app.ml.llm.model

import kotlinx.coroutines.flow.Flow

interface LlmModel {
    suspend fun initialize(): Boolean
    suspend fun generate(prompt: String): Flow<String>
}
