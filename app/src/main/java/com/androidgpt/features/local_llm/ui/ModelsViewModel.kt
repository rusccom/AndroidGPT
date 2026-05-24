package com.androidgpt.features.local_llm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidgpt.features.local_llm.ModelManager
import com.androidgpt.features.local_llm.catalog.CatalogModel
import com.androidgpt.features.local_llm.download.DownloadProgress
import com.androidgpt.features.local_llm.storage.InstalledModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ModelsViewModel @Inject constructor(
    private val manager: ModelManager,
) : ViewModel() {

    data class UiState(
        val installed: List<InstalledModel> = emptyList(),
        val catalog: List<CatalogModel> = emptyList(),
        val active: InstalledModel? = null,
        val downloads: Map<String, DownloadProgress> = emptyMap(),
    )

    val ui: StateFlow<UiState> = combine(
        manager.installed, manager.catalog, manager.active, manager.downloads,
    ) { installed, catalog, active, downloads ->
        UiState(installed, catalog, active, downloads)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun download(m: CatalogModel) = manager.download(m)
    fun downloadCustom(name: String, url: String, sha256: String?) = viewModelScope.launch {
        val id = "custom-${UUID.randomUUID().toString().take(8)}"
        manager.downloadCustom(id, name, url, sha256)
    }
    fun cancel(id: String) = manager.cancel(id)
    fun delete(id: String) = manager.delete(id)
    fun setActive(id: String) = viewModelScope.launch { manager.setActive(id) }
    fun refresh() = viewModelScope.launch { manager.refreshCatalog() }
}
