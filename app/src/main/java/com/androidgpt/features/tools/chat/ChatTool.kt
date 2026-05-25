package com.androidgpt.features.tools.chat

import com.androidgpt.features.local_llm.runtime.LlamaSession
import com.androidgpt.features.tools.Tool
import com.androidgpt.features.tools.ToolResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

class ChatTool @Inject constructor(
    private val session: LlamaSession,
    private val history: DialogHistory,
) : Tool {
    override val name = "chat"
    override val description = "Ответить пользователю коротким диалогом. args: {raw: string}"

    override suspend fun execute(args: JsonObject): ToolResult {
        val text = args["raw"]?.jsonPrimitive?.content.orEmpty()
        if (text.isBlank()) return ToolResult("Не услышал команду.", ok = false)
        if (!session.engine().isLoaded()) {
            return ToolResult(
                "Локальная модель не загружена. Скачай и активируй модель в разделе «Модели».",
                ok = false,
            )
        }
        val prompt = buildPrompt(text, history.snapshot())
        val reply = runCatching { session.engine().complete(prompt) }
            .getOrElse { return ToolResult("Не получилось ответить: ${it.message}", ok = false) }
        val cleaned = trim(reply).ifBlank { "Не знаю, что ответить." }
        history.add(DialogTurn(text, cleaned))
        return ToolResult(cleaned)
    }

    private fun buildPrompt(userText: String, past: List<DialogTurn>): String = buildString {
        append("Ты — голосовой ассистент. Отвечай на русском, одним-двумя короткими предложениями, без вступлений.\n\n")
        past.forEach {
            append("Пользователь: ${it.user}\n")
            append("Ассистент: ${it.assistant}\n")
        }
        append("Пользователь: $userText\nАссистент:")
    }

    private fun trim(raw: String): String {
        var s = raw.trim()
        listOf("Пользователь:", "Ассистент:", "\n\n").forEach { marker ->
            val idx = s.indexOf(marker)
            if (idx > 0) s = s.substring(0, idx).trim()
        }
        return s.take(MAX_REPLY)
    }

    companion object { const val MAX_REPLY = 500 }
}
