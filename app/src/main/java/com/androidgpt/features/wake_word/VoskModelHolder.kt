package com.androidgpt.features.wake_word

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import org.vosk.Model
import org.vosk.android.StorageService
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Распаковывает Vosk-модель из assets во внешнее хранилище и кэширует Model.
 * Положи распакованную модель vosk-model-small-ru в app/src/main/assets/model-ru/
 */
@Singleton
class VoskModelHolder @Inject constructor(
    @ApplicationContext private val ctx: Context,
) {
    @Volatile private var cached: Model? = null

    suspend fun load(): Model {
        cached?.let { return it }
        val model = unpack()
        cached = model
        return model
    }

    private suspend fun unpack(): Model = suspendCancellableCoroutine { cont ->
        StorageService.unpack(
            ctx,
            ASSET_DIR,
            EXTERNAL_DIR,
            { model -> cont.resume(model) },
            { error -> cont.resumeWithException(error) },
        )
    }

    companion object {
        const val ASSET_DIR = "model-ru"
        const val EXTERNAL_DIR = "vosk-model-ru"
    }
}
