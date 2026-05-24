package com.androidgpt.features.tools.expert

import com.androidgpt.features.tools.Tool
import com.androidgpt.features.tools.ToolResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

class ExpertModeTool @Inject constructor() : Tool {
    override val name = "expert_mode"
    override val description = "Запустить голосовой режим Эксперта (Realtime). args: {topic?: string}"

    override suspend fun execute(args: JsonObject): ToolResult {
        val topic = args["topic"]?.jsonPrimitive?.content
        return ToolResult(
            reply = "Подключаюсь к Эксперту...",
            nextAction = ToolResult.NextAction.StartExpert(topic),
        )
    }
}
