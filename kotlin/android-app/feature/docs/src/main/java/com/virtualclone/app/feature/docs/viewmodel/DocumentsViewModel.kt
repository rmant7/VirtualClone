package com.virtualclone.app.feature.docs.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.virtualclone.app.core.common.getOrElse
import com.virtualclone.app.core.common.isError
import com.virtualclone.app.core.common.isSuccess
import com.virtualclone.app.data.pdf.model.PdfDocument
import com.virtualclone.app.data.pdf.repository.DocumentRepository
import com.virtualclone.app.feature.docs.model.DocumentsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DocumentsViewModel @Inject constructor(
    private val documentRepository: DocumentRepository
) : ViewModel() {
    private val tag = DocumentsViewModel::class.qualifiedName

    private val _uiState = MutableStateFlow(DocumentsUiState())
    val uiState: StateFlow<DocumentsUiState> = _uiState.asStateFlow()

    init {
        loadDocuments()
    }

    private fun loadDocuments() {
        viewModelScope.launch {
            documentRepository.getAllDocuments().collect { documents ->
                _uiState.value = _uiState.value.copy(
                    documents = documents,
                    isLoading = false
                )
            }
        }
    }

    fun importPdf(uri: android.net.Uri, name: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isImporting = true,
                    importProgress = 0f,
                    error = null
                )

                // Simulate progress updates
                val progressJob = launch {
                    repeat(10) { step ->
                        delay(200)
                        _uiState.value = _uiState.value.copy(
                            importProgress = (step + 1) * 0.1f
                        )
                    }
                }

                val result = documentRepository.importPdf(uri, name)
                progressJob.cancel()

                if (result.isSuccess()) {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        importProgress = 1f
                    )
                    Log.d(tag, "Successfully imported PDF: $name")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        importProgress = 0f,
                        error = "Failed to import PDF: ${result.getOrElse { "Unknown error" }}"
                    )
                }
            } catch (e: Exception) {
                Log.e(tag, "Error importing PDF", e)
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importProgress = 0f,
                    error = "Error importing PDF: ${e.message}"
                )
            }
        }
    }

    fun deleteDocument(document: PdfDocument) {
        viewModelScope.launch {
            try {
                val result = documentRepository.deleteDocument(document.id)
                if (result.isError()) {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to delete document"
                    )
                }
            } catch (e: Exception) {
                Log.e(tag, "Error deleting document", e)
                _uiState.value = _uiState.value.copy(
                    error = "Error deleting document: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
