package com.androidgpt.features.wake_word

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONObject
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Постоянно слушает микрофон через Vosk с грамматикой из ключевых слов.
 * Эмитит Unit, когда в гипотезе обнаружено триггер-слово ("сэм" и варианты).
 */
@Singleton
class VoskWakeWordDetector @Inject constructor(
    private val holder: VoskModelHolder,
) {
    @Volatile private var service: SpeechService? = null

    fun detections(): Flow<Unit> = callbackFlow {
        val model = runCatching { holder.load() }.getOrElse {
            close(it); return@callbackFlow
        }
        val rec = Recognizer(model, SAMPLE_RATE, GRAMMAR)
        val svc = SpeechService(rec, SAMPLE_RATE)
        service = svc
        svc.startListening(object : RecognitionListener {
            override fun onPartialResult(hypothesis: String?) {
                if (containsWake(hypothesis, key = "partial")) trySend(Unit)
            }
            override fun onResult(hypothesis: String?) {
                if (containsWake(hypothesis, key = "text")) trySend(Unit)
            }
            override fun onFinalResult(hypothesis: String?) {}
            override fun onError(e: Exception?) { close(e) }
            override fun onTimeout() {}
        })
        awaitClose {
            runCatching { svc.stop() }
            runCatching { svc.shutdown() }
            runCatching { rec.close() }
            service = null
        }
    }

    fun stop() {
        runCatching { service?.stop() }
    }

    private fun containsWake(json: String?, key: String): Boolean {
        if (json.isNullOrBlank()) return false
        val text = runCatching { JSONObject(json).optString(key, "") }.getOrDefault("")
        if (text.isBlank()) return false
        val lower = text.lowercase()
        return KEYWORDS.any { lower.contains(it) }
    }

    companion object {
        const val SAMPLE_RATE = 16000f
        private val KEYWORDS = listOf("сэм", "сем", "сам")
        private val GRAMMAR = "[\"сэм\", \"сем\", \"сам\", \"[unk]\"]"
    }
}
