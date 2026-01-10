package com.virtualclone.app.data.chat.mapper

import com.virtualclone.app.core.domain.model.Document
import com.virtualclone.app.data.db.entity.DocumentEntity

object DocumentMapper {

    fun toDomain(entity: DocumentEntity): Document =
        Document(
            id = entity.id,
            name = entity.name,
            size = entity.size,
            pageCount = entity.pageCount
        )
}
