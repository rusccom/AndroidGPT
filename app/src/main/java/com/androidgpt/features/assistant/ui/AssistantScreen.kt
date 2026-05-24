package com.androidgpt.features.assistant.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidgpt.features.tools.ToolResult

@Composable
fun AssistantScreen(onOpenExpert: () -> Unit, vm: AssistantViewModel = hiltViewModel()) {
    val state by vm.ui.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) vm.startListening() }

    LaunchedEffect(state.pendingAction) {
        when (val a = state.pendingAction) {
            is ToolResult.NextAction.StartExpert -> { onOpenExpert(); vm.clearAction() }
            else -> Unit
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Ассистент", style = MaterialTheme.typography.headlineMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (state.listening) "Слушаю: ${state.partial}" else "Готов",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (state.lastUser.isNotBlank()) Text("Вы: ${state.lastUser}")
                if (state.lastReply.isNotBlank()) Text("Я: ${state.lastReply}")
            }
        }

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Команда") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            IconButton(onClick = { vm.submit(input); input = "" }) {
                Icon(Icons.Outlined.Send, contentDescription = "Отправить")
            }
        }

        FloatingActionButton(
            onClick = {
                if (state.listening) vm.stopListening()
                else micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        ) { Icon(Icons.Outlined.Mic, contentDescription = "Микрофон") }

        Button(onClick = onOpenExpert) { Text("Открыть Эксперта") }
    }
}
