package com.androidgpt.features.assistant

import com.androidgpt.features.local_llm.runtime.LlamaSession
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class IntentRouter @Inject constructor(
    private val session: LlamaSession,
    private val json: Json,
) {
    suspend fun route(userText: String): RoutedIntent {
        if (!session.engine().isLoaded()) return keywordRoute(userText)
        val raw = runCatching { session.engine().complete(buildPrompt(userText)) }.getOrDefault("")
        return parse(raw, userText) ?: keywordRoute(userText)
    }

    private fun keywordRoute(text: String): RoutedIntent {
        val t = text.lowercase()
        val tool = when {
            "будильник" in t || "разбуди" in t -> "set_alarm"
            "таймер" in t -> "set_timer"
            "видео" in t || "ютуб" in t -> "play_video"
            "свет" in t || "лампу" in t || "розетку" in t -> "smart_home"
            "позвони" in t && "эксперт" in t -> "expert_mode"
            "позвони" in t -> "telegram_call"
            "эксперт" in t -> "expert_mode"
            else -> "chat"
        }
        val args = buildJsonObject {
            put("raw", text)
            if (tool == "telegram_call") put("contact", text.substringAfter("позвони").trim().substringBefore(" "))
            if (tool == "play_video") put("query", text)
        }
        return RoutedIntent(tool, args)
    }

    private fun parse(raw: String, fallbackText: String): RoutedIntent? = runCatching {
        val cleaned = raw.substringAfter('{', "")
            .let { "{$it" }
            .substringBeforeLast('}', "")
            .let { "$it}" }
        if (cleaned.length < 5) return null
        val obj = json.parseToJsonElement(cleaned) as? JsonObject ?: return null
        val tool = (obj["tool"] ?: return null).toString().trim('"')
        val args = (obj["args"] as? JsonObject) ?: buildJsonObject { put("raw", fallbackText) }
        RoutedIntent(tool, args)
    }.getOrNull()

    private fun buildPrompt(userText: String): String = """
        Ты роутер команд. Отвечай ТОЛЬКО JSON, без пояснений:
        { "tool": "<name>", "args": {...} }

        Доступные tools:
        - set_alarm(time:"HH:mm", label?)
        - set_timer(seconds:number, label?)
        - play_video(query)
        - smart_home(entity, action)
        - telegram_call(contact, with_video?)
        - expert_mode(topic?)
        - chat(raw)

        Если не уверен — используй chat.

        Пользователь: $userText
        JSON:
    """.trimIndent()
}
