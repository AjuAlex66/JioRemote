package com.jioremote.app.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.jioremote.app.R
import com.jioremote.app.data.Channel
import com.jioremote.app.data.ChannelLogoStore
import com.jioremote.app.data.DefaultChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class ChannelDrag(val key: String, val center: Float, val lastTarget: String? = null)

@Composable
internal fun ChannelList(state: RemoteUiState, viewModel: RemoteViewModel, modifier: Modifier = Modifier) {
    val list = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val edge = with(LocalDensity.current) { 72.dp.toPx() }
    var drag by remember { mutableStateOf<ChannelDrag?>(null) }

    fun moveAcrossRows() {
        val current = drag ?: return
        val visible = list.layoutInfo.visibleItemsInfo
        val dragged = visible.firstOrNull { it.key == current.key } ?: return
        val center = dragged.offset + dragged.size / 2f
        val target = if (current.center > center) {
            visible.lastOrNull { it.offset > dragged.offset && current.center >= it.offset + it.size / 2f }
        } else {
            visible.firstOrNull { it.offset < dragged.offset && current.center <= it.offset + it.size / 2f }
        }?.key as? String
        if (target != current.lastTarget) {
            drag = current.copy(lastTarget = target)
            if (target != null) viewModel.moveChannel(current.key, target)
        }
    }

    LaunchedEffect(drag?.key) {
        while (drag != null) {
            withFrameNanos { }
            val center = drag?.center ?: break
            val layout = list.layoutInfo
            val above = center - layout.viewportStartOffset
            val below = layout.viewportEndOffset - center
            val delta = when {
                above < edge -> -((edge - above) / edge).coerceIn(0f, 1f) * 20f
                below < edge -> ((edge - below) / edge).coerceIn(0f, 1f) * 20f
                else -> 0f
            }
            if (delta != 0f) list.scrollBy(delta)
            moveAcrossRows()
        }
    }
    DisposableEffect(viewModel) {
        onDispose { viewModel.finishChannelReorder(save = false) }
    }

    LazyColumn(
        modifier.fillMaxWidth(),
        state = list,
        contentPadding = PaddingValues(top = 2.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.channels.isEmpty()) item {
            Text("Add a channel to get started.", Modifier.padding(vertical = 12.dp))
        }
        itemsIndexed(state.channels, key = { _, channel -> channel.key }) { index, channel ->
            val isDragging = drag?.key == channel.key
            val placement = if (isDragging) Modifier.zIndex(1f).graphicsLayer {
                val current = drag
                val item = list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == channel.key }
                if (current != null && item != null) translationY = current.center - item.offset - item.size / 2f
            } else Modifier.animateItem()
            val actions = buildList {
                state.channels.getOrNull(index - 1)?.let { neighbor ->
                    add(CustomAccessibilityAction("Move up") { viewModel.moveChannel(channel.key, neighbor.key); true })
                }
                state.channels.getOrNull(index + 1)?.let { neighbor ->
                    add(CustomAccessibilityAction("Move down") { viewModel.moveChannel(channel.key, neighbor.key); true })
                }
            }
            ChannelRow(
                channel = channel,
                tuning = state.tuningChannelKey == channel.key,
                elevated = isDragging,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    viewModel.play(channel)
                },
                onEdit = { viewModel.openEditChannel(channel) },
                modifier = placement,
                handle = Modifier.semantics {
                    contentDescription = "Reorder ${channel.name}"
                    customActions = actions
                }.pointerInput(channel.key, list) {
                    detectDragGestures(
                        onDragStart = {
                            val item = list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == channel.key }
                            if (item != null && drag == null) {
                                drag = ChannelDrag(channel.key, item.offset + item.size / 2f)
                                viewModel.beginChannelReorder()
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        onDrag = { change, amount ->
                            val current = drag
                            if (current?.key == channel.key) {
                                change.consume()
                                drag = current.copy(center = current.center + amount.y)
                                moveAcrossRows()
                            }
                        },
                        onDragEnd = {
                            if (drag?.key == channel.key) {
                                viewModel.finishChannelReorder(save = true)
                                drag = null
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onDragCancel = {
                            if (drag?.key == channel.key) {
                                viewModel.finishChannelReorder(save = false)
                                drag = null
                            }
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun ChannelRow(
    channel: Channel,
    tuning: Boolean,
    elevated: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier,
    handle: Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (tuning || elevated) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = if (elevated) 6.dp else 0.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).heightIn(min = 84.dp).combinedClickable(
                    role = Role.Button,
                    onClickLabel = if (channel.number == null) "Set channel number" else "Tune channel",
                    onLongClickLabel = "Edit channel",
                    onClick = onClick,
                    onLongClick = onEdit,
                ).padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChannelArtwork(channel.key, channel.logoFile, Modifier.size(width = 62.dp, height = 54.dp))
                Column(Modifier.weight(1f)) {
                    Text(channel.name, style = MaterialTheme.typography.titleSmall)
                    if (channel.number != null) {
                        Text(channel.number, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Set channel number", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (tuning) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            }
            Box(handle.size(width = 48.dp, height = 64.dp), contentAlignment = Alignment.Center) {
                RemoteIcon(RemoteSymbol.Drag, Modifier.size(24.dp), MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChannelEditor(draft: ChannelDraft, viewModel: RemoteViewModel, confirmingDelete: Boolean) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        if (it != null) viewModel.chooseChannelLogo(it)
    }
    ModalBottomSheet(
        onDismissRequest = { if (!confirmingDelete) viewModel.closeAddChannel() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (draft.key == null) "Add channel" else "Edit channel", style = MaterialTheme.typography.headlineSmall)
            RemoteField(draft.name, viewModel::setChannelName, "Channel name", KeyboardType.Text)
            RemoteField(draft.number, viewModel::setChannelNumber, "Channel number", KeyboardType.Number,
                hint = "Use the number you enter on the TV remote.")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChannelArtwork(draft.key, draft.logoFile, Modifier.size(64.dp))
                Column {
                    TextButton(
                        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        enabled = !draft.importingLogo,
                    ) { Text(if (draft.importingLogo) "Importing…" else "Choose logo") }
                    if (draft.logoFile != null) {
                        TextButton(onClick = viewModel::restoreChannelLogo, enabled = !draft.importingLogo) {
                            Text(if (bundledLogo(draft.key) != null) "Restore bundled logo" else "Remove logo")
                        }
                    }
                }
            }
            draft.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = viewModel::saveChannel, enabled = !draft.importingLogo, modifier = Modifier.fillMaxWidth()) {
                Text("Save")
            }
            if (draft.key != null) {
                TextButton(onClick = viewModel::confirmRemove, enabled = !draft.importingLogo) {
                    Text("Delete channel", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun ChannelArtwork(key: String?, logo: String?, modifier: Modifier) {
    val context = LocalContext.current
    val custom by produceState<ImageBitmap?>(null, logo) {
        value = null
        if (logo != null) {
            value = withContext(Dispatchers.IO) {
                ChannelLogoStore(context.filesDir.resolve(ChannelLogoStore.directoryName)).file(logo)
                    ?.let { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() }
            }
        }
    }
    val bundled = bundledLogo(key)
    Box(
        modifier.clip(MaterialTheme.shapes.small).background(Color(0xFF242637)).padding(5.dp),
        contentAlignment = Alignment.Center,
    ) {
        val imported = custom
        when {
            imported != null -> Image(imported, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            bundled != null -> Image(painterResource(bundled), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            else -> RemoteIcon(RemoteSymbol.Tv, Modifier.size(25.dp), Color(0xFFC9CBD9))
        }
    }
}

private fun bundledLogo(key: String?): Int? = when (key) {
    DefaultChannel.AsianetHd.key -> R.drawable.logo_asianet_hd
    DefaultChannel.Flowers.key -> R.drawable.logo_flowers
    DefaultChannel.MazhavilHd.key -> R.drawable.logo_mazhavil_hd
    DefaultChannel.AsianetMoviesHd.key -> R.drawable.logo_asianet_movies_hd
    DefaultChannel.AsianetPlus.key -> R.drawable.logo_asianet_plus
    DefaultChannel.SunTv.key -> R.drawable.logo_sun_tv
    else -> null
}
