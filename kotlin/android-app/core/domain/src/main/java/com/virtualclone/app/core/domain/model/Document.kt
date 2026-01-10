package com.virtualclone.app.core.domain.model

data class Document(
    val id: String,
    val name: String,
    val size: Long,
    val pageCount: Int
)