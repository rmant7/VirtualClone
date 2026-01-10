package com.virtualclone.app.feature.onboarding

import com.virtualclone.app.core.common.model.ModelUi
import com.virtualclone.app.feature.onboarding.ui.ModelDownloadUiItem

sealed class DownloadUiState {
    object Idle : DownloadUiState()

    /**
     * Loaded = working state for the screen.
     * - userSelected: set of selected model ids
     * - allModels: list of ModelUi shown in UI (order preserved)
     * - statusMap: map from modelId -> status item
     */
    data class Loaded(
        val userSelected: Set<String>,
        val allModels: List<ModelUi>,
        val statusMap: Map<String, ModelDownloadUiItem>,
        val completed: Set<String>,
        val showSnackbar: Boolean = false,
        val snackbarMessage: String = ""
    ) : DownloadUiState()
}