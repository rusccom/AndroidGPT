package com.androidgpt.features.telegram

import javax.inject.Inject
import javax.inject.Singleton

data class TgContact(
    val userId: Long,
    val displayName: String,
    val username: String?,
    val phone: String?,
)

/**
 * Контакты Telegram.
 *
 * Сейчас возвращает пустой список. Когда подключите TDLib:
 *   1) реализовать loadContacts() через TdApi.GetContacts + TdApi.GetUser
 *   2) кешировать в Room
 *   3) метод search должен искать по подстроке в displayName и username, без учёта регистра
 */
@Singleton
class TelegramContacts @Inject constructor() {
    @Volatile private var cache: List<TgContact> = emptyList()

    fun setCache(items: List<TgContact>) { cache = items }
    fun all(): List<TgContact> = cache

    fun search(query: String): List<TgContact> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return cache.filter {
            it.displayName.lowercase().contains(q) ||
                it.username?.lowercase()?.contains(q) == true
        }.sortedBy { it.displayName.length }
    }
}
