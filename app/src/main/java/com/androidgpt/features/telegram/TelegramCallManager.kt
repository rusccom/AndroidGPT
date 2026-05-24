package com.androidgpt.features.telegram

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface CallState {
    data object Idle : CallState
    data class Outgoing(val userId: Long, val withVideo: Boolean) : CallState
    data class Incoming(val userId: Long, val withVideo: Boolean) : CallState
    data class Active(val userId: Long, val withVideo: Boolean) : CallState
    data class Ended(val reason: String) : CallState
}

/**
 * Менеджер звонков.
 *
 * Сейчас лишь выставляет состояния в UI. Для реальных звонков:
 *  1) Использовать TdApi.CreateCall(userId, callProtocol{udpP2p=true, udpReflector=true, libraryVersions=[...]}).
 *  2) Подписаться на updateCall и пробрасывать состояния (ringing, accepted, established, ended).
 *  3) Поднимать media через tgcalls/libtgvoip: PCM capture (AudioRecord) → encode → отправка через native engine.
 *  4) AudioTrack для воспроизведения; CameraX для видеопотока.
 *  5) Foreground service CallService на время звонка.
 */
@Singleton
class TelegramCallManager @Inject constructor() {

    private val _state = MutableStateFlow<CallState>(CallState.Idle)
    val state: StateFlow<CallState> = _state

    suspend fun startCall(userId: Long, withVideo: Boolean) {
        _state.value = CallState.Outgoing(userId, withVideo)
        // TDLib: createCall(userId, protocol{udpP2p, udpReflector, libraryVersions}, isVideo=withVideo)
    }

    suspend fun accept() { val s = _state.value; if (s is CallState.Incoming) _state.value = CallState.Active(s.userId, s.withVideo) }
    suspend fun decline() { _state.value = CallState.Ended("declined") }
    suspend fun hangup() { _state.value = CallState.Ended("hangup") }
}
