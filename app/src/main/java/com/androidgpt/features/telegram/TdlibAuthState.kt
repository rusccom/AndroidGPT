package com.androidgpt.features.telegram

sealed interface TdlibAuthState {
    data object Idle : TdlibAuthState
    data object Initializing : TdlibAuthState
    data object MissingApiCredentials : TdlibAuthState
    data class WaitQrScan(val link: String) : TdlibAuthState
    data object WaitPassword : TdlibAuthState
    data object Ready : TdlibAuthState
    data object LoggedOut : TdlibAuthState
    data class Error(val message: String) : TdlibAuthState
}
