package com.jioremote.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RemoteScreen(state: RemoteUiState, viewModel: RemoteViewModel) {
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val notice = state.notice
    LaunchedEffect(notice, settingsOpen, state.channelDraft != null) {
        if (notice != null && !settingsOpen && state.channelDraft == null &&
            notice.operation !in setOf(RemoteOperation.Pair, RemoteOperation.Save)
        ) {
            val offerSettings = notice.isError && state.connection == ConnectionStatus.Error
            val result = snackbar.showSnackbar(
                notice.text,
                actionLabel = if (offerSettings) "Settings" else null,
                withDismissAction = notice.isError,
                duration = if (notice.isError) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) settingsOpen = true
            else if (!offerSettings) viewModel.clearNotice(notice)
        }
    }
    LaunchedEffect(state.hostError, state.portError) {
        if (state.hostError != null || state.portError != null) settingsOpen = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { VolumeBar(state.canSend, viewModel::keyEvent) },
    ) { safePadding ->
        Box(Modifier.fillMaxSize().padding(safePadding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 480.dp).fillMaxSize().padding(horizontal = 16.dp),
            ) {
                ConnectionHeader(
                    state,
                    onAction = {
                        when {
                            state.connection == ConnectionStatus.Connected -> viewModel.disconnect()
                            state.host.isBlank() || state.hostError != null || state.portError != null -> settingsOpen = true
                            else -> viewModel.connect()
                        }
                    },
                    onSettings = { settingsOpen = true },
                )
                Button(
                    onClick = viewModel::launch,
                    enabled = state.canSend,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    RemoteIcon(RemoteSymbol.Play, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open JioTV+", style = MaterialTheme.typography.titleMedium)
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Channels", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = viewModel::openAddChannel) { Text("Add channel") }
                }
                ChannelList(state, viewModel, Modifier.weight(1f))
            }
        }
    }

    if (settingsOpen) {
        ModalBottomSheet(
            onDismissRequest = { settingsOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) { SettingsContent(state, viewModel) }
    }

    state.channelDraft?.let { ChannelEditor(it, viewModel, state.removingChannel != null) }
    state.removingChannel?.let { channel ->
        AlertDialog(
            onDismissRequest = viewModel::cancelRemove,
            title = { Text("Delete ${channel.name}?") },
            text = { Text(state.channelDraft?.error ?: "This removes the channel from this phone.") },
            confirmButton = { TextButton(onClick = viewModel::removeChannel) { Text("Delete") } },
            dismissButton = { TextButton(onClick = viewModel::cancelRemove) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ConnectionHeader(state: RemoteUiState, onAction: () -> Unit, onSettings: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val (status, dot) = when (state.connection) {
        ConnectionStatus.Loading -> "Loading TV settings" to colors.outline
        ConnectionStatus.Disconnected -> "Disconnected" to colors.outline
        ConnectionStatus.Connecting -> "Connecting" to Color(0xFFDA982E)
        ConnectionStatus.Connected -> "Connected" to Color(0xFF36B478)
        ConnectionStatus.Error -> "Connection error" to colors.error
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).semantics(mergeDescendants = true) {
                stateDescription = status
                liveRegion = LiveRegionMode.Polite
            },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RemoteIcon(RemoteSymbol.Tv, Modifier.size(25.dp), tint = colors.onSurface)
            Text("Jio Remote", Modifier.weight(1f, fill = false), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(Modifier.size(8.dp).background(dot, CircleShape))
        }
        TextButton(
            onClick = onAction,
            enabled = !state.isBusy,
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Text(when (state.connection) {
                ConnectionStatus.Connecting -> "Connecting…"
                ConnectionStatus.Connected -> "Disconnect"
                else -> "Connect"
            }, maxLines = 1)
        }
        IconButton(onClick = onSettings, modifier = Modifier.size(48.dp).semantics { contentDescription = "Settings" }) {
            RemoteIcon(RemoteSymbol.Settings, Modifier.size(23.dp))
        }
    }
}
