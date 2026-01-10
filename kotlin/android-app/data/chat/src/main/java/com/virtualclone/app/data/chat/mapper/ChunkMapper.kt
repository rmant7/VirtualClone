package com.virtualclone.app.data.chat.mapper

import com.virtualclone.app.core.domain.model.DocumentChunk
import com.virtualclone.app.data.db.entity.ChunkEntity

object ChunkMapper {

    fun toDomain(entity: ChunkEntity): DocumentChunk =
        DocumentChunk(
            id = entity.uuid,
            index = entity.idx,
            text = entity.text
        )
}