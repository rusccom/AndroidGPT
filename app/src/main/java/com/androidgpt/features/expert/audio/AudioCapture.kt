package com.androidgpt.features.expert.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** PCM16 mono capture для Realtime API. По умолчанию 24kHz (OpenAI). */
@Singleton
class AudioCapture @Inject constructor() {

    @SuppressLint("MissingPermission")
    fun stream(sampleRate: Int = 24_000): Flow<ByteArray> = callbackFlow {
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(4096)
        val rec = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuf * 2,
        )
        rec.startRecording()
        val buf = ByteArray(minBuf)
        val job = launch(Dispatchers.IO) {
            while (isActive && rec.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val n = rec.read(buf, 0, buf.size)
                if (n > 0) trySend(buf.copyOf(n))
            }
        }
        awaitClose {
            job.cancel()
            runCatching { rec.stop() }
            runCatching { rec.release() }
        }
    }.flowOn(Dispatchers.IO)
}
