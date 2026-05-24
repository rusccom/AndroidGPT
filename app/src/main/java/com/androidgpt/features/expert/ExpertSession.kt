package com.androidgpt.features.expert

import com.androidgpt.features.expert.audio.AudioCapture
import com.androidgpt.features.expert.audio.AudioPlayback
import com.androidgpt.features.expert.gemini.GeminiLiveClient
import com.androidgpt.features.expert.openai.OpenAiRealtimeClient
import com.androidgpt.features.settings.ApiKeysRepository
import com.androidgpt.features.settings.ExpertProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpertSession @Inject constructor(
    private val openAi: OpenAiRealtimeClient,
    private val gemini: GeminiLiveClient,
    private val capture: AudioCapture,
    private val playback: AudioPlayback,
    private val keys: ApiKeysRepository,
) {
    data class State(val active: Boolean = false, val transcript: String = "", val error: String? = null)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private var client: ExpertRealtimeClient? = null

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun start(topic: String? = null) {
        if (job?.isActive == true) return
        val provider = keys.state.value.provider
        val rt = when (provider) {
            ExpertProvider.OpenAi -> openAi
            ExpertProvider.Gemini -> gemini
        }
        client = rt
        playback.start(rt.sampleRate)
        job = scope.launch {
            launch { rt.audioOut().collect { playback.write(it) } }
            launch {
                capture.stream(rt.sampleRate).collect { rt.sendAudio(it) }
            }
            rt.connect().collect { evt -> handleEvent(evt, topic) }
        }
    }

    private suspend fun handleEvent(evt: ExpertEvent, topic: String?) {
        when (evt) {
            is ExpertEvent.Connected -> {
                _state.value = State(active = true)
                topic?.let { client?.sendUserText("Контекст: $it") }
            }
            is ExpertEvent.Transcript -> {
                val prefix = if (evt.fromUser) "вы: " else "эксперт: "
                _state.value = _state.value.copy(transcript = _state.value.transcript + "\n$prefix${evt.text}")
            }
            is ExpertEvent.Error -> _state.value = _state.value.copy(error = evt.message)
            ExpertEvent.Disconnected -> _state.value = State(active = false)
        }
    }

    fun stop() {
        scope.launch {
            runCatching { client?.close() }
            playback.stop()
            job?.cancel(); job = null
            _state.value = State(active = false)
        }
    }
}
