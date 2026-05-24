package com.androidgpt.features.tools.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.androidgpt.features.tools.Tool
import com.androidgpt.features.tools.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Calendar
import javax.inject.Inject

class AlarmTool @Inject constructor(
    @ApplicationContext private val ctx: Context,
) : Tool {
    override val name = "set_alarm"
    override val description = "Поставить будильник. args: {time: 'HH:mm', label?: string}"

    override suspend fun execute(args: JsonObject): ToolResult {
        val time = args["time"]?.jsonPrimitive?.content ?: return error("Не указано время")
        val label = args["label"]?.jsonPrimitive?.content
        val parsed = parseTime(time) ?: return error("Неверный формат времени: $time")
        val trigger = nextTriggerAt(parsed.first, parsed.second)
        scheduleExact(trigger, label)
        return ToolResult(reply = "Будильник на ${"%02d:%02d".format(parsed.first, parsed.second)} установлен")
    }

    private fun scheduleExact(triggerMs: Long, label: String?) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(ctx, AlarmReceiver::class.java).apply { putExtra("label", label) }
        val pi = PendingIntent.getBroadcast(ctx, triggerMs.toInt(), intent, flags())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
        }
    }

    private fun flags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    private fun parseTime(text: String): Pair<Int, Int>? {
        val parts = text.split(":")
        if (parts.size != 2) return null
        val h = parts[0].trim().toIntOrNull() ?: return null
        val m = parts[1].trim().toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h to m
    }

    private fun nextTriggerAt(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DATE, 1)
        }
        return cal.timeInMillis
    }

    private fun error(msg: String) = ToolResult(reply = msg, ok = false)
}
