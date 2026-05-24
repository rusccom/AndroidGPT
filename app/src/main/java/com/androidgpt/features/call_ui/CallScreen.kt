package com.androidgpt.features.call_ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidgpt.features.telegram.CallState

@Composable
fun CallScreen(vm: CallViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label(s), style = MaterialTheme.typography.headlineMedium)
            when (s) {
                is CallState.Incoming -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = vm::accept) { Text("Принять") }
                    Button(onClick = vm::decline) { Text("Отклонить") }
                }
                is CallState.Outgoing, is CallState.Active ->
                    Button(onClick = vm::hangup) { Text("Завершить") }
                else -> Text("Нет активного звонка")
            }
        }
    }
}

private fun label(s: CallState): String = when (s) {
    is CallState.Outgoing -> "Исходящий${if (s.withVideo) " (видео)" else ""}"
    is CallState.Incoming -> "Входящий${if (s.withVideo) " (видео)" else ""}"
    is CallState.Active -> "Идёт звонок"
    is CallState.Ended -> "Завершён: ${s.reason}"
    CallState.Idle -> "Ожидание"
}
