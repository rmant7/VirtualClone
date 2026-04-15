package com.virtualclone.app.core.domain.model

data class LLMModel(
    val id: String,
    val path: String,
    val url: String,
    val licenseUrl: String,
    val needsAuth: Boolean,
    val defaultTemperature: Float,
    val defaultTopK: Int,
    val defaultTopP: Float,
    val preferredBackend: String?, // e.g. "CPU", "GPU", "NNAPI"
    val thinking: Boolean,
    val inferenceEngine: InferenceEngine = InferenceEngine.MEDIA_PIPE 
)