package com.androidgpt.features.tools.telegram_call

import com.androidgpt.features.telegram.TelegramCallManager
import com.androidgpt.features.telegram.TelegramContacts
import com.androidgpt.features.tools.Tool
import com.androidgpt.features.tools.ToolResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

class TelegramCallTool @Inject constructor(
    private val contacts: TelegramContacts,
    private val calls: TelegramCallManager,
) : Tool {
    override val name = "telegram_call"
    override val description = "Позвонить в Telegram. args: {contact: string, with_video?: bool}"

    override suspend fun execute(args: JsonObject): ToolResult {
        val name = args["contact"]?.jsonPrimitive?.content
            ?: return ToolResult("Кому звонить?", ok = false)
        val withVideo = args["with_video"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
        val match = contacts.search(name).firstOrNull()
            ?: return ToolResult("Не нашёл контакт «$name» в Telegram", ok = false)
        return runCatching {
            calls.startCall(match.userId, withVideo)
            ToolResult(
                reply = "Звоню ${match.displayName}${if (withVideo) " (видео)" else ""}",
                nextAction = ToolResult.NextAction.StartCall(match.displayName, withVideo),
            )
        }.getOrElse { ToolResult("Ошибка звонка: ${it.message}", ok = false) }
    }
}
