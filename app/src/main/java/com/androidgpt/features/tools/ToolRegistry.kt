package com.androidgpt.features.tools

import com.androidgpt.features.tools.alarm.AlarmTool
import com.androidgpt.features.tools.chat.ChatTool
import com.androidgpt.features.tools.expert.ExpertModeTool
import com.androidgpt.features.tools.media.MediaTool
import com.androidgpt.features.tools.smart_home.SmartHomeTool
import com.androidgpt.features.tools.telegram_call.TelegramCallTool
import com.androidgpt.features.tools.timer.TimerTool
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolRegistry @Inject constructor(
    alarm: AlarmTool,
    timer: TimerTool,
    media: MediaTool,
    smartHome: SmartHomeTool,
    telegramCall: TelegramCallTool,
    expert: ExpertModeTool,
    chat: ChatTool,
) {
    private val tools: Map<String, Tool> = listOf(alarm, timer, media, smartHome, telegramCall, expert, chat)
        .associateBy { it.name }

    fun all(): Collection<Tool> = tools.values
    fun find(name: String): Tool? = tools[name]
    fun fallback(): Tool = tools.getValue("chat")
}
