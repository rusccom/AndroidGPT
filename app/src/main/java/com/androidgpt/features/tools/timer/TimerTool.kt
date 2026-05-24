package com.androidgpt.features.tools.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.androidgpt.features.tools.Tool
import com.androidgpt.features.tools.ToolResult
import com.androidgpt.features.tools.alarm.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

class TimerTool @Inject constructor(
    @ApplicationContext private val ctx: Context,
) : Tool {
    override val name = "set_timer"
    override val description = "Поставить таймер. args: {seconds: number, label?: string}"

    override suspend fun execute(args: JsonObject): ToolResult {
        val seconds = args["seconds"]?.jsonPrimitive?.content?.toLongOrNull()
            ?: return ToolResult("Не указано время таймера", ok = false)
        val label = args["label"]?.jsonPrimitive?.content ?: "Таймер"
        val triggerAt = System.currentTimeMillis() + seconds * 1000
        schedule(triggerAt, label)
        return ToolResult("Таймер на ${formatSeconds(seconds)} запущен")
    }

    private fun schedule(triggerMs: Long, label: String) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(ctx, AlarmReceiver::class.java).apply { putExtra("label", label) }
        val pi = PendingIntent.getBroadcast(
            ctx, triggerMs.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
    }

    private fun formatSeconds(s: Long): String {
        val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
        return buildString {
            if (h > 0) append("${h}ч "); if (m > 0) append("${m}мин "); if (sec > 0) append("${sec}сек")
        }.trim().ifEmpty { "${s}сек" }
    }
}
