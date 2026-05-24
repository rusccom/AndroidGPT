package com.androidgpt.features.local_llm.download

data class DownloadProgress(
    val modelId: String,
    val downloaded: Long,
    val total: Long,
    val state: State,
    val error: String? = null,
) {
    enum class State { QUEUED, RUNNING, VERIFYING, DONE, CANCELLED, ERROR }
    val percent: Int get() = if (total > 0) ((downloaded * 100) / total).toInt() else 0
    val finished: Boolean get() = state == State.DONE || state == State.CANCELLED || state == State.ERROR
}
