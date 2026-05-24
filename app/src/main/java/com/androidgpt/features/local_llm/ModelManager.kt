package com.androidgpt.features.local_llm

import com.androidgpt.features.local_llm.catalog.CatalogModel
import com.androidgpt.features.local_llm.catalog.ModelCatalogRepository
import com.androidgpt.features.local_llm.download.DownloadProgress
import com.androidgpt.features.local_llm.download.ModelDownloader
import com.androidgpt.features.local_llm.runtime.LlamaSession
import com.androidgpt.features.local_llm.storage.InstalledModel
import com.androidgpt.features.local_llm.storage.ModelStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelManager @Inject constructor(
    private val catalogRepo: ModelCatalogRepository,
    private val storage: ModelStorage,
    private val downloader: ModelDownloader,
    private val session: LlamaSession,
) {
    val catalog: StateFlow<List<CatalogModel>> = catalogRepo.catalog
    val installed: StateFlow<List<InstalledModel>> = storage.models
    val active: StateFlow<InstalledModel?> = session.active
    val downloads: Flow<Map<String, DownloadProgress>> = downloader.progress

    suspend fun refreshCatalog() = catalogRepo.refresh()

    fun download(model: CatalogModel) = downloader.startCatalog(model)

    fun downloadCustom(id: String, name: String, url: String, sha256: String?) =
        downloader.startCustom(id, name, url, sha256)

    fun cancel(id: String) = downloader.cancel(id)

    fun delete(id: String): Boolean = storage.remove(id)

    suspend fun setActive(id: String) {
        val m = storage.byId(id) ?: return
        session.activate(m)
    }

    suspend fun warmUp() = session.activateActiveOrFirst()
}
