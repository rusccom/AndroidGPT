package com.androidgpt.features.telegram.ui

import androidx.lifecycle.ViewModel
import com.androidgpt.features.telegram.TdlibAuthState
import com.androidgpt.features.telegram.TdlibClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class TelegramAuthViewModel @Inject constructor(
    private val client: TdlibClient,
) : ViewModel() {

    val auth: StateFlow<TdlibAuthState> = client.auth

    fun connect() = client.start()
    fun disconnect() = client.stop()
    fun submitPassword(password: String) = client.submitPassword(password)
}
