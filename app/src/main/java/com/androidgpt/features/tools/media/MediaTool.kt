package com.androidgpt.features.tools.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.androidgpt.features.tools.Tool
import com.androidgpt.features.tools.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder
import javax.inject.Inject

class MediaTool @Inject constructor(
    @ApplicationContext private val ctx: Context,
) : Tool {
    override val name = "play_video"
    override val description = "Открыть YouTube/браузер с поиском видео. args: {query: string}"

    override suspend fun execute(args: JsonObject): ToolResult {
        val query = args["query"]?.jsonPrimitive?.content
            ?: return ToolResult("Не указан запрос видео", ok = false)
        val url = "https://www.youtube.com/results?search_query=" + URLEncoder.encode(query, "UTF-8")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
        return ToolResult("Открываю «$query»")
    }
}
