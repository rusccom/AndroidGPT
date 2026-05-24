package com.androidgpt.core.di

import com.androidgpt.features.voice.AndroidStt
import com.androidgpt.features.voice.AndroidTts
import com.androidgpt.features.voice.LocalStt
import com.androidgpt.features.voice.LocalTts
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VoiceModule {
    @Binds @Singleton abstract fun stt(impl: AndroidStt): LocalStt
    @Binds @Singleton abstract fun tts(impl: AndroidTts): LocalTts
}
