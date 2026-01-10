package com.virtualclone.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val uri: String,
    val size: Long,
    val pageCount: Int,
    val importedAt: Date
)
