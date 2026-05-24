package com.androidgpt.features.settings

import com.androidgpt.core.crypto.SecureStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeysRepository @Inject constructor(
    private val storage: SecureStorage,
) {
    private val _state = MutableStateFlow(load())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun get(key: ApiKey): String = storage.read(key.storageKey).orEmpty()

    fun set(key: ApiKey, value: String) {
        storage.write(key.storageKey, value.trim().ifBlank { null })
        _state.value = load()
    }

    fun setProvider(provider: ExpertProvider) {
        storage.write(ExpertProvider.PREF_KEY, provider.name)
        _state.value = load()
    }

    fun setVoice(voice: ExpertVoice) {
        storage.write(ExpertVoice.PREF_KEY, voice.name)
        _state.value = load()
    }

    private fun load(): SettingsState = SettingsState(
        keys = ApiKey.entries.associateWith { storage.read(it.storageKey).orEmpty() },
        provider = ExpertProvider.parse(storage.read(ExpertProvider.PREF_KEY)),
        voice = ExpertVoice.parse(storage.read(ExpertVoice.PREF_KEY)),
    )
}

data class SettingsState(
    val keys: Map<ApiKey, String>,
    val provider: ExpertProvider,
    val voice: ExpertVoice,
) {
    fun has(key: ApiKey): Boolean = keys[key]?.isNotBlank() == true
    fun value(key: ApiKey): String = keys[key].orEmpty()
}
