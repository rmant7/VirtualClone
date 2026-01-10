package com.virtualclone.app.core.domain.model

data class ModelDownloadInfo(
    val modelId: String,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = -1L,
    val status: DownloadStatus = DownloadStatus.NOT_STARTED
)

enum class DownloadStatus {
    NOT_STARTED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    DELETED
}