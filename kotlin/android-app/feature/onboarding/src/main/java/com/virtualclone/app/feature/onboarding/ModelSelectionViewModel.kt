package com.virtualclone.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.virtualclone.app.core.common.Result
import com.virtualclone.app.core.common.model.ModelUi
import com.virtualclone.app.core.domain.repository.ModelRepository
import com.virtualclone.app.core.domain.usecase.model.GetAvailableModelsUseCase
import com.virtualclone.app.core.domain.usecase.model.GetDownloadedModelsUseCase
import com.virtualclone.app.feature.onboarding.mapper.ModelUiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.virtualclone.app.core.common.Logger

data class ModelSelectionUiState(
    val downloadedModels: List<ModelUi> = emptyList(),
    val selectedModel: ModelUi? = null,
    val showSnackbar: Boolean = false,
    val snackbarMessage: String = ""
)

@HiltViewModel
class ModelSelectionViewModel @Inject constructor(
    private val getAvailableModels: GetAvailableModelsUseCase,
    private val getDownloadedModels: GetDownloadedModelsUseCase,
    private val modelRepository: ModelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelSelectionUiState())
    val uiState: StateFlow<ModelSelectionUiState> = _uiState.asStateFlow()

    private val tag = ModelSelectionViewModel::class.java.simpleName

    // -------------------------------------------------------------------------
    // Load models
    // -------------------------------------------------------------------------
    fun loadModels() = viewModelScope.launch {
        when (val result = getAvailableModels()) {
            is Result.Success -> {
                val domainModels = result.data
                val ids = domainModels.map { it.id }

                when (val downloaded = getDownloadedModels(ids)) {
                    is Result.Success -> {
                        val validIds = downloaded.data
                        val filtered = domainModels.filter { it.id in validIds }
                        val uiModels = ModelUiMapper.toUiList(filtered)

                        _uiState.update {
                            it.copy(downloadedModels = uiModels)
                        }

                        Logger.i("Loaded ${uiModels.size} models", tag)
                    }

                    is Result.Error -> {
                        Logger.e("Validation failed", downloaded.exception, tag)
                        showError("Failed to validate models") // I am seeing this when internet is OFF. Why? Models are downloaded are visible on Model Download screen without internet, but when I go to model selection screen I see this error
                    }

                    Result.Loading -> Unit
                }
            }

            is Result.Error -> {
                Logger.e("Failed to load models", result.exception, tag)
                showError("Failed to load models")
            }

            Result.Loading -> Unit
        }
    }

    // -------------------------------------------------------------------------
    // Model selection (🔥 NO initialization here)
    // -------------------------------------------------------------------------
    fun selectModel(
        model: ModelUi,
        onSelected: (ModelUi, String) -> Unit
    ) {
        val conversationId = model.id

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedModel = model,
                    showSnackbar = true,
                    snackbarMessage = "Loading model…"
                )
            }

            // 🔑 Only mark active + navigate
            modelRepository.setActiveModel(model.id)

            onSelected(model, conversationId)

            _uiState.update {
                it.copy(
                    selectedModel = null,
                    showSnackbar = false,
                    snackbarMessage = ""
                )
            }
        }
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(showSnackbar = false) }
    }

    private fun showError(message: String) {
        _uiState.update {
            it.copy(showSnackbar = true, snackbarMessage = message)
        }
    }
}