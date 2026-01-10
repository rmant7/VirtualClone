package com.virtualclone.app.core.common.model

data class ModelUi(
    val id: String,
    val displayName: String,
    val needsAuth: Boolean,
    val thinking: Boolean,
    val defaultTemperature: Float,
    val defaultTopK: Int,
    val defaultTopP: Float,
    val preferredBackend: String?,
    val sizeBytes: Long? = null
)