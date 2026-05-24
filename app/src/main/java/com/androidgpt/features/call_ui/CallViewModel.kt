package com.androidgpt.features.call_ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidgpt.features.telegram.CallState
import com.androidgpt.features.telegram.TelegramCallManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val manager: TelegramCallManager,
) : ViewModel() {

    val state: StateFlow<CallState> = manager.state

    fun accept() = viewModelScope.launch { manager.accept() }
    fun decline() = viewModelScope.launch { manager.decline() }
    fun hangup() = viewModelScope.launch { manager.hangup() }
}
