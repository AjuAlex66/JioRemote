package com.jioremote.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.jioremote.app.data.Channel

@Composable
internal fun SettingsContent(state: RemoteUiState, viewModel: RemoteViewModel, onManage: () -> Unit) {
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
        HorizontalDivider()
        TextButton(onClick = onManage) { Text("Manage channels") }
    }
}

@Composable
internal fun ChannelManagement(channels: List<Channel>, onAdd: () -> Unit, onEdit: (Channel) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
    ) {
        item {
            Text("Manage channels", Modifier.padding(8.dp), style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onAdd) { Text("Add channel") }
        }
        items(channels, key = { it.key }) { channel ->
            ListItem(
                headlineContent = { Text(channel.name) },
                supportingContent = { Text(channel.id?.let { "Playback ID $it" } ?: "Playback ID required") },
                modifier = Modifier.clickable(onClickLabel = "Edit channel") { onEdit(channel) },
            )
        }
    }
}

@Composable
internal fun ChannelDialog(draft: ChannelDraft, viewModel: RemoteViewModel) {
    val title = when {
        draft.key == null -> "Add channel"
        draft.needsPlaybackId -> "Configure channel"
        else -> "Edit channel"
    }
    AlertDialog(
        onDismissRequest = viewModel::closeAddChannel,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Enter this channel’s verified JioTV+ playback ID.")
                RemoteField(draft.name, viewModel::setChannelName, "Channel name", KeyboardType.Text)
                RemoteField(draft.id, viewModel::setChannelId, "Playback ID", KeyboardType.Number)
                draft.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (draft.key != null) {
                    TextButton(onClick = viewModel::confirmRemove) {
                        Text("Remove channel", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = viewModel::saveChannel) { Text("Save") } },
        dismissButton = { TextButton(onClick = viewModel::closeAddChannel) { Text("Cancel") } },
    )
}

@Composable
private fun NoticeText(notice: RemoteNotice) {
    Text(notice.text, color = if (notice.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
}

@Composable
private fun RemoteField(
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
