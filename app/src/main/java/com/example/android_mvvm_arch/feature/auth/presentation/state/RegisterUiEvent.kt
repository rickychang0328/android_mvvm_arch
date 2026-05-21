package com.example.android_mvvm_arch.feature.auth.presentation.state

sealed interface RegisterUiEvent {
    data object NavigateToProfile : RegisterUiEvent
    data class ShowMessage(val message: String) : RegisterUiEvent
}
