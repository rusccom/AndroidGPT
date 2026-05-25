package com.androidgpt.features.telegram.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidgpt.features.telegram.TdlibAuthState
import com.androidgpt.features.telegram.qr.QrEncoder

@Composable
fun TelegramAuthSection(vm: TelegramAuthViewModel = hiltViewModel()) {
    val state by vm.auth.collectAsStateWithLifecycle()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        AuthBody(state, vm)
        ControlRow(state, vm)
    }
}

@Composable
private fun AuthBody(state: TdlibAuthState, vm: TelegramAuthViewModel) {
    when (state) {
        TdlibAuthState.Idle -> Text("Не подключён. Нажми «Подключить» и отсканируй QR.")
        TdlibAuthState.Initializing -> Text("Инициализация TDLib…")
        TdlibAuthState.MissingApiCredentials ->
            Text("Заполни Telegram api_id и api_hash выше.", color = MaterialTheme.colorScheme.error)
        is TdlibAuthState.WaitQrScan -> QrPanel(state.link)
        TdlibAuthState.WaitPassword -> PasswordField(vm)
        TdlibAuthState.Ready -> Text("Подключено. Контакты загружаются.")
        TdlibAuthState.LoggedOut -> Text("Выход выполнен.")
        is TdlibAuthState.Error -> Text("Ошибка: ${state.message}", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun QrPanel(link: String) {
    val bmp = remember(link) { QrEncoder.encode(link, size = 600) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Открой Telegram → Настройки → Устройства → Подключить устройство и отсканируй:")
        if (bmp != null) {
            Image(bitmap = bmp.asImageBitmap(), contentDescription = "QR", modifier = Modifier.size(260.dp))
        } else {
            Text(link, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PasswordField(vm: TelegramAuthViewModel) {
    var pwd by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Введи облачный пароль 2FA:")
        OutlinedTextField(
            value = pwd, onValueChange = { pwd = it },
            label = { Text("Пароль") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { vm.submitPassword(pwd) }) { Text("Подтвердить") }
    }
}

@Composable
private fun ControlRow(state: TdlibAuthState, vm: TelegramAuthViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state is TdlibAuthState.Ready || state is TdlibAuthState.WaitQrScan ||
            state is TdlibAuthState.WaitPassword || state is TdlibAuthState.Initializing
        ) {
            OutlinedButton(onClick = vm::disconnect) { Text("Отключить") }
        } else {
            Button(onClick = vm::connect) { Text("Подключить") }
        }
    }
}
