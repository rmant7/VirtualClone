package com.virtualclone.app.data.pdf.model

import java.util.Date

data class PdfDocument(
    val id: String,
    val name: String,
    val uri: String,
    val size: Long,
    val pageCount: Int,
    val importedAt: Date,
    val chunks: List<TextChunk> = emptyList()
)

data class TextChunk(
    val id: String,
    val docId: String,
    val idx: Int,
    val text: String,
    val pageNumber: Int
)
