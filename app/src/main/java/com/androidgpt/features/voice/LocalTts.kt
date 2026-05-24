package com.androidgpt.features.voice

interface LocalTts {
    suspend fun speak(text: String, language: String = "ru-RU")
    fun stop()
    fun release()
}
