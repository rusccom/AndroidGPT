package com.androidgpt.features.local_llm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.androidgpt.features.local_llm.catalog.CatalogModel
import com.androidgpt.features.local_llm.download.DownloadProgress
import com.androidgpt.features.local_llm.storage.InstalledModel

@Composable
fun CatalogCard(
    model: CatalogModel,
    progress: DownloadProgress?,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(model.name, style = MaterialTheme.typography.titleMedium)
                if (model.recommended) AssistChip(onClick = {}, label = { Text("реком.") })
            }
            Text(
                "ctx ${model.context} · ${formatSize(model.sizeBytes)} · ${model.languages.joinToString()}",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (progress != null && !progress.finished) {
                LinearProgressIndicator(
                    progress = { progress.percent / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${progress.percent}% · ${progress.state}", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onCancel) { Text("Отмена") }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDownload) { Text("Скачать") }
                    if (progress?.state == DownloadProgress.State.ERROR) {
                        Text(progress.error ?: "Ошибка", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun InstalledCard(
    model: InstalledModel,
    isActive: Boolean,
    onSetActive: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(model.name, style = MaterialTheme.typography.titleMedium)
                if (isActive) AssistChip(onClick = {}, label = { Text("активна") })
            }
            Text(
                "ctx ${model.context} · ${formatSize(model.sizeBytes)} · ${model.source}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isActive) TextButton(onClick = onSetActive) { Text("Выбрать") }
                TextButton(onClick = onDelete) { Text("Удалить") }
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "${bytes} B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var v = bytes.toDouble() / 1024.0
    var i = 0
    while (v >= 1024 && i < units.lastIndex) { v /= 1024.0; i++ }
    return "%.1f %s".format(v, units[i])
}
