package com.androidgpt.features.expert

import kotlinx.coroutines.flow.Flow

sealed interface ExpertEvent {
    data class Transcript(val text: String, val isFinal: Boolean, val fromUser: Boolean) : ExpertEvent
    data class Error(val message: String) : ExpertEvent
    data object Connected : ExpertEvent
    data object Disconnected : ExpertEvent
}

/** Унифицированный интерфейс для OpenAI Realtime / Gemini Live. */
interface ExpertRealtimeClient {
    val sampleRate: Int
    suspend fun connect(): Flow<ExpertEvent>
    suspend fun sendAudio(pcm16: ByteArray)
    suspend fun sendUserText(text: String)
    suspend fun close()
    /** Поток PCM16 mono от модели — направляется в AudioPlayback. */
    fun audioOut(): Flow<ByteArray>
}
