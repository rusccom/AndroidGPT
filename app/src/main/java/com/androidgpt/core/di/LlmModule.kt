package com.androidgpt.core.di

import com.androidgpt.core.network.HttpClientFactory
import com.androidgpt.features.local_llm.runtime.LlamaEngine
import com.androidgpt.features.local_llm.runtime.StubLlamaEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LlmModule {

    @Provides
    @Singleton
    fun provideJson(factory: HttpClientFactory): Json = factory.json

    @Provides
    @Singleton
    fun provideLlamaEngine(): LlamaEngine = StubLlamaEngine()
    // TODO: заменить на JNI-обёртку llama.cpp когда подключите нативку
}
