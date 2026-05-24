package com.androidgpt.features.local_llm.storage

enum class ModelSource { CATALOG, CUSTOM_URL, IMPORTED_FILE }

data class InstalledModel(
    val id: String,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val context: Int,
    val source: ModelSource,
    val sourceUrl: String? = null,
)
