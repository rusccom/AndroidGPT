package com.androidgpt.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: ApiKeysRepository,
) : ViewModel() {

    val state: StateFlow<SettingsState> = repo.state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        repo.state.value,
    )

    fun setKey(key: ApiKey, value: String) = repo.set(key, value)
    fun setProvider(p: ExpertProvider) = repo.setProvider(p)
    fun setVoice(v: ExpertVoice) = repo.setVoice(v)
}
