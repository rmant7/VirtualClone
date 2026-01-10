package com.virtualclone.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "model_downloads")
data class ModelDownloadEntity(
    @PrimaryKey val modelName: String,
    @ColumnInfo val url: String,
    @ColumnInfo val fileName: String,
    @ColumnInfo val totalBytes: Long = -1L,
    @ColumnInfo val downloadedBytes: Long = 0L,
    @ColumnInfo val status: String = "PENDING",
    @ColumnInfo val checksumSha256: String? = null,
    @ColumnInfo val lastUpdated: Long = System.currentTimeMillis()
)
