package com.virtualclone.app.feature.onboarding.ui

import com.virtualclone.app.core.domain.model.DownloadStatus
import com.virtualclone.app.feature.onboarding.ModelDownloadUiStatus

data class ModelDownloadUiItem(
    val modelId: String,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = -1L,
    val status: ModelDownloadUiStatus = ModelDownloadUiStatus.NOT_STARTED,
    val errorMessage: String? = null
) {
    val progressPercentage: Int
        get() = when {
            status == ModelDownloadUiStatus.COMPLETED -> 100
            totalBytes > 0 -> ((bytesDownloaded.toFloat() / totalBytes.toFloat()) * 100).toInt()
            else -> 0
        }

    val downloadedMB: Float get() = bytesDownloaded / (1024f * 1024f)
    val totalMB: Float get() = if (totalBytes > 0) totalBytes / (1024f * 1024f) else 0f
}