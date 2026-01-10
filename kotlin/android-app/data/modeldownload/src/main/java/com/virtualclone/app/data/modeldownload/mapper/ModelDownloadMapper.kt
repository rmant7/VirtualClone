package com.virtualclone.app.data.modeldownload.mapper

import com.virtualclone.app.core.domain.model.DownloadStatus
import com.virtualclone.app.core.domain.model.ModelDownloadInfo
import com.virtualclone.app.data.db.entity.ModelDownloadEntity
import com.virtualclone.app.data.modeldownload.datasource.ResumableDownloadManager

object ModelDownloadMapper {

    fun fromEntity(entity: ModelDownloadEntity): ModelDownloadInfo =
        ModelDownloadInfo(
            modelId = entity.modelName,
            bytesDownloaded = entity.downloadedBytes,
            totalBytes = entity.totalBytes,
            status = when (entity.status.uppercase()) {
                "DOWNLOADING" -> DownloadStatus.DOWNLOADING
                "PAUSED" -> DownloadStatus.PAUSED
                "COMPLETED" -> DownloadStatus.COMPLETED
                "FAILED" -> DownloadStatus.FAILED
                "DELETED", "CANCELED" -> DownloadStatus.DELETED
                else -> DownloadStatus.NOT_STARTED
            }
        )

    fun fromProgress(p: ResumableDownloadManager.Progress): ModelDownloadInfo =
        ModelDownloadInfo(
            modelId = p.modelName,
            bytesDownloaded = p.downloaded,
            totalBytes = p.total,
            status = when (p.status.uppercase()) {
                "DOWNLOADING" -> DownloadStatus.DOWNLOADING
                "PAUSED" -> DownloadStatus.PAUSED
                "COMPLETED" -> DownloadStatus.COMPLETED
                "FAILED" -> DownloadStatus.FAILED
                "CANCELED" -> DownloadStatus.DELETED
                else -> DownloadStatus.NOT_STARTED
            }
        )
}