package com.androidgpt.core.di

import com.androidgpt.core.network.HttpClientFactory
import com.androidgpt.features.local_llm.runtime.LlamaCppEngine
import com.androidgpt.features.local_llm.runtime.LlamaEngine
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LlmModule {

    @Binds
    @Singleton
    abstract fun bindLlamaEngine(impl: LlamaCppEngine): LlamaEngine

    companion object {
        @Provides
        @Singleton
        fun provideJson(factory: HttpClientFactory): Json = factory.json
    }
}
