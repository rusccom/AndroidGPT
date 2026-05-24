package com.androidgpt.features.telegram

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Заглушка клиента TDLib.
 *
 * Чтобы подключить реальный Telegram:
 *  1) Добавить в app/build.gradle.kts:
 *       implementation("org.drinkless:tdlib:1.8.x")
 *     (или собрать TDLib для Android по инструкции github.com/tdlib/td)
 *  2) В TdlibClient.start():
 *       Client.create(updatesHandler, ...) и хранить экземпляр
 *  3) Реализовать send(query) → suspend Result<TdApi.Object>
 *  4) Реализовать sendCheckAuthenticationCode, setTdlibParameters и т.д.
 *  5) Заполнить TelegramContacts.setCache(...) после GetContacts.
 */
@Singleton
class TdlibClient @Inject constructor() {

    enum class AuthState { LoggedOut, WaitPhone, WaitCode, WaitPassword, Ready }

    @Volatile var authState: AuthState = AuthState.LoggedOut
        private set

    fun start() { /* TDLib.create(...) */ }

    suspend fun submitPhone(phone: String) { authState = AuthState.WaitCode }

    suspend fun submitCode(code: String) { authState = AuthState.Ready }

    suspend fun submitPassword(password: String) { authState = AuthState.Ready }

    suspend fun logout() { authState = AuthState.LoggedOut }
}
