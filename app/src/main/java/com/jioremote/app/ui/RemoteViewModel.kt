package com.jioremote.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jioremote.app.adb.AdbRemoteClient
import com.jioremote.app.adb.RemoteCommands
import com.jioremote.app.adb.TvPowerAction
import com.jioremote.app.adb.TvPowerResult
import com.jioremote.app.data.Channel
import com.jioremote.app.data.ChannelLogoStore
import com.jioremote.app.data.RemoteSettings
import com.jioremote.app.data.TvConnection
import com.jioremote.app.data.TvConnectionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val privateIpv4 = Regex("""[0-9]{1,3}(\.[0-9]{1,3}){3}""")
private val pairingPattern = Regex("[0-9]{6}")

internal fun validatedHost(raw: String): String {
    val host = raw.trim()
    val octets = host.split('.').mapNotNull(String::toIntOrNull)
    val isValidIpv4 = privateIpv4.matches(host) && octets.size == 4 && octets.all { it in 0..255 }
    require(isValidIpv4 && (
        octets[0] == 10 ||
            (octets[0] == 172 && octets[1] in 16..31) ||
            (octets[0] == 192 && octets[1] == 168) ||
            (octets[0] == 169 && octets[1] == 254)
        )
    ) { "Enter a private TV IPv4 address, such as 192.168.1.20." }
    return octets.joinToString(".")
}

internal fun validatedPort(raw: String): Int = raw.trim().toIntOrNull()
    ?.takeIf { it in 1..65535 }
    ?: throw IllegalArgumentException("Enter a port from 1 to 65535.")

internal fun validatedPairingCode(raw: String): String = raw.trim().also {
    require(pairingPattern.matches(it)) { "Enter the six digits shown on your TV." }
}

internal enum class ConnectionStatus { Loading, Disconnected, Connecting, Connected, Error }
internal enum class RemoteOperation { Save, Connect, Pair, Command, Power }

internal data class RemoteNotice(
    val operation: RemoteOperation?,
    val text: String,
    val isError: Boolean = false,
)

internal data class ChannelDraft(
    val key: String? = null,
    val name: String = "",
    val number: String = "",
    val logoFile: String? = null,
    val importingLogo: Boolean = false,
    val error: String? = null,
)

internal data class RemoteUiState(
    val host: String = "",
    val port: String = TvConnection().port,
    val pairingPort: String = "",
    val pairingCode: String = "",
    val hostError: String? = null,
    val portError: String? = null,
    val pairingPortError: String? = null,
    val pairingCodeError: String? = null,
    val channels: List<Channel> = emptyList(),
    val connection: ConnectionStatus = ConnectionStatus.Loading,
    val operation: RemoteOperation? = null,
    val notice: RemoteNotice? = null,
    val channelDraft: ChannelDraft? = null,
    val tuningChannelKey: String? = null,
    val removingChannel: Channel? = null,
    val pendingPowerAction: TvPowerAction? = null,
) {
    val isBusy: Boolean get() = connection == ConnectionStatus.Loading || operation != null
    val canSend: Boolean get() = !isBusy && pendingPowerAction == null && connection == ConnectionStatus.Connected
}

