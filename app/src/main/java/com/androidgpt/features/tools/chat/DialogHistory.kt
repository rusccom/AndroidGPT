package com.androidgpt.features.tools.chat

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

data class DialogTurn(val user: String, val assistant: String)

@Singleton
class DialogHistory @Inject constructor() {
    private val turns = ArrayDeque<DialogTurn>()
    private val mutex = Mutex()

    suspend fun add(turn: DialogTurn) = mutex.withLock {
        turns.addLast(turn)
        while (turns.size > MAX_TURNS) turns.removeFirst()
    }

    suspend fun snapshot(): List<DialogTurn> = mutex.withLock { turns.toList() }

    suspend fun clear() = mutex.withLock { turns.clear() }

    companion object { const val MAX_TURNS = 5 }
}
