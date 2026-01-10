package com.virtualclone.app.feature.docs.model

import com.virtualclone.app.data.pdf.model.PdfDocument

data class DocumentsUiState(
    val documents: List<PdfDocument> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isImporting: Boolean = false,
    val importProgress: Float = 0f
)
