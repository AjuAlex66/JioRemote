package com.jioremote.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingsContent(state: RemoteUiState, viewModel: RemoteViewModel) {
    var pairingOpen by rememberSaveable { mutableStateOf(false) }
    val connected = state.connection == ConnectionStatus.Connected
    val editable = !state.isBusy && !connected
    Column(
        Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Remote settings", style = MaterialTheme.typography.headlineSmall)
        state.notice?.takeIf { it.isError || it.operation == RemoteOperation.Save }?.let {
            NoticeText(it)
        }
        if (connected) {
            TextButton(onClick = viewModel::disconnect, enabled = !state.isBusy) { Text("Disconnect to edit TV settings") }
        }
        RemoteField(state.host, viewModel::setHost, "TV IP address", KeyboardType.Decimal, editable, state.hostError)
        RemoteField(
            state.port, viewModel::setPort, "ADB connection port", KeyboardType.Number, editable,
            state.portError, "Use 5555 for classic ADB or the port shown on the TV’s Wireless debugging screen.",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = viewModel::saveConnection, enabled = editable, modifier = Modifier.weight(1f)) {
                Text(if (state.operation == RemoteOperation.Save) "Saving…" else "Save settings")
            }
            Button(onClick = viewModel::connect, enabled = editable, modifier = Modifier.weight(1f)) {
                Text(if (state.operation == RemoteOperation.Connect) "Connecting…" else "Connect")
            }
        }
        HorizontalDivider()
        TextButton(onClick = { pairingOpen = !pairingOpen }) {
            Text(if (pairingOpen) "Hide wireless pairing" else "Wireless pairing")
        }
        if (pairingOpen) {
            Text("On the TV, open Wireless debugging and choose Pair device with pairing code.", style = MaterialTheme.typography.bodyMedium)
            RemoteField(
                state.pairingPort, viewModel::setPairingPort, "Pairing port", KeyboardType.Number, editable,
                state.pairingPortError, "Use the temporary port displayed with the six-digit code.",
            )
            RemoteField(
                state.pairingCode, viewModel::setPairingCode, "Six-digit pairing code", KeyboardType.NumberPassword,
                editable, state.pairingCodeError, secret = true,
            )
            FilledTonalButton(onClick = viewModel::pair, enabled = editable) {
                Text(if (state.operation == RemoteOperation.Pair) "Pairing…" else "Pair phone with TV")
            }
            state.notice?.takeIf { it.operation == RemoteOperation.Pair && !it.isError }?.let { NoticeText(it) }
        }
    }
}

@Composable
private fun NoticeText(notice: RemoteNotice) {
    Text(notice.text, color = if (notice.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
}

@Composable
internal fun RemoteField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    keyboard: KeyboardType,
    enabled: Boolean = true,
    error: String? = null,
    hint: String? = null,
    secret: Boolean = false,
) {
    val support = error ?: hint
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        supportingText = support?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        isError = error != null,
        enabled = enabled,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
