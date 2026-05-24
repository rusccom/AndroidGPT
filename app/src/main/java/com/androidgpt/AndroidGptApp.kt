package com.androidgpt

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AndroidGptApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)
        listOf(
            channel("expert", getString(R.string.channel_expert), NotificationManager.IMPORTANCE_LOW),
            channel("wake", getString(R.string.channel_wake), NotificationManager.IMPORTANCE_MIN),
            channel("call", getString(R.string.channel_call), NotificationManager.IMPORTANCE_HIGH),
            channel("alarm", getString(R.string.channel_alarm), NotificationManager.IMPORTANCE_HIGH),
            channel("download", "Загрузки", NotificationManager.IMPORTANCE_LOW),
        ).forEach(nm::createNotificationChannel)
    }

    private fun channel(id: String, name: String, importance: Int) =
        NotificationChannel(id, name, importance)
}
