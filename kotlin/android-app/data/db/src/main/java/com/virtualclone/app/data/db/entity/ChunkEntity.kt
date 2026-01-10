package com.virtualclone.app.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chunks",
    indices = [Index(value = ["docId"])]
)
data class ChunkEntity(
    @PrimaryKey(autoGenerate = true)
    val rowid: Long = 0,
    val uuid: String,
    val docId: String,
    val idx: Int,
    val text: String
)
