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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Будильник / напоминание. Поддерживает явный time="HH:mm" либо разбор raw на русском
 * ("через 2 часа", "в среду в 9", "завтра в 8 утра").
 */
class AlarmTool @Inject constructor(
    @ApplicationContext private val ctx: Context,
) : Tool {
    override val name = "set_alarm"
    override val description = "Будильник/напоминание. args: {time?: 'HH:mm', raw?: string, label?: string}"

    override suspend fun execute(args: JsonObject): ToolResult {
        val label = args["label"]?.jsonPrimitive?.content
        val explicit = args["time"]?.jsonPrimitive?.content
        val raw = args["raw"]?.jsonPrimitive?.content
        val trigger = resolveTrigger(explicit, raw) ?: return error("Не понял время")
        scheduleExact(trigger, label)
        return ToolResult("Поставлено на ${formatHm(trigger)}")
    }

    private fun resolveTrigger(explicit: String?, raw: String?): Long? {
        explicit?.let { parseHm(it) }?.let { (h, m) -> return nextAtHm(h, m) }
        raw?.let { TimeParser.parse(it) }?.let { return it }
        return null
    }

    private fun parseHm(text: String): Pair<Int, Int>? {
        val parts = text.split(":")
        if (parts.size != 2) return null
        val h = parts[0].trim().toIntOrNull() ?: return null
        val m = parts[1].trim().toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h to m
    }

    private fun nextAtHm(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DATE, 1)
        }
        return cal.timeInMillis
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

    private fun formatHm(ms: Long): String {
        val fmt = SimpleDateFormat("EEE HH:mm", Locale("ru"))
        return fmt.format(Date(ms))
    }

    private fun error(msg: String) = ToolResult(reply = msg, ok = false)
}
