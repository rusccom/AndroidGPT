package com.androidgpt.features.expert.openai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class OaServerEvent(
    val type: String,
    val event_id: String? = null,
    val delta: String? = null,
    val transcript: String? = null,
    val text: String? = null,
    val item: JsonElement? = null,
    val response: JsonElement? = null,
    val audio: String? = null,
    val error: JsonElement? = null,
)

object OaEventType {
    const val SESSION_UPDATE = "session.update"
    const val INPUT_AUDIO_APPEND = "input_audio_buffer.append"
    const val INPUT_AUDIO_COMMIT = "input_audio_buffer.commit"
    const val RESPONSE_CREATE = "response.create"
    const val CONVERSATION_ITEM_CREATE = "conversation.item.create"

    const val RES_AUDIO_DELTA = "response.audio.delta"
    const val RES_AUDIO_TRANSCRIPT_DELTA = "response.audio_transcript.delta"
    const val RES_TEXT_DELTA = "response.text.delta"
    const val RES_DONE = "response.done"
    const val INPUT_TRANSCRIPTION_COMPLETED = "conversation.item.input_audio_transcription.completed"
    const val ERROR = "error"
}
