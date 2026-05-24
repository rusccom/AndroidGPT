package com.androidgpt.features.local_llm.runtime

data class LlamaConfig(
    val contextSize: Int = 4096,
    val threads: Int = 4,
    val gpuLayers: Int = 0,
    val temperature: Float = 0.4f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 512,
)
