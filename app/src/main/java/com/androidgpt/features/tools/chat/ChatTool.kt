package com.androidgpt.features.tools.chat

import com.androidgpt.features.local_llm.runtime.LlamaSession
import com.androidgpt.features.tools.Tool
import com.androidgpt.features.tools.ToolResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

class ChatTool @Inject constructor(
    private val session: LlamaSession,
) : Tool {
    override val name = "chat"
    override val description = "Ответить пользователю простым диалогом. args: {raw: string}"

    override suspend fun execute(args: JsonObject): ToolResult {
        val text = args["raw"]?.jsonPrimitive?.content ?: ""
        if (!session.engine().isLoaded()) {
            return ToolResult(
                reply = "Локальная модель не загружена. Скачай и выбери модель в разделе «Модели».",
            )
        }
        val prompt = """
            <s>[INST] Ответь коротко и по делу на русском. [/INST]
            Пользователь: $text
            Ассистент:
        """.trimIndent()
        val reply = runCatching { session.engine().complete(prompt) }
            .getOrElse { "Не получилось обработать: ${it.message}" }
        return ToolResult(reply = reply.ifBlank { "..." })
    }
}
