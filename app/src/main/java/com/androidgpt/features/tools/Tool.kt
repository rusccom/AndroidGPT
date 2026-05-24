package com.androidgpt.features.tools

import kotlinx.serialization.json.JsonObject

interface Tool {
    val name: String
    val description: String
    suspend fun execute(args: JsonObject): ToolResult
}

data class ToolResult(
    val reply: String,
    val nextAction: NextAction = NextAction.None,
    val ok: Boolean = true,
) {
    sealed interface NextAction {
        data object None : NextAction
        data class StartExpert(val topic: String?) : NextAction
        data class StartCall(val contactQuery: String, val withVideo: Boolean) : NextAction
    }
}
