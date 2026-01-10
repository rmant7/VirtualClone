package com.virtualclone.app.core.common

sealed class UiEvent {
    data class ShowError(val error: String) : UiEvent()
    data class ShowMessage(val message: String) : UiEvent()
}
