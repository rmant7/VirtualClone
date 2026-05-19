package com.virtualclone.app.core.domain.model

data class WhisperModel(
    val id: String,
    val name: String,
    val size: String, // "Tiny", "Base", "Small", "Medium", "Large"
    val url: String,
    val sizeBytes: Long,
    val version: String = "v1",
    val needsAuth: Boolean = false
)

object WhisperModelsCatalog {
    val tiny = WhisperModel(
        id = "whisper_tiny",
        name = "Tiny",
        size = "Tiny",
        url = "https://huggingface.co/cik009/whisper/resolve/main/whisper-tiny.tflite",
        sizeBytes = 75_000_000L
    )

    val base = WhisperModel(
        id = "whisper_base",
        name = "Base",
        size = "Base",
        url = "https://huggingface.co/cik009/whisper/resolve/main/whisper-base.tflite",
        sizeBytes = 145_000_000L
    )

    val small = WhisperModel(
        id = "whisper_small",
        name = "Small",
        size = "Small",
        url = "https://huggingface.co/cik009/whisper/resolve/main/whisper-small.tflite",
        sizeBytes = 480_000_000L
    )

    val medium = WhisperModel(
        id = "whisper_medium",
        name = "Medium",
        size = "Medium",
        url = "https://huggingface.co/cik009/whisper/resolve/main/whisper-medium.tflite",
        sizeBytes = 1_500_000_000L
    )

    val large = WhisperModel(
        id = "whisper_large",
        name = "Large",
        size = "Large",
        url = "https://huggingface.co/cik009/whisper/resolve/main/whisper-large.tflite",
        sizeBytes = 3_000_000_000L
    )

    val all = listOf(tiny, base, small, medium, large)

    fun find(id: String) = all.find { it.id == id } ?: tiny
}
