package com.virtualclone.app.core.domain.model

import java.util.UUID

const val USER_PREFIX = "user"
const val MODEL_PREFIX = "model"
const val THINKING_MARKER_END = "</think>"

enum class ModelPhase {
    THINKING,
    STREAMING,
    DONE
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val author: String,
    val text: String = "",
    val phase: ModelPhase = ModelPhase.DONE,
    val isLoading: Boolean = false
) {
    val isFromUser: Boolean get() = author == USER_PREFIX
}