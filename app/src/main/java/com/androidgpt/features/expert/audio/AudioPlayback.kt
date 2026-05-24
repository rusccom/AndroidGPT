package com.androidgpt.features.expert.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import javax.inject.Inject
import javax.inject.Singleton

/** PCM16 mono playback. OpenAI отдаёт 24kHz, Gemini — 24kHz. */
@Singleton
class AudioPlayback @Inject constructor() {

    @Volatile private var track: AudioTrack? = null

    fun start(sampleRate: Int = 24_000) {
        if (track != null) return
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(4096)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .setSampleRate(sampleRate)
            .build()
        track = AudioTrack(
            attrs, format, minBuf * 4, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE,
        ).also { it.play() }
    }

    fun write(bytes: ByteArray) {
        track?.write(bytes, 0, bytes.size, AudioTrack.WRITE_BLOCKING)
    }

    fun stop() {
        runCatching { track?.stop() }
        runCatching { track?.release() }
        track = null
    }
}
