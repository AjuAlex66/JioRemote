package com.jioremote.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jioremote.app.R
import com.jioremote.app.data.Channel
import com.jioremote.app.data.DefaultChannel

private enum class RemoteSheet { Settings, Channels }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RemoteScreen(state: RemoteUiState, viewModel: RemoteViewModel) {
    var sheet by rememberSaveable { mutableStateOf<RemoteSheet?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val notice = state.notice
    LaunchedEffect(notice, sheet) {
        if (notice != null && sheet == null && notice.operation !in setOf(RemoteOperation.Pair, RemoteOperation.Save)) {
            val settingsAction = notice.isError
            val result = snackbar.showSnackbar(
                notice.text,
                actionLabel = if (settingsAction) "Settings" else null,
                withDismissAction = settingsAction,
                duration = if (settingsAction) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) sheet = RemoteSheet.Settings
            else if (!notice.isError) viewModel.clearNotice(notice)
        }
    }
    LaunchedEffect(state.hostError, state.portError) {
        if (state.hostError != null || state.portError != null) sheet = RemoteSheet.Settings
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { safePadding ->
        Box(Modifier.fillMaxSize().padding(safePadding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 26.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item("header") {
                    ConnectionHeader(
                        state,
                        onAction = {
                            when {
                                state.connection == ConnectionStatus.Connected -> viewModel.disconnect()
                                state.host.isBlank() || state.hostError != null || state.portError != null ->
                                    { sheet = RemoteSheet.Settings }
                                else -> viewModel.connect()
                            }
                        },
                        onSettings = { sheet = RemoteSheet.Settings },
                    )
                }
                item("launch") {
                    Button(
                        onClick = viewModel::launch,
                        enabled = state.canSend,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(bottom = 3.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        RemoteIcon(RemoteSymbol.Play, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Open JioTV+", style = MaterialTheme.typography.titleMedium)
                    }
                }
                if (state.channels.isEmpty()) {
                    item("empty") {
                        TextButton(onClick = { sheet = RemoteSheet.Channels }) { Text("Manage channels") }
                    }
                }
                items(state.channels.chunked(3), key = { row -> "channel-${row.first().key}" }) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { channel ->
                            ChannelTile(channel, { viewModel.play(channel) }, { viewModel.openEditChannel(channel) }, Modifier.weight(1f))
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                item("remote") {
                    RemoteControls(state.canSend, viewModel::keyEvent, Modifier.padding(top = 5.dp))
                }
            }
            if (state.operation == RemoteOperation.Command) {
                LinearProgressIndicator(Modifier.align(Alignment.TopCenter).fillMaxWidth())
            }
        }
    }

    sheet?.let { active ->
        key(active) {
            ModalBottomSheet(
                onDismissRequest = { sheet = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                when (active) {
                    RemoteSheet.Settings -> SettingsContent(state, viewModel, onManage = { sheet = RemoteSheet.Channels })
                    RemoteSheet.Channels -> ChannelManagement(
                        state.channels,
                        onAdd = { sheet = null; viewModel.openAddChannel() },
                        onEdit = { channel -> sheet = null; viewModel.openEditChannel(channel) },
                    )
                }
            }
        }
    }

    state.channelDraft?.let { ChannelDialog(it, viewModel) }
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

@Composable
private fun ChannelTile(channel: Channel, onClick: () -> Unit, onEdit: () -> Unit, modifier: Modifier) {
    val logo = channelLogo(channel.key)
    val shape = MaterialTheme.shapes.medium
    Box(
        modifier.height(64.dp).clip(shape)
            .background(Color(0xFF242637))
            .semantics {
                contentDescription = channel.name
                if (channel.id == null) stateDescription = "Playback ID required"
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onEdit,
                onClickLabel = if (channel.id == null) "Configure playback" else "Open channel",
                onLongClickLabel = "Edit channel",
                role = Role.Button,
            )
            .padding(horizontal = 10.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (logo != null) {
            Image(
                painter = painterResource(logo),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            RemoteIcon(RemoteSymbol.Tv, Modifier.size(24.dp), tint = Color(0xFFC9CBD9))
        }
    }
}

@DrawableRes
private fun channelLogo(key: String): Int? = when (key) {
    DefaultChannel.AsianetHd.key -> R.drawable.logo_asianet_hd
    DefaultChannel.Flowers.key -> R.drawable.logo_flowers
    DefaultChannel.MazhavilHd.key -> R.drawable.logo_mazhavil_hd
    DefaultChannel.AsianetMoviesHd.key -> R.drawable.logo_asianet_movies_hd
    DefaultChannel.AsianetPlus.key -> R.drawable.logo_asianet_plus
    DefaultChannel.SunTv.key -> R.drawable.logo_sun_tv
    else -> null
}
