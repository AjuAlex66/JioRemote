package com.jioremote.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jioremote.app.data.Channel

@Composable
internal fun RemoteScreen(state: RemoteUiState, viewModel: RemoteViewModel) {
    val snackbar = remember { SnackbarHostState() }
    val commandNotice = state.notice?.takeIf { it.operation == RemoteOperation.Command && !it.isError }
    LaunchedEffect(commandNotice) {
        commandNotice?.let {
            snackbar.showSnackbar(it.text, withDismissAction = true)
            viewModel.clearNotice(it)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { safePadding ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(safePadding).imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            val fontScale = LocalDensity.current.fontScale
            val columns = when {
                maxWidth < 340.dp || fontScale > 1.4f -> 1
                maxWidth >= 570.dp && fontScale <= 1.15f -> 3
                else -> 2
            }
            LazyColumn(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item("header") { RemoteHeader() }
                item("connection") { ConnectionCard(state, viewModel) }
                item("jio-tv") {
                    Button(
                        onClick = viewModel::launch,
                        enabled = state.canSend,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        PlayIcon(Modifier.size(16.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Open JioTV+", style = MaterialTheme.typography.titleMedium)
                    }
                }
                item("remote") { RemoteControls(state, viewModel::keyEvent) }
                item("channels-label") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Channels", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        TextButton(onClick = viewModel::openAddChannel, enabled = state.connection != ConnectionStatus.Loading) {
                            Text("+ Add channel")
                        }
                    }
                    if (state.channels.isEmpty() && state.connection != ConnectionStatus.Loading) {
                        Text("Add a channel using its JioTV+ channel ID.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(state.channels.chunked(columns), key = { row -> "channel-${row.first().id}" }) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { channel ->
                            ChannelCard(
                                channel = channel,
                                enabled = state.canSend,
                                onPlay = { viewModel.play(channel) },
                                onRemove = { viewModel.confirmRemove(channel) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }

    state.channelDraft?.let { AddChannelDialog(it, viewModel) }
    state.removingChannel?.let { channel ->
        AlertDialog(
            onDismissRequest = viewModel::cancelRemove,
            title = { Text("Remove ${channel.name}?") },
            text = { Text("This removes the channel from this phone.") },
            confirmButton = { TextButton(onClick = viewModel::removeChannel) { Text("Remove") } },
            dismissButton = { TextButton(onClick = viewModel::cancelRemove) { Text("Cancel") } },
        )
    }
}

@Composable
private fun RemoteHeader() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
            val ink = MaterialTheme.colorScheme.onPrimaryContainer
            Canvas(Modifier.padding(12.dp).size(28.dp)) {
                val stroke = 2.dp.toPx()
                drawRoundRect(
                    ink, topLeft = Offset(stroke, stroke),
                    size = Size(size.width - 2 * stroke, size.height * 0.67f),
                    cornerRadius = CornerRadius(4.dp.toPx()), style = Stroke(stroke),
                )
                drawLine(
                    ink, Offset(size.width * 0.32f, size.height * 0.92f),
                    Offset(size.width * 0.68f, size.height * 0.92f), stroke, StrokeCap.Round,
                )
            }
        }
        Column {
            Text("Jio Remote", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Android TV remote", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ConnectionCard(state: RemoteUiState, viewModel: RemoteViewModel) {
    var showPairing by rememberSaveable { mutableStateOf(false) }
    val connected = state.connection == ConnectionStatus.Connected
    val loading = state.connection == ConnectionStatus.Loading
    val notice = state.notice?.takeIf { it.operation != RemoteOperation.Pair && (it.operation != RemoteOperation.Command || it.isError) }
    val colors = MaterialTheme.colorScheme
    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(containerColor = colors.surfaceContainerLow),
        border = BorderStroke(1.dp, colors.outlineVariant),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("TV connection", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            ConnectionBadge(state.connection)
            if (loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            } else if (connected) {
                Text("${state.host}:${state.port}", style = MaterialTheme.typography.bodyLarge)
                OutlinedButton(
                    onClick = viewModel::disconnect,
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text("Disconnect") }
            } else {
                ConnectionField(
                    state.host, viewModel::setHost, "TV IP address", state.hostError, !state.isBusy,
                    KeyboardType.Decimal, placeholder = "192.168.1.20",
                )
                ConnectionField(
                    state.port, viewModel::setPort, "ADB connection port", state.portError, !state.isBusy,
                    KeyboardType.Number, hint = "Use the port on the TV’s Wireless debugging screen, or 5555 for classic ADB.",
                )
                Button(
                    onClick = viewModel::connect,
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    if (state.operation == RemoteOperation.Connect) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (state.operation == RemoteOperation.Connect) "Connecting…" else "Connect")
                }
                OutlinedButton(
                    onClick = viewModel::saveConnection,
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text(if (state.operation == RemoteOperation.Save) "Saving…" else "Save settings") }
                notice?.let { InlineNotice(it) }
                HorizontalDivider(color = colors.outlineVariant)
                TextButton(onClick = { showPairing = !showPairing }, enabled = !state.isBusy) {
                    Text(if (showPairing) "Hide wireless pairing" else "Pair with a six-digit code")
                }
                if (showPairing) {
                    PairingForm(state, viewModel)
                }
            }
        }
    }
}

@Composable
private fun ConnectionBadge(status: ConnectionStatus) {
    val colors = MaterialTheme.colorScheme
    val (label, background, foreground) = when (status) {
        ConnectionStatus.Loading -> Triple("Loading settings", colors.surfaceContainerHigh, colors.onSurfaceVariant)
        ConnectionStatus.Disconnected -> Triple("Disconnected", colors.surfaceContainerHigh, colors.onSurfaceVariant)
        ConnectionStatus.Connecting -> Triple("Connecting", colors.primaryContainer, colors.onPrimaryContainer)
        ConnectionStatus.Connected -> Triple("Connected", colors.secondaryContainer, colors.onSecondaryContainer)
        ConnectionStatus.Error -> Triple("Connection error", colors.errorContainer, colors.onErrorContainer)
    }
    Surface(color = background, contentColor = foreground, shape = CircleShape) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(7.dp).background(foreground, CircleShape))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PairingForm(state: RemoteUiState, viewModel: RemoteViewModel) {
    Text(
        "On an Android 11+ TV, open Wireless debugging and choose Pair device with pairing code.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    ConnectionField(
        state.pairingPort, viewModel::setPairingPort, "Pairing port", state.pairingPortError,
        !state.isBusy, KeyboardType.Number, hint = "Use the temporary port shown with the pairing code.",
    )
    ConnectionField(
        state.pairingCode, viewModel::setPairingCode, "Six-digit pairing code", state.pairingCodeError,
        !state.isBusy, KeyboardType.NumberPassword, secret = true,
    )
    FilledTonalButton(
        onClick = viewModel::pair, enabled = !state.isBusy,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) { Text(if (state.operation == RemoteOperation.Pair) "Pairing…" else "Pair phone with TV") }
    state.notice?.takeIf { it.operation == RemoteOperation.Pair }?.let { InlineNotice(it) }
}

@Composable
private fun InlineNotice(notice: RemoteNotice) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (notice.isError) colors.errorContainer else colors.secondaryContainer,
        contentColor = if (notice.isError) colors.onErrorContainer else colors.onSecondaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(notice.text, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ConnectionField(
    value: String, onChange: (String) -> Unit, label: String, error: String?, enabled: Boolean,
    keyboardType: KeyboardType, hint: String? = null, placeholder: String? = null, secret: Boolean = false,
) {
    val supportingText = error ?: hint
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = supportingText?.let { { Text(it) } },
        isError = error != null,
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun RemoteControls(state: RemoteUiState, onKey: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(containerColor = colors.surfaceContainerLow),
        border = BorderStroke(1.dp, colors.outlineVariant),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Remote controls", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (state.operation == RemoteOperation.Command) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            Surface(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                shape = CircleShape,
                color = colors.surfaceContainer,
                border = BorderStroke(1.dp, colors.outlineVariant),
            ) {
                Column(
                    Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    DirectionKey("↑", "Up", 19, state.canSend, onKey)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        DirectionKey("←", "Left", 21, state.canSend, onKey)
                        FilledIconButton(
                            onClick = { onKey(23) }, enabled = state.canSend,
                            modifier = Modifier.size(60.dp),
                        ) { Text("OK", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                        DirectionKey("→", "Right", 22, state.canSend, onKey)
                    }
                    DirectionKey("↓", "Down", 20, state.canSend, onKey)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RemoteKey("Back", 4, state.canSend, onKey, Modifier.weight(1f))
                RemoteKey("Home", 3, state.canSend, onKey, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RemoteKey("Volume −", 25, state.canSend, onKey, Modifier.weight(1f))
                RemoteKey("Volume +", 24, state.canSend, onKey, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DirectionKey(symbol: String, label: String, code: Int, enabled: Boolean, onKey: (Int) -> Unit) {
    FilledTonalIconButton(
        onClick = { onKey(code) }, enabled = enabled,
        modifier = Modifier.size(52.dp).semantics { contentDescription = label },
    ) { Text(symbol, style = MaterialTheme.typography.titleLarge, modifier = Modifier.clearAndSetSemantics { }) }
}

@Composable
private fun RemoteKey(label: String, code: Int, enabled: Boolean, onKey: (Int) -> Unit, modifier: Modifier) {
    FilledTonalButton(
        onClick = { onKey(code) }, enabled = enabled, modifier = modifier.heightIn(min = 50.dp),
        shape = MaterialTheme.shapes.small,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
    ) { Text(label, textAlign = TextAlign.Center) }
}

@Composable
private fun ChannelCard(channel: Channel, enabled: Boolean, onPlay: () -> Unit, onRemove: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(modifier) {
        OutlinedCard(
            onClick = onPlay,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors(
                containerColor = colors.surfaceContainerLow,
                disabledContainerColor = colors.surfaceContainerLow,
            ),
            border = BorderStroke(1.dp, colors.outlineVariant),
        ) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val iconColor = if (enabled) colors.primaryContainer else colors.surfaceContainer
                val ink = if (enabled) colors.onPrimaryContainer else colors.onSurfaceVariant
                Surface(shape = CircleShape, color = iconColor, contentColor = ink) {
                    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) { PlayIcon(Modifier.size(13.dp)) }
                }
                Text(channel.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd).semantics { contentDescription = "Remove ${channel.name}" },
        ) {
            Text("×", style = MaterialTheme.typography.titleLarge, modifier = Modifier.clearAndSetSemantics { })
        }
    }
}

@Composable
private fun PlayIcon(modifier: Modifier) {
    val color = LocalContentColor.current
    Canvas(modifier) {
        drawPath(Path().apply {
            moveTo(size.width * 0.18f, 0f)
            lineTo(size.width, size.height / 2)
            lineTo(size.width * 0.18f, size.height)
            close()
        }, color)
    }
}

@Composable
private fun AddChannelDialog(draft: ChannelDraft, viewModel: RemoteViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::closeAddChannel,
        title = { Text("Add channel") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Enter a JioTV+ channel ID you’ve verified on your TV.")
                OutlinedTextField(
                    value = draft.name, onValueChange = viewModel::setChannelName,
                    label = { Text("Channel name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                ConnectionField(draft.id, viewModel::setChannelId, "Channel ID", null, true, KeyboardType.Number)
                draft.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = viewModel::addChannel) { Text("Add") } },
        dismissButton = { TextButton(onClick = viewModel::closeAddChannel) { Text("Cancel") } },
    )
}
