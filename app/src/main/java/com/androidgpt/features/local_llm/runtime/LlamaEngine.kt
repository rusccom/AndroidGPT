package com.androidgpt.features.local_llm.runtime

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Контракт нативного inference-движка.
 *
 * Текущая реализация — заглушка. Чтобы включить реальный inference:
 *   1) Добавить зависимость llama.cpp Android, например:
 *      implementation("com.github.<author>:llama.android:<version>")
 *      или собрать локально из github.com/ggerganov/llama.cpp/tree/master/examples/llama.android
 *   2) Заменить StubLlamaEngine на JNI-обёртку (llama_init, llama_decode и т.д.)
 *   3) Метаданные модели (архитектура, контекст) llama.cpp считает сам из GGUF.
 */
interface LlamaEngine {
    suspend fun load(modelPath: String, config: LlamaConfig)
    suspend fun unload()
    fun isLoaded(): Boolean
    fun generate(prompt: String, stop: List<String> = emptyList()): Flow<String>
    suspend fun complete(prompt: String, stop: List<String> = emptyList()): String
}

class StubLlamaEngine : LlamaEngine {
    @Volatile private var loaded: Boolean = false
    @Volatile private var path: String? = null

    override suspend fun load(modelPath: String, config: LlamaConfig) {
        path = modelPath
        loaded = true
    }

    override suspend fun unload() {
        loaded = false; path = null
    }

    override fun isLoaded(): Boolean = loaded

    override fun generate(prompt: String, stop: List<String>): Flow<String> = flow {
        emit(stubResponse(prompt))
    }

    override suspend fun complete(prompt: String, stop: List<String>): String = stubResponse(prompt)

    private fun stubResponse(prompt: String): String {
        val text = prompt.lowercase()
        val tool = when {
            "будильник" in text || "разбуди" in text -> "set_alarm"
            "таймер" in text -> "set_timer"
            "видео" in text || "включи фильм" in text || "ютуб" in text -> "play_video"
            "свет" in text || "лампу" in text || "розетку" in text -> "smart_home"
            "позвони" in text -> "telegram_call"
            "эксперт" in text -> "expert_mode"
            else -> "chat"
        }
        return """{"tool":"$tool","args":{"raw":"${prompt.replace("\"", "'")}"}}"""
    }
}
