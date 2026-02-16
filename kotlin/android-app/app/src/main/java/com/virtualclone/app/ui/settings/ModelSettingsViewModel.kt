package com.virtualclone.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelSettingsViewModel @Inject constructor(
    private val repository: ModelSettingsRepository
) : ViewModel() {
    
    val settings: StateFlow<ModelSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ModelSettings()
    )
    
    fun updateMaxTokens(value: Int) {
        viewModelScope.launch {
            repository.updateMaxTokens(value)
        }
    }
    
    fun updateTemperature(value: Float) {
        viewModelScope.launch {
            repository.updateTemperature(value)
        }
    }
    
    fun updateSummarizationModel(value: String) {
        viewModelScope.launch {
            repository.updateSummarizationModel(value)
        }
    }
    
    fun updateQaModel(value: String) {
        viewModelScope.launch {
            repository.updateQaModel(value)
        }
    }
}
