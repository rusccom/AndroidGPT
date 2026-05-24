package com.androidgpt.features.local_llm.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelStorage @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val json: Json,
) {
    private val root: File = File(ctx.filesDir, "models").apply { mkdirs() }
    private val index: File = File(root, "index.json")

    private val _models = MutableStateFlow(loadIndex())
    val models: StateFlow<List<InstalledModel>> = _models.asStateFlow()

    fun finalFile(id: String): File = File(root, "$id.gguf")
    fun tempFile(id: String): File = File(root, "$id.gguf.part")

    fun add(model: InstalledModel) {
        val list = _models.value.filterNot { it.id == model.id } + model
        persist(list)
    }

    fun remove(id: String): Boolean {
        val list = _models.value.filterNot { it.id == id }
        finalFile(id).delete()
        tempFile(id).delete()
        persist(list)
        return list.size != _models.value.size
    }

    fun byId(id: String): InstalledModel? = _models.value.firstOrNull { it.id == id }

    private fun persist(list: List<InstalledModel>) {
        _models.value = list
        runCatching {
            val dtos = list.map { InstalledModelDto.from(it) }
            index.writeText(json.encodeToString(InstalledModelIndex.serializer(), InstalledModelIndex(dtos)))
        }
    }

    private fun loadIndex(): List<InstalledModel> = runCatching {
        if (!index.exists()) return@runCatching emptyList()
        json.decodeFromString(InstalledModelIndex.serializer(), index.readText())
            .items.map { it.toDomain() }
            .filter { File(it.path).exists() }
    }.getOrDefault(emptyList())
}

@Serializable
private data class InstalledModelIndex(val items: List<InstalledModelDto>)

@Serializable
private data class InstalledModelDto(
    val id: String,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val context: Int,
    val source: String,
    val sourceUrl: String? = null,
) {
    fun toDomain() = InstalledModel(
        id, name, path, sizeBytes, context, ModelSource.valueOf(source), sourceUrl,
    )
    companion object {
        fun from(m: InstalledModel) = InstalledModelDto(
            m.id, m.name, m.path, m.sizeBytes, m.context, m.source.name, m.sourceUrl,
        )
    }
}
