package com.androidgpt.features.voice

import kotlinx.coroutines.flow.Flow

interface LocalStt {
    /** Запускает распознавание, возвращает поток финальных и партиальных гипотез. */
    fun listen(language: String = "ru-RU"): Flow<SttEvent>
    fun stop()
}

sealed interface SttEvent {
    data class Partial(val text: String) : SttEvent
    data class Final(val text: String) : SttEvent
    data class Error(val code: Int, val message: String) : SttEvent
    data object EndOfSpeech : SttEvent
}
