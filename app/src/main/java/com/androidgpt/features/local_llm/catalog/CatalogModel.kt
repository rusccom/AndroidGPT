package com.androidgpt.features.local_llm.catalog

import kotlinx.serialization.Serializable

@Serializable
data class ModelCatalogDto(
    val version: Int,
    val updated: String,
    val models: List<CatalogModelDto>,
)

@Serializable
data class CatalogModelDto(
    val id: String,
    val name: String,
    val url: String,
    val sha256: String? = null,
    val size_bytes: Long,
    val context: Int,
    val ram_min_mb: Int = 1024,
    val languages: List<String> = emptyList(),
    val use_case: String = "chat",
    val recommended: Boolean = false,
)

data class CatalogModel(
    val id: String,
    val name: String,
    val url: String,
    val sha256: String?,
    val sizeBytes: Long,
    val context: Int,
    val ramMinMb: Int,
    val languages: List<String>,
    val useCase: String,
    val recommended: Boolean,
) {
    companion object {
        fun from(dto: CatalogModelDto) = CatalogModel(
            id = dto.id, name = dto.name, url = dto.url, sha256 = dto.sha256,
            sizeBytes = dto.size_bytes, context = dto.context, ramMinMb = dto.ram_min_mb,
            languages = dto.languages, useCase = dto.use_case, recommended = dto.recommended,
        )
    }
}
