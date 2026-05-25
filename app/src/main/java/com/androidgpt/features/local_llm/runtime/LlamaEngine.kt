package com.androidgpt.features.local_llm.runtime

import kotlinx.coroutines.flow.Flow

/**
 * Контракт нативного inference-движка.
 * Текущая имплементация: [LlamaCppEngine] поверх io.github.ljcamargo:llamacpp-kotlin.
 */
interface LlamaEngine {
    suspend fun load(modelPath: String, config: LlamaConfig)
    suspend fun unload()
    fun isLoaded(): Boolean
    fun generate(prompt: String, stop: List<String> = emptyList()): Flow<String>
    suspend fun complete(prompt: String, stop: List<String> = emptyList()): String
}
