package com.androidgpt.features.assistant

import com.androidgpt.features.tools.ToolRegistry
import com.androidgpt.features.tools.ToolResult
import com.androidgpt.features.voice.LocalTts
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssistantOrchestrator @Inject constructor(
    private val router: IntentRouter,
    private val tools: ToolRegistry,
    private val tts: LocalTts,
) {
    suspend fun handle(text: String, speak: Boolean = true): ToolResult {
        if (text.isBlank()) return ToolResult("", ok = false)
        val intent = router.route(text)
        val tool = tools.find(intent.tool) ?: tools.fallback()
        val result = runCatching { tool.execute(intent.args) }
            .getOrElse { ToolResult("Ошибка: ${it.message}", ok = false) }
        if (speak && result.reply.isNotBlank()) tts.speak(result.reply)
        return result
    }
}
