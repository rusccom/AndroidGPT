package com.androidgpt.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(onOpenAssistant: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("AndroidGPT", style = MaterialTheme.typography.headlineMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Готов к работе", style = MaterialTheme.typography.titleLarge)
                Text(
                    "1. Заполни ключи в Настройках\n" +
                        "2. Скачай LLM в разделе Модели\n" +
                        "3. Положи Vosk-модель в assets/model-ru\n" +
                        "4. Скажи «Sam» — и сразу команду\n" +
                        "5. «Sam, позвони эксперту» — Realtime",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Button(onClick = onOpenAssistant, modifier = Modifier.fillMaxWidth()) {
            Text("Открыть ассистента")
        }
    }
}
