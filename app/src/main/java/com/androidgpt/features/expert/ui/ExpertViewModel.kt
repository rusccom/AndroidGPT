package com.androidgpt.features.expert.ui

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.androidgpt.features.expert.ExpertService
import com.androidgpt.features.expert.ExpertSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ExpertViewModel @Inject constructor(
    app: Application,
    private val session: ExpertSession,
) : AndroidViewModel(app) {

    val state: StateFlow<ExpertSession.State> = session.state

    fun start(topic: String? = null) {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, ExpertService::class.java).apply {
            action = ExpertService.ACTION_START
            putExtra("topic", topic)
        }
        ContextCompat.startForegroundService(ctx, intent)
    }

    fun stop() {
        val ctx = getApplication<Application>()
        ctx.startService(Intent(ctx, ExpertService::class.java).apply { action = ExpertService.ACTION_STOP })
    }
}
