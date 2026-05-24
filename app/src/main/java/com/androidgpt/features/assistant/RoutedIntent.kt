package com.androidgpt.features.assistant

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class RoutedIntent(
    val tool: String,
    val args: JsonObject,
)
