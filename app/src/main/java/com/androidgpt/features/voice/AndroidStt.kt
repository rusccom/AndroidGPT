package com.androidgpt.features.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidStt @Inject constructor(
    @ApplicationContext private val ctx: Context,
) : LocalStt {

    @Volatile private var recognizer: SpeechRecognizer? = null

    override fun listen(language: String): Flow<SttEvent> = callbackFlow {
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
            trySend(SttEvent.Error(-1, "SpeechRecognizer недоступен"))
            close(); return@callbackFlow
        }
        val sr = SpeechRecognizer.createSpeechRecognizer(ctx).also { recognizer = it }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        sr.setRecognitionListener(buildListener { trySend(it) })
        sr.startListening(intent)
        awaitClose { runCatching { sr.destroy() }; recognizer = null }
    }

    override fun stop() {
        runCatching { recognizer?.stopListening() }
    }

    private fun buildListener(emit: (SttEvent) -> Unit) = object : RecognitionListener {
        override fun onReadyForSpeech(p: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { emit(SttEvent.EndOfSpeech) }
        override fun onError(error: Int) { emit(SttEvent.Error(error, "STT error $error")) }
        override fun onResults(results: Bundle?) {
            results.firstHypothesis()?.let { emit(SttEvent.Final(it)) }
        }
        override fun onPartialResults(partial: Bundle?) {
            partial.firstHypothesis()?.let { emit(SttEvent.Partial(it)) }
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun Bundle?.firstHypothesis(): String? =
        this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
}
