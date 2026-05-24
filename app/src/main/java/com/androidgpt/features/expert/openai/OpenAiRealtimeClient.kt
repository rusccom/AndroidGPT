package com.androidgpt.features.expert.openai

import android.util.Base64
import com.androidgpt.features.expert.ExpertEvent
import com.androidgpt.features.expert.ExpertRealtimeClient
import com.androidgpt.features.settings.ApiKey
import com.androidgpt.features.settings.ApiKeysRepository
import com.androidgpt.features.settings.ExpertVoice
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class OpenAiRealtimeClient @Inject constructor(
    private val http: HttpClient,
    private val keys: ApiKeysRepository,
    private val json: Json,
) : ExpertRealtimeClient {

    override val sampleRate: Int = 24_000

    @Volatile private var session: DefaultClientWebSocketSession? = null
    private val audio = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)

    override suspend fun connect(): Flow<ExpertEvent> = channelFlow {
        val apiKey = keys.get(ApiKey.OpenAi)
        if (apiKey.isBlank()) { trySend(ExpertEvent.Error("Нет OpenAI ключа")); close(); return@channelFlow }
        val url = "wss://api.openai.com/v1/realtime?model=gpt-4o-realtime-preview"
        val ws = http.webSocketSession(url) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $apiKey")
                append("OpenAI-Beta", "realtime=v1")
            }
        }
        session = ws
        ws.send(Frame.Text(sessionUpdateFrame()))
        trySend(ExpertEvent.Connected)
        for (frame in ws.incoming) {
            if (frame !is Frame.Text) continue
            parseFrame(frame.readText())?.let { trySend(it) }
        }
        trySend(ExpertEvent.Disconnected)
        awaitCloseSafely()
    }

    private suspend fun awaitCloseSafely() = runCatching { session?.close() }

    private fun parseFrame(payload: String): ExpertEvent? = runCatching {
        val evt = json.decodeFromString(OaServerEvent.serializer(), payload)
        when (evt.type) {
            OaEventType.RES_AUDIO_DELTA -> evt.delta?.let {
                audio.tryEmit(Base64.decode(it, Base64.NO_WRAP)); null
            }
            OaEventType.RES_AUDIO_TRANSCRIPT_DELTA ->
                evt.delta?.let { ExpertEvent.Transcript(it, false, fromUser = false) }
            OaEventType.RES_DONE -> ExpertEvent.Transcript("", true, fromUser = false)
            OaEventType.INPUT_TRANSCRIPTION_COMPLETED ->
                evt.transcript?.let { ExpertEvent.Transcript(it, true, fromUser = true) }
            OaEventType.ERROR -> ExpertEvent.Error(evt.error?.toString().orEmpty())
            else -> null
        }
    }.getOrNull()

    private fun sessionUpdateFrame(): String {
        val voice = ExpertVoice.parse(null).display
        val body = buildJsonObject {
            put("type", OaEventType.SESSION_UPDATE)
            put("session", buildJsonObject {
                put("modalities", buildJsonArray { add("audio"); add("text") })
                put("voice", voice)
                put("input_audio_format", "pcm16")
                put("output_audio_format", "pcm16")
                put("input_audio_transcription", buildJsonObject { put("model", "whisper-1") })
                put("turn_detection", buildJsonObject { put("type", "server_vad") })
                put("instructions", "Ты дружелюбный голосовой ассистент. Отвечай кратко на языке пользователя.")
            })
        }
        return body.toString()
    }

    override suspend fun sendAudio(pcm16: ByteArray) {
        val b64 = Base64.encodeToString(pcm16, Base64.NO_WRAP)
        val frame = buildJsonObject {
            put("type", OaEventType.INPUT_AUDIO_APPEND)
            put("audio", b64)
        }.toString()
        session?.send(Frame.Text(frame))
    }

    override suspend fun sendUserText(text: String) {
        val frame = buildJsonObject {
            put("type", OaEventType.CONVERSATION_ITEM_CREATE)
            put("item", buildJsonObject {
                put("type", "message")
                put("role", "user")
                put("content", buildJsonArray {
                    add(buildJsonObject { put("type", "input_text"); put("text", text) })
                })
            })
        }.toString()
        session?.send(Frame.Text(frame))
        session?.send(Frame.Text(buildJsonObject { put("type", OaEventType.RESPONSE_CREATE) }.toString()))
    }

    override fun audioOut(): SharedFlow<ByteArray> = audio.asSharedFlow()

    override suspend fun close() {
        runCatching { session?.close() }
        session = null
    }
}
