package com.androidgpt.features.tools.system

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import com.androidgpt.features.tools.Tool
import com.androidgpt.features.tools.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

/**
 * Системные регуляторы: громкость / яркость / Bluetooth.
 * args: { action: "volume_up|volume_down|volume_set|mute|unmute|brightness_set|bluetooth_on|bluetooth_off|bluetooth_toggle",
 *         value?: Int (0..100) }
 */
class SystemControlTool @Inject constructor(
    @ApplicationContext private val ctx: Context,
) : Tool {
    override val name = "system_control"
    override val description = "Громкость / яркость / Bluetooth. args: {action, value?}"

    override suspend fun execute(args: JsonObject): ToolResult {
        val action = args["action"]?.jsonPrimitive?.content
            ?: return ToolResult("Не указано action", ok = false)
        val value = args["value"]?.jsonPrimitive?.content?.toIntOrNull()
        return when (action) {
            "volume_up" -> adjustVolume(true)
            "volume_down" -> adjustVolume(false)
            "volume_set" -> setVolume(value)
            "mute" -> setVolume(0)
            "unmute" -> setVolume(50)
            "brightness_set" -> setBrightness(value)
            "bluetooth_on", "bluetooth_off", "bluetooth_toggle" -> bluetooth(action)
            else -> ToolResult("Неизвестное action: $action", ok = false)
        }
    }

    private fun audio(): AudioManager? = ctx.getSystemService(AudioManager::class.java)

    private fun adjustVolume(up: Boolean): ToolResult {
        val am = audio() ?: return ToolResult("AudioManager недоступен", ok = false)
        val dir = if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, dir, AudioManager.FLAG_SHOW_UI)
        val pct = currentVolumePct(am)
        return ToolResult("Громкость ${if (up) "+" else "-"}, сейчас $pct%")
    }

    private fun setVolume(pct: Int?): ToolResult {
        if (pct == null || pct !in 0..100) return ToolResult("Уровень громкости 0..100", ok = false)
        val am = audio() ?: return ToolResult("AudioManager недоступен", ok = false)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (max * pct / 100).coerceIn(0, max)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
        return ToolResult("Громкость $pct%")
    }

    private fun currentVolumePct(am: AudioManager): Int {
        val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return cur * 100 / max
    }

    private fun setBrightness(pct: Int?): ToolResult {
        if (pct == null || pct !in 0..100) return ToolResult("Яркость 0..100", ok = false)
        if (!Settings.System.canWrite(ctx)) {
            val i = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(i)
            return ToolResult("Нужно разрешение «Изменение системных настроек». Открыл настройки.", ok = false)
        }
        val target = (pct * 255 / 100).coerceIn(0, 255)
        Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0)
        Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, target)
        return ToolResult("Яркость $pct%")
    }

    private fun bluetooth(action: String): ToolResult {
        val mgr = ctx.getSystemService(BluetoothManager::class.java)
            ?: return ToolResult("Bluetooth не поддерживается", ok = false)
        val adapter = mgr.adapter ?: return ToolResult("Bluetooth адаптер отсутствует", ok = false)
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
        val current = if (adapter.isEnabled) "вкл" else "выкл"
        val target = when (action) {
            "bluetooth_on" -> "вкл"
            "bluetooth_off" -> "выкл"
            else -> if (adapter.isEnabled) "выкл" else "вкл"
        }
        return ToolResult("Bluetooth сейчас $current. Подтверди $target в открывшихся настройках.")
    }
}
