package com.virtualclone.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Fts4(contentEntity = ChunkEntity::class)
@Entity(tableName = "chunks_fts")
data class ChunkFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Int,
    val text: String
)
