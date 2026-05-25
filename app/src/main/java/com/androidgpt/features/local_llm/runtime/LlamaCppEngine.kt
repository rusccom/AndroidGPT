package com.androidgpt.features.local_llm.runtime

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.nehuatl.llamacpp.LlamaHelper
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Реальный движок поверх io.github.ljcamargo:llamacpp-kotlin (org.nehuatl.llamacpp.LlamaHelper).
 * Потокобезопасен через два Mutex (load и predict).
 */
@Singleton
class LlamaCppEngine @Inject constructor(
    @ApplicationContext private val ctx: Context,
) : LlamaEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val events = MutableSharedFlow<LlamaHelper.LLMEvent>(extraBufferCapacity = 256)
    private val helper by lazy { LlamaHelper(ctx.contentResolver, scope, events) }

    private val loadMutex = Mutex()
    private val predictMutex = Mutex()
    @Volatile private var loaded: Boolean = false

    override suspend fun load(modelPath: String, config: LlamaConfig) = loadMutex.withLock {
        loaded = false
        val uri = Uri.fromFile(File(modelPath)).toString()
        suspendCancellableCoroutine { cont ->
            val errorWatcher = scope.launch {
                events.collect { ev ->
                    if (ev is LlamaHelper.LLMEvent.Error && cont.isActive) {
                        cont.resume(Unit)
                    }
                }
            }
            helper.load(path = uri, contextLength = config.contextSize) { _ ->
                if (cont.isActive) { loaded = true; cont.resume(Unit) }
            }
            cont.invokeOnCancellation { errorWatcher.cancel() }
        }
    }

    override suspend fun unload() {
        runCatching { helper.abort() }
        runCatching { helper.release() }
        loaded = false
    }

    override fun isLoaded(): Boolean = loaded

    override fun generate(prompt: String, stop: List<String>): Flow<String> = callbackFlow {
        val gate = predictMutex
        gate.lock()
        val collector = scope.launch {
            events.collect { ev ->
                when (ev) {
                    is LlamaHelper.LLMEvent.Ongoing -> trySend(ev.word)
                    is LlamaHelper.LLMEvent.Done -> close()
                    is LlamaHelper.LLMEvent.Error -> close(RuntimeException(ev.message))
                    else -> Unit
                }
            }
        }
        runCatching { helper.predict(prompt) }.onFailure {
            close(it); collector.cancel(); gate.unlock(); return@callbackFlow
        }
        awaitClose {
            runCatching { helper.stopPrediction() }
            collector.cancel()
            if (gate.isLocked) gate.unlock()
        }
    }

    override suspend fun complete(prompt: String, stop: List<String>): String {
        if (!loaded) return ""
        val sb = StringBuilder()
        generate(prompt, stop).collect { sb.append(it) }
        return sb.toString().trim()
    }
}
