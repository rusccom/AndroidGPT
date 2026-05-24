package com.androidgpt.features.expert

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ExpertService : Service() {

    @Inject lateinit var session: ExpertSession

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        when (intent?.action) {
            ACTION_START -> session.start(intent.getStringExtra("topic"))
            ACTION_STOP -> { session.stop(); stopSelf() }
        }
        return START_STICKY
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, "expert")
            .setContentTitle("Эксперт активен")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        session.stop()
        super.onDestroy()
    }

    companion object {
        const val NOTIF_ID = 4201
        const val ACTION_START = "com.androidgpt.expert.START"
        const val ACTION_STOP = "com.androidgpt.expert.STOP"
    }
}
