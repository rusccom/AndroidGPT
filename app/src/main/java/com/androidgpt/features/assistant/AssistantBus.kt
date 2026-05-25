package com.androidgpt.features.assistant

import com.androidgpt.features.tools.ToolResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class AssistantPhase {
    IDLE,
    MODEL_LOADING,
    MODEL_MISSING,
    WAITING_WAKE,
    LISTENING,
    THINKING,
    REPLYING,
    ERROR,
}

data class AssistantUi(
    val phase: AssistantPhase = AssistantPhase.IDLE,
    val partial: String = "",
    val lastUser: String = "",
    val lastReply: String = "",
    val error: String = "",
    val pendingAction: ToolResult.NextAction = ToolResult.NextAction.None,
)

@Singleton
class AssistantBus @Inject constructor() {

    private val _state = MutableStateFlow(AssistantUi())
    val state: StateFlow<AssistantUi> = _state.asStateFlow()

    fun setPhase(phase: AssistantPhase) {
        _state.value = _state.value.copy(phase = phase, partial = "")
    }

    fun setError(message: String) {
        _state.value = _state.value.copy(phase = AssistantPhase.ERROR, error = message)
    }

    fun setPartial(text: String) {
        _state.value = _state.value.copy(partial = text)
    }

    fun setHeard(text: String) {
        _state.value = _state.value.copy(lastUser = text, partial = "")
    }

    fun setReply(text: String, action: ToolResult.NextAction) {
        _state.value = _state.value.copy(lastReply = text, pendingAction = action)
    }

    fun clearAction() {
        _state.value = _state.value.copy(pendingAction = ToolResult.NextAction.None)
    }
}
