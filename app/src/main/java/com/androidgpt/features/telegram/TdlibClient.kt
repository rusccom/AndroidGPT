package com.androidgpt.features.telegram

import android.content.Context
import com.androidgpt.features.settings.ApiKey
import com.androidgpt.features.settings.ApiKeysRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Обёртка над TDLib (com.github.tdlibx:td 1.8.56). QR-логин:
 *  1) start() создаёт Client и слушает UpdateAuthorizationState
 *  2) WaitTdlibParameters → SetTdlibParameters
 *  3) WaitPhoneNumber → RequestQrCodeAuthentication
 *  4) WaitOtherDeviceConfirmation → emits WaitQrScan(link) — UI рисует QR
 *  5) Ready
 */
@Singleton
class TdlibClient @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val keys: ApiKeysRepository,
    private val contacts: TelegramContacts,
) {
    private val _auth = MutableStateFlow<TdlibAuthState>(TdlibAuthState.Idle)
    val auth: StateFlow<TdlibAuthState> = _auth.asStateFlow()

    @Volatile private var client: Client? = null

    fun start() {
        if (client != null) return
        _auth.value = TdlibAuthState.Initializing
        client = Client.create(
            Client.ResultHandler { obj -> handleUpdate(obj) },
            null,
            null,
        )
    }

    fun stop() {
        client?.send(TdApi.Close(), null)
        client = null
        _auth.value = TdlibAuthState.LoggedOut
    }

    fun submitPassword(password: String) {
        client?.send(
            TdApi.CheckAuthenticationPassword(password),
            Client.ResultHandler { result ->
                if (result is TdApi.Error) _auth.value = TdlibAuthState.Error("password: ${result.message}")
            },
        )
    }

    private fun handleUpdate(obj: TdApi.Object) {
        if (obj !is TdApi.UpdateAuthorizationState) return
        when (val s = obj.authorizationState) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> sendParameters()
            is TdApi.AuthorizationStateWaitPhoneNumber -> requestQr()
            is TdApi.AuthorizationStateWaitOtherDeviceConfirmation ->
                _auth.value = TdlibAuthState.WaitQrScan(s.link)
            is TdApi.AuthorizationStateWaitPassword -> _auth.value = TdlibAuthState.WaitPassword
            is TdApi.AuthorizationStateReady -> onReady()
            is TdApi.AuthorizationStateLoggingOut -> _auth.value = TdlibAuthState.LoggedOut
            is TdApi.AuthorizationStateClosed -> { client = null; _auth.value = TdlibAuthState.LoggedOut }
            else -> Unit
        }
    }

    private fun sendParameters() {
        val apiId = keys.get(ApiKey.TelegramApiId).toIntOrNull()
        val apiHash = keys.get(ApiKey.TelegramApiHash)
        if (apiId == null || apiHash.isBlank()) {
            _auth.value = TdlibAuthState.MissingApiCredentials
            return
        }
        val params = TdApi.SetTdlibParameters().apply {
            databaseDirectory = File(ctx.filesDir, "tdlib").absolutePath
            filesDirectory = File(ctx.filesDir, "tdlib_files").absolutePath
            useMessageDatabase = true
            useSecretChats = false
            this.apiId = apiId
            this.apiHash = apiHash
            systemLanguageCode = "ru"
            deviceModel = android.os.Build.MODEL
            systemVersion = android.os.Build.VERSION.RELEASE
            applicationVersion = "1.0"
        }
        client?.send(params, Client.ResultHandler { res ->
            if (res is TdApi.Error) _auth.value = TdlibAuthState.Error("params: ${res.message}")
        })
    }

    private fun requestQr() {
        client?.send(
            TdApi.RequestQrCodeAuthentication(LongArray(0)),
            Client.ResultHandler { res ->
                if (res is TdApi.Error) _auth.value = TdlibAuthState.Error("qr: ${res.message}")
            },
        )
    }

    private fun onReady() {
        _auth.value = TdlibAuthState.Ready
        client?.send(TdApi.GetContacts(), Client.ResultHandler { res ->
            if (res is TdApi.Users) loadUsers(res.userIds)
        })
    }

    private fun loadUsers(ids: LongArray) {
        val acc = mutableListOf<TgContact>()
        ids.forEach { id ->
            client?.send(TdApi.GetUser(id), Client.ResultHandler { res ->
                if (res is TdApi.User) {
                    acc += TgContact(
                        userId = res.id,
                        displayName = listOf(res.firstName, res.lastName)
                            .filter { it.isNotBlank() }.joinToString(" "),
                        username = res.usernames?.activeUsernames?.firstOrNull(),
                        phone = res.phoneNumber.takeIf { it.isNotBlank() },
                    )
                    if (acc.size == ids.size) contacts.setCache(acc.toList())
                }
            })
        }
    }
}
