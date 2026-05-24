package com.androidgpt.features.tools.smart_home

import com.androidgpt.features.settings.ApiKey
import com.androidgpt.features.settings.ApiKeysRepository
import com.androidgpt.features.tools.Tool
import com.androidgpt.features.tools.ToolResult
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject

class SmartHomeTool @Inject constructor(
    private val http: HttpClient,
    private val keys: ApiKeysRepository,
) : Tool {
    override val name = "smart_home"
    override val description = "Home Assistant. args: {entity: 'light.kitchen', action: 'turn_on'|'turn_off'|'toggle'}"

    override suspend fun execute(args: JsonObject): ToolResult {
        val entity = args["entity"]?.jsonPrimitive?.content
            ?: return ToolResult("Не указан entity_id", ok = false)
        val action = args["action"]?.jsonPrimitive?.content
            ?: return ToolResult("Не указано действие", ok = false)
        val baseUrl = keys.get(ApiKey.HomeAssistantUrl).trimEnd('/')
        val token = keys.get(ApiKey.HomeAssistantToken)
        if (baseUrl.isBlank() || token.isBlank()) {
            return ToolResult("Заполни Home Assistant URL и Token в настройках", ok = false)
        }
        val domain = entity.substringBefore('.')
        return runCatching {
            val resp = http.post("$baseUrl/api/services/$domain/$action") {
                headers { append(HttpHeaders.Authorization, "Bearer $token") }
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject { put("entity_id", entity) }.toString())
            }
            ToolResult("Готово: $entity → $action (${resp.bodyAsText().take(40)})")
        }.getOrElse { ToolResult("Ошибка HA: ${it.message}", ok = false) }
    }
}
