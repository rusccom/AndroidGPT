package com.androidgpt.features.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class AndroidTts @Inject constructor(
    @ApplicationContext private val ctx: Context,
) : LocalTts {

    @Volatile private var tts: TextToSpeech? = null
    @Volatile private var ready: Boolean = false

    private fun ensureInit(): TextToSpeech {
        tts?.let { return it }
        val instance = TextToSpeech(ctx) { status -> ready = (status == TextToSpeech.SUCCESS) }
        tts = instance
        return instance
    }

    override suspend fun speak(text: String, language: String) = suspendCancellableCoroutine<Unit> { cont ->
        val engine = ensureInit()
        val locale = Locale.forLanguageTag(language)
        engine.language = locale
        val id = UUID.randomUUID().toString()
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { if (cont.isActive) cont.resume(Unit) }
            @Deprecated("legacy")
            override fun onError(utteranceId: String?) { if (cont.isActive) cont.resume(Unit) }
            override fun onError(utteranceId: String?, errorCode: Int) { if (cont.isActive) cont.resume(Unit) }
        })
        val rc = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        if (rc == TextToSpeech.ERROR && cont.isActive) cont.resume(Unit)
        cont.invokeOnCancellation { engine.stop() }
    }

    override fun stop() { tts?.stop() }
    override fun release() { tts?.shutdown(); tts = null; ready = false }
}
