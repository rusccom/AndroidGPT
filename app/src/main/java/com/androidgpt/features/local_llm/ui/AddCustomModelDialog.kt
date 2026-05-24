package com.androidgpt.features.local_llm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

@Composable
fun AddCustomModelDialog(
    onDismiss: () -> Unit,
    onSubmit: (name: String, url: String, sha256: String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var sha by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить модель по URL") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") })
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL на .gguf") })
                OutlinedTextField(value = sha, onValueChange = { sha = it }, label = { Text("SHA256 (опционально)") })
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && url.endsWith(".gguf", ignoreCase = true),
                onClick = { onSubmit(name.trim(), url.trim(), sha.trim().ifBlank { null }) },
            ) { Text("Скачать") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
