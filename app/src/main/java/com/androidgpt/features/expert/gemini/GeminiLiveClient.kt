package com.androidgpt.features.expert.gemini

import android.util.Base64
import com.androidgpt.features.expert.ExpertEvent
import com.androidgpt.features.expert.ExpertRealtimeClient
import com.androidgpt.features.settings.ApiKey
import com.androidgpt.features.settings.ApiKeysRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject

class GeminiLiveClient @Inject constructor(
    private val http: HttpClient,
    private val keys: ApiKeysRepository,
    private val json: Json,
) : ExpertRealtimeClient {

    override val sampleRate: Int = 16_000

    @Volatile private var session: DefaultClientWebSocketSession? = null
    private val audio = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)

    override suspend fun connect(): Flow<ExpertEvent> = channelFlow {
        val key = keys.get(ApiKey.Gemini)
        if (key.isBlank()) { trySend(ExpertEvent.Error("Нет Gemini ключа")); close(); return@channelFlow }
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$key"
        val ws = http.webSocketSession(url)
        session = ws
        ws.send(Frame.Text(setupFrame()))
        trySend(ExpertEvent.Connected)
        for (frame in ws.incoming) {
            if (frame !is Frame.Text) continue
            handleFrame(frame.readText())?.let { trySend(it) }
        }
        trySend(ExpertEvent.Disconnected)
    }

    private fun setupFrame(): String = buildJsonObject {
        put("setup", buildJsonObject {
            put("model", "models/gemini-2.0-flash-exp")
            put("generation_config", buildJsonObject {
                put("response_modalities", buildJsonArray { add("AUDIO") })
            })
        })
    }.toString()

    private fun handleFrame(payload: String): ExpertEvent? = runCatching {
        val root = json.parseToJsonElement(payload).jsonObject
        val serverContent = root["serverContent"]?.jsonObject ?: return null
        val parts = serverContent["modelTurn"]?.jsonObject?.get("parts")?.jsonArray
        parts?.forEach { p ->
            val obj = p.jsonObject
            obj["inlineData"]?.jsonObject?.get("data")?.jsonPrimitive?.content?.let {
                audio.tryEmit(Base64.decode(it, Base64.NO_WRAP))
            }
            obj["text"]?.jsonPrimitive?.content?.let {
                return ExpertEvent.Transcript(it, false, fromUser = false)
            }
        }
        null
    }.getOrNull()

    override suspend fun sendAudio(pcm16: ByteArray) {
        val b64 = Base64.encodeToString(pcm16, Base64.NO_WRAP)
        val frame = buildJsonObject {
            put("realtimeInput", buildJsonObject {
                put("mediaChunks", buildJsonArray {
                    add(buildJsonObject {
                        put("mimeType", "audio/pcm;rate=16000")
                        put("data", b64)
                    })
                })
            })
        }.toString()
        session?.send(Frame.Text(frame))
    }

    override suspend fun sendUserText(text: String) {
        val frame = buildJsonObject {
            put("clientContent", buildJsonObject {
                put("turns", buildJsonArray {
                    add(buildJsonObject {
                        put("role", "user")
                        put("parts", buildJsonArray { add(buildJsonObject { put("text", text) }) })
                    })
                })
                put("turnComplete", true)
            })
        }.toString()
        session?.send(Frame.Text(frame))
    }

    override fun audioOut(): SharedFlow<ByteArray> = audio.asSharedFlow()

    override suspend fun close() {
        runCatching { session?.close() }
        session = null
    }
}
