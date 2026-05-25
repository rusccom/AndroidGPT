package com.androidgpt.features.wake_word

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.androidgpt.MainActivity
import com.androidgpt.features.assistant.AssistantBus
import com.androidgpt.features.assistant.AssistantPhase
import com.androidgpt.features.assistant.AssistantOrchestrator
import com.androidgpt.features.local_llm.ModelManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WakeWordService : Service() {

    @Inject lateinit var holder: VoskModelHolder
    @Inject lateinit var wake: VoskWakeWordDetector
    @Inject lateinit var command: VoskCommandRecognizer
    @Inject lateinit var orchestrator: AssistantOrchestrator
    @Inject lateinit var bus: AssistantBus
    @Inject lateinit var models: ModelManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loopJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        if (loopJob == null) {
            scope.launch { runCatching { models.warmUp() } }
            loopJob = scope.launch { runLoop() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        wake.stop(); command.stop()
        loopJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun runLoop() {
        bus.setPhase(AssistantPhase.MODEL_LOADING)
        if (runCatching { holder.load() }.isFailure) {
            bus.setError("Модель Vosk не найдена. Положи vosk-model-small-ru в app/src/main/assets/model-ru/")
            return
        }
        while (scope.isActive) {
            bus.setPhase(AssistantPhase.WAITING_WAKE)
            val woke = runCatching { wake.detections().first() }
            wake.stop()
            if (woke.isFailure) { bus.setError(woke.exceptionOrNull()?.message ?: "wake error"); continue }
            handleCommand()
        }
    }

    private suspend fun handleCommand() {
        bus.setPhase(AssistantPhase.LISTENING)
        val text = runCatching { collectCommand() }.getOrElse {
            bus.setError(it.message ?: "stt error"); return
        }
        if (text.isBlank()) return
        bus.setHeard(text)
        bus.setPhase(AssistantPhase.THINKING)
        runCatching { orchestrator.handle(text) }
            .onSuccess { bus.setReply(it.reply, it.nextAction) }
            .onFailure { bus.setError(it.message ?: "orchestrator error") }
    }

    private suspend fun collectCommand(): String {
        var last = ""
        command.listen().collect { ev ->
            when (ev) {
                is CommandEvent.Partial -> { last = ev.text; bus.setPartial(ev.text) }
                is CommandEvent.Final -> last = ev.text
            }
        }
        return last
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, "wake")
            .setContentTitle("Слушаю «Sam»")
            .setContentText("Скажи Sam для начала команды")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    companion object { const val NOTIF_ID = 4101 }
}
