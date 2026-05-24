package com.androidgpt.features.local_llm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ModelsScreen(vm: ModelsViewModel = hiltViewModel()) {
    val state by vm.ui.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Модели", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { vm.refresh() }) { Icon(Icons.Outlined.Refresh, contentDescription = "Обновить") }
                IconButton(onClick = { showDialog = true }) { Icon(Icons.Outlined.Add, contentDescription = "Добавить") }
            }
        }
        if (state.installed.isNotEmpty()) {
            item { SectionTitle("Установленные") }
            items(state.installed, key = { it.id }) { m ->
                InstalledCard(
                    model = m,
                    isActive = state.active?.id == m.id,
                    onSetActive = { vm.setActive(m.id) },
                    onDelete = { vm.delete(m.id) },
                )
            }
        }
        item { SectionTitle("Каталог") }
        items(state.catalog, key = { it.id }) { m ->
            CatalogCard(
                model = m,
                progress = state.downloads[m.id],
                onDownload = { vm.download(m) },
                onCancel = { vm.cancel(m.id) },
            )
        }
    }

    if (showDialog) {
        AddCustomModelDialog(
            onDismiss = { showDialog = false },
            onSubmit = { n, u, s -> vm.downloadCustom(n, u, s); showDialog = false },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge)
}
