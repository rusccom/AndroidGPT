package com.androidgpt.features.local_llm.runtime

import com.androidgpt.features.local_llm.storage.ActiveModelPrefs
import com.androidgpt.features.local_llm.storage.InstalledModel
import com.androidgpt.features.local_llm.storage.ModelStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlamaSession @Inject constructor(
    private val engine: LlamaEngine,
    private val storage: ModelStorage,
    private val activePrefs: ActiveModelPrefs,
) {
    private val mutex = Mutex()
    private val _active = MutableStateFlow<InstalledModel?>(null)
    val active: StateFlow<InstalledModel?> = _active

    suspend fun activate(model: InstalledModel, cfg: LlamaConfig = LlamaConfig(contextSize = model.context)) {
        mutex.withLock {
            if (engine.isLoaded()) engine.unload()
            engine.load(model.path, cfg)
            activePrefs.set(model.id)
            _active.value = model
        }
    }

    suspend fun activateActiveOrFirst() {
        val id = activePrefs.activeId.value
        val target = storage.byId(id ?: "") ?: storage.models.value.firstOrNull() ?: return
        activate(target)
    }

    suspend fun deactivate() {
        mutex.withLock { engine.unload(); _active.value = null; activePrefs.set(null) }
    }

    fun engine(): LlamaEngine = engine
}
