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
 * Полнословарное распознавание команды после wake-word.
 * Эмитит партиальные строки и финальную (CommandEvent.Final).
 */
@Singleton
class VoskCommandRecognizer @Inject constructor(
    private val holder: VoskModelHolder,
) {
    @Volatile private var service: SpeechService? = null

    fun listen(timeoutMs: Int = TIMEOUT_MS): Flow<CommandEvent> = callbackFlow {
        val model = runCatching { holder.load() }.getOrElse {
            close(it); return@callbackFlow
        }
        val rec = Recognizer(model, VoskWakeWordDetector.SAMPLE_RATE)
        val svc = SpeechService(rec, VoskWakeWordDetector.SAMPLE_RATE)
        service = svc
        svc.startListening(object : RecognitionListener {
            override fun onPartialResult(hypothesis: String?) {
                pick(hypothesis, "partial")?.let { trySend(CommandEvent.Partial(it)) }
            }
            override fun onResult(hypothesis: String?) {
                pick(hypothesis, "text")?.let { trySend(CommandEvent.Final(it)) }
                channel.close()
            }
            override fun onFinalResult(hypothesis: String?) {
                pick(hypothesis, "text")?.let { trySend(CommandEvent.Final(it)) }
                channel.close()
            }
            override fun onError(e: Exception?) { close(e) }
            override fun onTimeout() { channel.close() }
        }, timeoutMs)
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

    private fun pick(json: String?, key: String): String? {
        if (json.isNullOrBlank()) return null
        val text = runCatching { JSONObject(json).optString(key, "") }.getOrDefault("")
        return text.takeIf { it.isNotBlank() }
    }

    companion object { const val TIMEOUT_MS = 8000 }
}

sealed interface CommandEvent {
    data class Partial(val text: String) : CommandEvent
    data class Final(val text: String) : CommandEvent
}
