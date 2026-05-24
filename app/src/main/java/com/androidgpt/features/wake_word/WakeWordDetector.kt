package com.androidgpt.features.wake_word

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wake-word детектор. Заглушка.
 *
 * Реальная реализация:
 *  1) Добавить зависимость ai.picovoice:porcupine-android:3.x
 *  2) Получить access key на console.picovoice.ai
 *  3) Создать кастомный keyword (.ppn) в Picovoice Console
 *  4) Заменить flow на PorcupineManager с callback'ом detected -> emit(Unit)
 */
@Singleton
class WakeWordDetector @Inject constructor() {
    fun detections(): Flow<Unit> = flow { /* эмит при детекции */ }
    fun stop() {}
}
