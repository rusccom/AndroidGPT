package com.androidgpt.features.assistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidgpt.features.assistant.AssistantPhase
import com.androidgpt.features.assistant.AssistantUi
import com.androidgpt.features.tools.ToolResult

@Composable
fun AssistantScreen(onOpenExpert: () -> Unit, vm: AssistantViewModel = hiltViewModel()) {
    val state by vm.ui.collectAsStateWithLifecycle()

    LaunchedEffect(state.pendingAction) {
        if (state.pendingAction is ToolResult.NextAction.StartExpert) {
            onOpenExpert(); vm.clearAction()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Ассистент", style = MaterialTheme.typography.headlineMedium)
        PhaseCard(state)
        if (state.lastUser.isNotBlank()) DialogLine("Вы", state.lastUser)
        if (state.lastReply.isNotBlank()) DialogLine("Я", state.lastReply)
        if (state.error.isNotBlank()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    state.error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun PhaseCard(state: AssistantUi) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.phase == AssistantPhase.MODEL_LOADING ||
                    state.phase == AssistantPhase.THINKING
                ) CircularProgressIndicator()
                Text(phaseTitle(state.phase), style = MaterialTheme.typography.titleLarge)
                if (state.partial.isNotBlank()) Text(state.partial, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun DialogLine(who: String, text: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text("$who: $text", modifier = Modifier.padding(12.dp))
    }
}

private fun phaseTitle(phase: AssistantPhase): String = when (phase) {
    AssistantPhase.IDLE -> "Инициализация…"
    AssistantPhase.MODEL_LOADING -> "Загружаю модель распознавания…"
    AssistantPhase.MODEL_MISSING -> "Модель не установлена"
    AssistantPhase.WAITING_WAKE -> "Скажи «Sam»"
    AssistantPhase.LISTENING -> "Слушаю команду…"
    AssistantPhase.THINKING -> "Думаю…"
    AssistantPhase.REPLYING -> "Отвечаю…"
    AssistantPhase.ERROR -> "Ошибка"
}
