package com.androidgpt.features.local_llm.catalog

import android.content.Context
import com.androidgpt.features.settings.ApiKey
import com.androidgpt.features.settings.ApiKeysRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelCatalogRepository @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val http: HttpClient,
    private val keys: ApiKeysRepository,
    private val json: Json,
) {
    private val _state = MutableStateFlow<List<CatalogModel>>(emptyList())
    val catalog: StateFlow<List<CatalogModel>> = _state.asStateFlow()

    init { _state.value = readBundled() }

    suspend fun refresh(): Result<List<CatalogModel>> = runCatching {
        val url = keys.get(ApiKey.CatalogUrl).ifBlank { null }
            ?: return@runCatching readBundled().also { _state.value = it }
        val raw = http.get(url).bodyAsText()
        val parsed = json.decodeFromString(ModelCatalogDto.serializer(), raw)
        val mapped = parsed.models.map(CatalogModel::from)
        _state.value = mapped
        mapped
    }

    private fun readBundled(): List<CatalogModel> = runCatching {
        ctx.assets.open("models_catalog.json").use { s ->
            val text = s.readBytes().decodeToString()
            json.decodeFromString(ModelCatalogDto.serializer(), text)
                .models.map(CatalogModel::from)
        }
    }.getOrDefault(emptyList())
}
