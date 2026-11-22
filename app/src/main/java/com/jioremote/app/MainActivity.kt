package com.jioremote.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.jioremote.app.data.Channel
import com.jioremote.app.ui.RemoteUiState
import com.jioremote.app.ui.RemoteViewModel

private val Ink = Color(0xFF081A22)
private val Panel = Color(0xFF152D35)
private val Accent = Color(0xFF82E7BD)

class MainActivity : ComponentActivity() {
    private val viewModel: RemoteViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.state.collectAsState()
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Accent,
                    onPrimary = Ink,
                    background = Ink,
                    surface = Panel,
                    onSurface = Color.White,
                )
            ) {
                RemoteScreen(state, viewModel)
            }
        }
    }
}

@Composable
private fun RemoteScreen(s: RemoteUiState, vm: RemoteViewModel) {
    var showPairing by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<Channel?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().background(Ink)
            .verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Text("Jio Remote", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Control JioTV+ on your Android TV over Wi-Fi", color = Color.LightGray)

        Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("TV CONNECTION", color = Accent, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = s.host, onValueChange = vm::setHost,
                    label = { Text("TV IPv4 address") }, placeholder = { Text("192.168.1.20") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = s.port, onValueChange = vm::setPort,
                    label = { Text("ADB connection port") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Button(onClick = vm::connect, enabled = !s.isBusy, modifier = Modifier.fillMaxWidth()) {
                    Text(if (s.isBusy) "Working…" else "Connect / Test TV")
                }
                Text(s.status, color = if (s.verified) Accent else Color.LightGray)
                TextButton(onClick = { showPairing = !showPairing }) {
                    Text(if (showPairing) "Hide wireless pairing" else "Pair with 6-digit code (Android 11+ TV)")
                }
                if (showPairing) {
                    Text(
                        "On your TV, open Developer options → Wireless debugging → Pair device with pairing code. " +
                            "The pairing port is DIFFERENT from the connection port.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = s.pairingPort, onValueChange = vm::setPairingPort,
                        label = { Text("Pairing port") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = s.pairingCode, onValueChange = vm::setPairingCode,
                        label = { Text("6-digit pairing code") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedButton(onClick = vm::pair, enabled = !s.isBusy, modifier = Modifier.fillMaxWidth()) {
                        Text("Pair phone with TV")
                    }
                }
            }
        }

        if (s.error != null) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF602D32))) {
                Column(Modifier.padding(14.dp)) {
                    Text("Connection / command error", fontWeight = FontWeight.Bold)
                    Text(s.error, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = vm::clearMessage) { Text("Dismiss") }
                }
            }
        }

        Button(onClick = vm::launch, enabled = !s.isBusy, modifier = Modifier.fillMaxWidth()) {
            Text("▶  Open JioTV+")
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Channels", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { showAdd = true }) { Text("+ Add channel") }
        }
        if (s.channels.isEmpty()) {
            Text("Add a channel using its JioTV+ numeric channel ID", color = Color.LightGray)
        }
        s.channels.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { channel ->
                    ChannelTile(
                        channel = channel,
                        enabled = !s.isBusy,
                        onClick = { vm.play(channel) },
                        onRemove = { removing = channel },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        Text("TV remote", style = MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RemoteButton("Home", !s.isBusy, { vm.keyEvent(3) }, Modifier.weight(1f))
            RemoteButton("Back", !s.isBusy, { vm.keyEvent(4) }, Modifier.weight(1f))
            RemoteButton("Vol −", !s.isBusy, { vm.keyEvent(25) }, Modifier.weight(1f))
            RemoteButton("Vol +", !s.isBusy, { vm.keyEvent(24) }, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            RemoteButton("↑", !s.isBusy, { vm.keyEvent(19) })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            RemoteButton("←", !s.isBusy, { vm.keyEvent(21) })
            Spacer(Modifier.width(8.dp))
            RemoteButton("OK", !s.isBusy, { vm.keyEvent(23) })
            Spacer(Modifier.width(8.dp))
            RemoteButton("→", !s.isBusy, { vm.keyEvent(22) })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            RemoteButton("↓", !s.isBusy, { vm.keyEvent(20) })
        }
        Text(
            "ADB must be enabled on your trusted local network. The phone needs its own TV authorization. " +
                "Channel playback depends on JioTV+ and your account.",
            style = MaterialTheme.typography.bodySmall, color = Color.LightGray
        )
        Spacer(Modifier.height(16.dp))
    }

    if (showAdd) AddChannelDialog(
        onDismiss = { showAdd = false },
        onAdd = { name, id -> vm.addChannel(name, id) },
    )
    removing?.let { channel ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("Remove ${channel.name}?") },
            text = { Text("The channel will be removed from this phone only.") },
            confirmButton = { TextButton(onClick = { vm.removeChannel(channel.id); removing = null }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ChannelTile(
    channel: Channel, enabled: Boolean, onClick: () -> Unit, onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(onClick = onClick, enabled = enabled, modifier = modifier.height(128.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text("◉", color = Accent, style = MaterialTheme.typography.headlineMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(channel.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("ID ${channel.id}", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = onRemove) { Text("×") }
            }
        }
    }
}

@Composable
private fun RemoteButton(label: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier) { Text(label) }
}

@Composable
private fun AddChannelDialog(onDismiss: () -> Unit, onAdd: (String, String) -> String?) {
    var name by remember { mutableStateOf("") }
    var id by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add JioTV+ channel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Use a channel ID verified on your TV. IDs may vary between versions.")
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Channel name") }, singleLine = true)
                OutlinedTextField(value = id, onValueChange = { id = it }, label = { Text("Numeric channel ID") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = onAdd(name, id)
                if (error == null) onDismiss()
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
