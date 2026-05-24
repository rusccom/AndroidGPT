package com.androidgpt.features.assistant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidgpt.features.assistant.AssistantOrchestrator
import com.androidgpt.features.tools.ToolResult
import com.androidgpt.features.voice.LocalStt
import com.androidgpt.features.voice.SttEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val orchestrator: AssistantOrchestrator,
    private val stt: LocalStt,
) : ViewModel() {

    data class UiState(
        val listening: Boolean = false,
        val partial: String = "",
        val lastUser: String = "",
        val lastReply: String = "",
        val pendingAction: ToolResult.NextAction = ToolResult.NextAction.None,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var listenJob: Job? = null

    fun startListening() {
        if (_ui.value.listening) return
        _ui.value = _ui.value.copy(listening = true, partial = "")
        listenJob = viewModelScope.launch {
            stt.listen().collect { event -> onSttEvent(event) }
        }
    }

    fun stopListening() {
        stt.stop()
        listenJob?.cancel()
        _ui.value = _ui.value.copy(listening = false)
    }

    fun submit(text: String) = viewModelScope.launch {
        _ui.value = _ui.value.copy(lastUser = text)
        val result = orchestrator.handle(text)
        _ui.value = _ui.value.copy(lastReply = result.reply, pendingAction = result.nextAction)
    }

    fun clearAction() { _ui.value = _ui.value.copy(pendingAction = ToolResult.NextAction.None) }

    private fun onSttEvent(event: SttEvent) {
        when (event) {
            is SttEvent.Partial -> _ui.value = _ui.value.copy(partial = event.text)
            is SttEvent.Final -> { _ui.value = _ui.value.copy(listening = false, partial = ""); submit(event.text) }
            is SttEvent.Error -> _ui.value = _ui.value.copy(listening = false, lastReply = event.message)
            SttEvent.EndOfSpeech -> _ui.value = _ui.value.copy(listening = false)
        }
    }
}
