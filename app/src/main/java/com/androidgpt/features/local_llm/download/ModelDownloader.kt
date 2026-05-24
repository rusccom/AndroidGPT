package com.androidgpt.features.local_llm.download

import com.androidgpt.features.local_llm.catalog.CatalogModel
import com.androidgpt.features.local_llm.storage.InstalledModel
import com.androidgpt.features.local_llm.storage.ModelSource
import com.androidgpt.features.local_llm.storage.ModelStorage
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelDownloader @Inject constructor(
    private val http: HttpClient,
    private val storage: ModelStorage,
    private val verifier: Sha256Verifier,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = mutableMapOf<String, Job>()

    private val _progress = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val progress: StateFlow<Map<String, DownloadProgress>> = _progress.asStateFlow()

    fun startCatalog(model: CatalogModel) = start(
        id = model.id, name = model.name, url = model.url, sha256 = model.sha256,
        totalHint = model.sizeBytes, context = model.context, source = ModelSource.CATALOG,
    )

    fun startCustom(id: String, name: String, url: String, sha256: String?, context: Int = 8192) = start(
        id = id, name = name, url = url, sha256 = sha256,
        totalHint = 0L, context = context, source = ModelSource.CUSTOM_URL,
    )

    fun cancel(id: String) {
        jobs.remove(id)?.cancel()
        emit(DownloadProgress(id, 0, 0, DownloadProgress.State.CANCELLED))
    }

    private fun start(
        id: String, name: String, url: String, sha256: String?,
        totalHint: Long, context: Int, source: ModelSource,
    ) {
        if (jobs[id]?.isActive == true) return
        emit(DownloadProgress(id, 0, totalHint, DownloadProgress.State.QUEUED))
        jobs[id] = scope.launch { runDownload(id, name, url, sha256, totalHint, context, source) }
    }

    private suspend fun runDownload(
        id: String, name: String, url: String, sha256: String?,
        totalHint: Long, context: Int, source: ModelSource,
    ) {
        val tmp = storage.tempFile(id)
        val final = storage.finalFile(id)
        val resumeFrom = if (tmp.exists()) tmp.length() else 0L
        runCatching {
            http.prepareGet(url) {
                if (resumeFrom > 0) headers { append(HttpHeaders.Range, "bytes=$resumeFrom-") }
            }.execute { resp ->
                val total = (resp.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: 0L) + resumeFrom
                writeChannel(resp.bodyAsChannel(), tmp, resumeFrom, total.coerceAtLeast(totalHint), id)
            }
            emit(DownloadProgress(id, tmp.length(), tmp.length(), DownloadProgress.State.VERIFYING))
            if (!verifier.verify(tmp, sha256)) error("SHA256 mismatch")
            tmp.renameTo(final)
            storage.add(InstalledModel(id, name, final.absolutePath, final.length(), context, source, url))
            emit(DownloadProgress(id, final.length(), final.length(), DownloadProgress.State.DONE))
        }.onFailure { e ->
            emit(DownloadProgress(id, tmp.length(), totalHint, DownloadProgress.State.ERROR, e.message))
        }
    }

    private suspend fun writeChannel(
        channel: io.ktor.utils.io.ByteReadChannel, tmp: java.io.File,
        startAt: Long, total: Long, id: String,
    ) {
        RandomAccessFile(tmp, "rw").use { raf ->
            raf.seek(startAt)
            var written = startAt
            while (!channel.isClosedForRead) {
                val packet = channel.readRemaining(64 * 1024)
                val bytes = packet.readBytes()
                if (bytes.isEmpty()) continue
                raf.write(bytes)
                written += bytes.size
                emit(DownloadProgress(id, written, total, DownloadProgress.State.RUNNING))
            }
        }
    }

    private fun emit(p: DownloadProgress) {
        _progress.value = _progress.value.toMutableMap().also { it[p.modelId] = p }
    }
}
