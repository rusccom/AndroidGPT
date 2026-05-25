package com.androidgpt.features.assistant.ui

import androidx.lifecycle.ViewModel
import com.androidgpt.features.assistant.AssistantBus
import com.androidgpt.features.assistant.AssistantUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val bus: AssistantBus,
) : ViewModel() {

    val ui: StateFlow<AssistantUi> = bus.state

    fun clearAction() = bus.clearAction()
}
