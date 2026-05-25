package com.androidgpt.features.assistant

import com.androidgpt.features.tools.ToolRegistry
import com.androidgpt.features.tools.ToolResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssistantOrchestrator @Inject constructor(
    private val router: IntentRouter,
    private val tools: ToolRegistry,
) {
    suspend fun handle(text: String): ToolResult {
        if (text.isBlank()) return ToolResult("", ok = false)
        val intent = router.route(text)
        val tool = tools.find(intent.tool) ?: tools.fallback()
        return runCatching { tool.execute(intent.args) }
            .getOrElse { ToolResult("Ошибка: ${it.message}", ok = false) }
    }
}
