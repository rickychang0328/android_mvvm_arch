package com.example.android_mvvm_arch.feature.profile.presentation.state

sealed interface ProfileIntent {
    data object Refresh : ProfileIntent
    data object StartEditing : ProfileIntent
    data object CancelEditing : ProfileIntent
    data class DisplayNameChanged(val value: String) : ProfileIntent
    data class PhoneChanged(val value: String) : ProfileIntent
    data class BioChanged(val value: String) : ProfileIntent
    data object SaveProfile : ProfileIntent
    data object Logout : ProfileIntent
}
