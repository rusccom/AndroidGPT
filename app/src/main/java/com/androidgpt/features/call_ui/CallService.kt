package com.androidgpt.features.call_ui

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.androidgpt.features.telegram.TelegramCallManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CallService : Service() {

    @Inject lateinit var manager: TelegramCallManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        when (intent?.action) {
            ACTION_HANGUP -> { scope.launch { manager.hangup() }; stopSelf() }
            ACTION_ACCEPT -> scope.launch { manager.accept() }
            ACTION_DECLINE -> { scope.launch { manager.decline() }; stopSelf() }
        }
        return START_STICKY
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, "call")
            .setContentTitle("Звонок")
            .setSmallIcon(android.R.drawable.sym_call_outgoing)
            .setOngoing(true)
            .build()

    companion object {
        const val NOTIF_ID = 4301
        const val ACTION_HANGUP = "call.HANGUP"
        const val ACTION_ACCEPT = "call.ACCEPT"
        const val ACTION_DECLINE = "call.DECLINE"
    }
}
