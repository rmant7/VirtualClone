package com.virtualclone.app.feature.llm.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.virtualclone.app.core.common.Result
import com.virtualclone.app.data.huggingface.HuggingFaceApiClient
import com.virtualclone.app.data.pdf.extractor.PdfTextExtractor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LlmUiState(
    val pdfName: String? = null,
    val pdfContent: String? = null,
    val isLoading: Boolean = false,
    val result: String? = null,
    val error: String? = null
)

@HiltViewModel
class LlmRunnerViewModel @Inject constructor(
    application: Application,
    private val huggingFaceClient: HuggingFaceApiClient
) : AndroidViewModel(application) {
    
    private val pdfExtractor = PdfTextExtractor(application)
    
    private val _uiState = MutableStateFlow(LlmUiState())
    val uiState: StateFlow<LlmUiState> = _uiState.asStateFlow()
    
    fun loadPdf(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val text = pdfExtractor.extractText(uri)
                val name = uri.lastPathSegment ?: "document.pdf"
                _uiState.value = _uiState.value.copy(
                    pdfName = name,
                    pdfContent = text,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load PDF: ${e.message}"
                )
            }
        }
    }
    
    fun summarize() {
        val content = _uiState.value.pdfContent ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, result = null)
            
            // HuggingFace has a token limit, so we truncate
            val truncatedContent = content.take(3000)
            
            when (val result = huggingFaceClient.summarize(truncatedContent)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        result = result.data
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exception.message ?: "Summarization failed"
                    )
                }
                else -> {}
            }
        }
    }
    
    fun askQuestion(question: String) {
        val content = _uiState.value.pdfContent ?: return
        if (question.isBlank()) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, result = null)
            
            // Truncate context for API limits
            val truncatedContent = content.take(3000)
            
            when (val result = huggingFaceClient.questionAnswering(truncatedContent, question)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        result = result.data
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exception.message ?: "Q&A failed"
                    )
                }
                else -> {}
            }
        }
    }
}