internal class RemoteViewModel(
    private val connectionStore: TvConnectionStore,
    loadChannels: () -> List<Channel>,
    private val saveChannels: (List<Channel>) -> Unit,
    private val runCommand: suspend (String, Int, String) -> String,
    private val pairDevice: suspend (String, Int, String) -> Unit,
    private val logoStore: ChannelLogoStore? = null,
    private val powerCommand: suspend (String, Int, TvPowerAction) -> TvPowerResult = { _, _, _ -> TvPowerResult.Unsupported },
    private val importLogo: (suspend (Uri) -> String)? = null,
) : ViewModel() {
    private val mutableState = MutableStateFlow(RemoteUiState(channels = loadChannels()))
    val state = mutableState.asStateFlow()
    private var reorderOrigin: List<Channel>? = null
    private var logoRequest: Any? = null

    init {
        logoStore?.removeUnused(state.value.channels.mapNotNull { it.logoFile }.toSet())
        viewModelScope.launch {
            try {
                val saved = connectionStore.load()
                mutableState.update {
                    it.copy(host = saved.host, port = saved.port, connection = ConnectionStatus.Disconnected)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(
                        connection = ConnectionStatus.Error,
                        notice = RemoteNotice(null, "Couldn’t load TV settings. Re-enter the address and port.", true),
                    )
                }
            }
        }
    }

    fun setHost(value: String) = editEndpoint { it.copy(host = value, hostError = null) }
    fun setPort(value: String) = editEndpoint { it.copy(port = value, portError = null) }
    fun setPairingPort(value: String) = editPairing { it.copy(pairingPort = value, pairingPortError = null) }
    fun setPairingCode(value: String) = editPairing { it.copy(pairingCode = value, pairingCodeError = null) }

    private fun editEndpoint(transform: (RemoteUiState) -> RemoteUiState) {
        mutableState.update {
            if (it.isBusy || it.connection == ConnectionStatus.Connected) it
            else transform(it).copy(connection = ConnectionStatus.Disconnected, notice = null)
        }
    }

    private fun editPairing(transform: (RemoteUiState) -> RemoteUiState) {
        mutableState.update {
            if (it.isBusy || it.connection == ConnectionStatus.Connected) it
            else transform(it).copy(notice = it.notice?.takeUnless { notice -> notice.operation == RemoteOperation.Pair })
        }
    }

    fun clearNotice(notice: RemoteNotice) {
        mutableState.update { if (it.notice === notice) it.copy(notice = null) else it }
    }

    fun disconnect() {
        if (!state.value.canSend) return
        mutableState.update { it.copy(connection = ConnectionStatus.Disconnected, notice = null) }
    }

    fun saveConnection() = withEndpoint(RemoteOperation.Save) { endpoint ->
        persist(endpoint)
        "TV settings saved."
    }

    fun connect() = withEndpoint(RemoteOperation.Connect) { endpoint ->
        persist(endpoint)
        val response = runCommand(endpoint.host, endpoint.port.toInt(), RemoteCommands.probe)
        check(response == "jioremote-ready")
        mutableState.update { it.copy(connection = ConnectionStatus.Connected) }
        null
    }

    fun pair() {
        val current = state.value
        if (current.isBusy || current.connection == ConnectionStatus.Connected) return
        val host = runCatching { validatedHost(current.host) }
        val port = runCatching { validatedPort(current.pairingPort) }
        val code = runCatching { validatedPairingCode(current.pairingCode) }
        mutableState.update {
            it.copy(
                hostError = host.exceptionOrNull()?.message,
                pairingPortError = port.exceptionOrNull()?.message,
                pairingCodeError = code.exceptionOrNull()?.message,
                notice = null,
            )
        }
        if (host.isFailure || port.isFailure || code.isFailure) return

        perform(RemoteOperation.Pair) {
            mutableState.update { it.copy(pairingCode = "") }
            pairDevice(host.getOrThrow(), port.getOrThrow(), code.getOrThrow())
            "Paired. Enter the connection port from the Wireless debugging screen, then tap Connect."
        }
    }

    fun launch() = sendCommand({ RemoteCommands.launchApp }, "JioTV+ launch requested.")
    fun play(channel: Channel) {
        val number = channel.number
        when {
            number == null -> openEditChannel(channel)
            state.value.connection != ConnectionStatus.Connected -> mutableState.update {
                val notice = it.notice?.takeIf(RemoteNotice::isError)
                    ?: RemoteNotice(null, "Connect to your TV to open ${channel.name}.")
                it.copy(notice = notice)
            }
            else -> sendCommand({ RemoteCommands.channel(number) }, null, channel.key)
        }
    }
    fun keyEvent(code: Int) = sendCommand({ RemoteCommands.key(code) }, null)

    fun requestPowerAction(action: TvPowerAction) {
        if (state.value.canSend) mutableState.update { it.copy(pendingPowerAction = action) }
    }

    fun cancelPowerAction() {
        mutableState.update { it.copy(pendingPowerAction = null) }
    }

    fun confirmPowerAction(action: TvPowerAction) {
        val current = state.value
        if (current.pendingPowerAction != action || current.isBusy || current.connection != ConnectionStatus.Connected) return

        perform(RemoteOperation.Power) {
            val request = if (action == TvPowerAction.Reboot) "reboot" else "power-off"
            val result = powerCommand(current.host, current.port.toInt(), action)
            val message = when (result) {
                TvPowerResult.Dispatched -> when (action) {
                    TvPowerAction.Reboot -> "Reboot request sent. Reconnect once the TV is back."
                    TvPowerAction.PowerOff -> "Power-off request sent. You may need the physical remote to turn the TV on."
                }
                TvPowerResult.PermissionDenied -> "The TV denied permission for the $request command."
                TvPowerResult.Unsupported -> "The TV does not support the $request command over ADB."
                TvPowerResult.Rejected -> "The TV rejected the $request command."
            }
            val dispatched = result == TvPowerResult.Dispatched
            mutableState.update {
                it.copy(
                    connection = if (dispatched) ConnectionStatus.Disconnected else it.connection,
                    notice = RemoteNotice(RemoteOperation.Power, message, isError = !dispatched),
                )
            }
            null
        }
    }

    private fun sendCommand(command: () -> String, success: String?, channelKey: String? = null) {
        val current = state.value
        if (!current.canSend) return
        mutableState.update { it.copy(tuningChannelKey = channelKey) }
        perform(RemoteOperation.Command) {
            val output = runCommand(current.host, current.port.toInt(), command())
            check("Error:" !in output && "Exception" !in output)
            success
        }
    }

    private suspend fun persist(endpoint: TvConnection) {
        connectionStore.save(endpoint)
        mutableState.update { it.copy(host = endpoint.host, port = endpoint.port) }
    }

    private fun withEndpoint(operation: RemoteOperation, task: suspend (TvConnection) -> String?) {
        val current = state.value
        if (current.isBusy || current.connection == ConnectionStatus.Connected) return
        val host = runCatching { validatedHost(current.host) }
        val port = runCatching { validatedPort(current.port) }
        mutableState.update {
            it.copy(hostError = host.exceptionOrNull()?.message, portError = port.exceptionOrNull()?.message, notice = null)
        }
        if (host.isFailure || port.isFailure) return
        perform(operation) { task(TvConnection(host.getOrThrow(), port.getOrThrow().toString())) }
    }

    private fun perform(operation: RemoteOperation, task: suspend () -> String?) {
        mutableState.update {
            it.copy(
                operation = operation,
                pendingPowerAction = if (operation == RemoteOperation.Power) null else it.pendingPowerAction,
                connection = if (operation == RemoteOperation.Connect) ConnectionStatus.Connecting else it.connection,
                notice = null,
            )
        }
        viewModelScope.launch {
            try {
                val message = task()
                if (message != null) mutableState.update { it.copy(notice = RemoteNotice(operation, message)) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                val message = when (operation) {
                    RemoteOperation.Save -> "Couldn’t save TV settings. Try again."
                    RemoteOperation.Connect -> "Couldn’t verify the TV. Check its IP and connection port, then approve this phone on the TV."
                    RemoteOperation.Pair -> "Pairing failed. Open a new pairing prompt on the TV and use its port and code."
                    RemoteOperation.Command -> "The TV command failed. Check the TV and its ADB authorization, then reconnect."
                    RemoteOperation.Power -> "Couldn’t confirm that the TV power request was sent. Reconnect to check the TV."
                }
                mutableState.update {
                    it.copy(
                        connection = when (operation) {
                            RemoteOperation.Connect, RemoteOperation.Command, RemoteOperation.Power -> ConnectionStatus.Error
                            else -> it.connection
                        },
                        notice = RemoteNotice(operation, message, true),
                    )
                }
            } finally {
                mutableState.update {
                    it.copy(
                        operation = null,
                        tuningChannelKey = null,
                        connection = if (it.connection == ConnectionStatus.Connecting) {
                            ConnectionStatus.Disconnected
                        } else it.connection,
                    )
                }
            }
        }
    }

    fun openAddChannel() = openEditor(ChannelDraft())

    fun openEditChannel(channel: Channel) = openEditor(
        ChannelDraft(channel.key, channel.name, channel.number.orEmpty(), channel.logoFile)
    )

    private fun openEditor(draft: ChannelDraft) {
        if (reorderOrigin != null || state.value.channelDraft != null) return
        mutableState.update { it.copy(channelDraft = draft) }
    }

    fun closeAddChannel() {
        val stagedLogo = state.value.channelDraft?.logoFile
        logoRequest = null
        mutableState.update { it.copy(channelDraft = null) }
        deleteIfUnused(stagedLogo)
    }

    fun setChannelName(name: String) = editDraft { it.copy(name = name, error = null) }
    fun setChannelNumber(number: String) = editDraft { it.copy(number = number, error = null) }

    private fun editDraft(transform: (ChannelDraft) -> ChannelDraft) {
        mutableState.update { it.copy(channelDraft = it.channelDraft?.let(transform)) }
    }

    fun chooseChannelLogo(uri: Uri) {
        val importer = importLogo ?: return
        val draft = state.value.channelDraft ?: return
        if (draft.importingLogo) return
        val request = Any()
        logoRequest = request
        editDraft { it.copy(importingLogo = true, error = null) }
        viewModelScope.launch {
            val file = try {
                importer(uri)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (logoRequest === request) {
                    editDraft { it.copy(importingLogo = false, error = "Couldn’t import that image.") }
                }
                return@launch
            }
            if (logoRequest !== request) {
                deleteIfUnused(file)
                return@launch
            }
            editDraft { it.copy(logoFile = file, importingLogo = false) }
            deleteIfUnused(draft.logoFile)
        }
    }

    fun restoreChannelLogo() {
        val draft = state.value.channelDraft ?: return
        if (draft.importingLogo) return
        editDraft { it.copy(logoFile = null, error = null) }
        deleteIfUnused(draft.logoFile)
    }

    fun saveChannel() {
        val current = state.value
        val draft = current.channelDraft ?: return
        if (draft.importingLogo) return
        val existing = current.channels.firstOrNull { it.key == draft.key }
        val candidate = try {
            val number = draft.number.ifEmpty { null }
            Channel(
                draft.key ?: "custom:${java.util.UUID.randomUUID()}",
                draft.name.trim(), number, draft.logoFile, existing?.legacyPlaybackId,
            )
        } catch (e: IllegalArgumentException) {
            editDraft { it.copy(error = e.message) }
            return
        }
        val error = when {
            candidate.number != null && current.channels.any { it.number == candidate.number && it.key != candidate.key } ->
                "That channel number already exists."
            draft.key == null && current.channels.size >= 200 -> "You can save up to 200 channels."
            else -> null
        }
        if (error != null) {
            editDraft { it.copy(error = error) }
            return
        }
        val revised = if (existing == null) current.channels + candidate
        else current.channels.map { if (it.key == candidate.key) candidate else it }
        if (persistChannels(revised)) {
            mutableState.update { it.copy(channelDraft = null) }
            deleteIfUnused(existing?.logoFile)
        }
    }

    fun confirmRemove() = mutableState.update { current ->
        current.copy(removingChannel = current.channels.firstOrNull { it.key == current.channelDraft?.key })
    }

    fun cancelRemove() = mutableState.update { it.copy(removingChannel = null) }

    fun removeChannel() {
        val current = state.value
        val channel = current.removingChannel ?: return
        if (persistChannels(current.channels.filterNot { it.key == channel.key })) {
            closeAddChannel()
            mutableState.update { it.copy(removingChannel = null) }
            deleteIfUnused(channel.logoFile)
        }
    }

    fun beginChannelReorder() {
        if (reorderOrigin == null) reorderOrigin = state.value.channels
    }

    fun moveChannel(key: String, targetKey: String) {
        val channels = state.value.channels
        val from = channels.indexOfFirst { it.key == key }
        val to = channels.indexOfFirst { it.key == targetKey }
        if (from < 0 || to < 0 || from == to) return
        val reordered = channels.toMutableList().apply { add(to, removeAt(from)) }.toList()
        if (reorderOrigin != null) mutableState.update { it.copy(channels = reordered) }
        else persistChannels(reordered)
    }

    fun finishChannelReorder(save: Boolean) {
        val original = reorderOrigin ?: return
        reorderOrigin = null
        val reordered = state.value.channels
        if (!save || (reordered != original && !persistChannels(reordered))) {
            mutableState.update { it.copy(channels = original) }
        }
    }

    private fun persistChannels(channels: List<Channel>): Boolean {
        return try {
            saveChannels(channels)
            mutableState.update { it.copy(channels = channels) }
            true
        } catch (_: Exception) {
            mutableState.update {
                val message = "Couldn’t save channels. Try again."
                if (it.channelDraft != null) it.copy(channelDraft = it.channelDraft.copy(error = message))
                else it.copy(notice = RemoteNotice(null, message, isError = true))
            }
            false
        }
    }

    private fun deleteIfUnused(name: String?) {
        if (name != null && state.value.channels.none { it.logoFile == name } &&
            state.value.channelDraft?.logoFile != name
        ) logoStore?.delete(name)
    }

    companion object {
        val factory = viewModelFactory {
            initializer {
                val application: Application = checkNotNull(this[APPLICATION_KEY])
                val settings = RemoteSettings(application)
                val remote = AdbRemoteClient(application)
                val logos = ChannelLogoStore(application.filesDir.resolve(ChannelLogoStore.directoryName))
                RemoteViewModel(settings.connection, settings::loadChannels, settings::saveChannels, remote::run, remote::pair, logos, remote::power) { uri ->
                    withContext(Dispatchers.IO) { logos.importPhoto(application.contentResolver, uri) }
                }
            }
        }
    }
}
