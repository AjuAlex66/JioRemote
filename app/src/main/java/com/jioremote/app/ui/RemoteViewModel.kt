package com.jioremote.app.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jioremote.app.adb.AdbRemoteClient
import com.jioremote.app.adb.RemoteCommands
import com.jioremote.app.data.Channel
import com.jioremote.app.data.RemoteSettings
import com.jioremote.app.data.TvConnection
import com.jioremote.app.data.TvConnectionStore
import kotlinx.coroutines.CancellationException
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
internal enum class RemoteOperation { Save, Connect, Pair, Command }

internal data class RemoteNotice(
    val operation: RemoteOperation?,
    val text: String,
    val isError: Boolean = false,
)

internal data class ChannelDraft(
    val key: String? = null,
    val name: String = "",
    val id: String = "",
    val needsPlaybackId: Boolean = false,
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
    val removingChannel: Channel? = null,
) {
    val isBusy: Boolean get() = connection == ConnectionStatus.Loading || operation != null
    val canSend: Boolean get() = !isBusy && connection == ConnectionStatus.Connected
}

internal class RemoteViewModel(
    private val connectionStore: TvConnectionStore,
    loadChannels: () -> List<Channel>,
    private val saveChannels: (List<Channel>) -> Unit,
    private val runCommand: suspend (String, Int, String) -> String,
    private val pairDevice: suspend (String, Int, String) -> Unit,
) : ViewModel() {
    private val mutableState = MutableStateFlow(RemoteUiState(channels = loadChannels()))
    val state = mutableState.asStateFlow()

    init {
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
        val id = channel.id
        when {
            id == null -> openEditChannel(channel)
            state.value.connection != ConnectionStatus.Connected -> mutableState.update {
                val notice = it.notice?.takeIf(RemoteNotice::isError)
                    ?: RemoteNotice(null, "Connect to your TV to open ${channel.name}.")
                it.copy(notice = notice)
            }
            else -> sendCommand({ RemoteCommands.channel(id) }, "Requested ${channel.name} on your TV.")
        }
    }
    fun keyEvent(code: Int) = sendCommand({ RemoteCommands.key(code) }, null)

    private fun sendCommand(command: () -> String, success: String?) {
        val current = state.value
        if (!current.canSend) return
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
                connection = if (operation == RemoteOperation.Connect) ConnectionStatus.Connecting else it.connection,
                notice = null,
            )
        }
        viewModelScope.launch {
            try {
                val message = task()
                mutableState.update {
                    it.copy(notice = message?.let { RemoteNotice(operation, it) })
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                val message = when (operation) {
                    RemoteOperation.Save -> "Couldn’t save TV settings. Try again."
                    RemoteOperation.Connect -> "Couldn’t verify the TV. Check its IP and connection port, then approve this phone on the TV."
                    RemoteOperation.Pair -> "Pairing failed. Open a new pairing prompt on the TV and use its port and code."
                    RemoteOperation.Command -> "The TV command failed. Check the TV and its ADB authorization, then reconnect."
                }
                mutableState.update {
                    it.copy(
                        connection = when (operation) {
                            RemoteOperation.Connect, RemoteOperation.Command -> ConnectionStatus.Error
                            else -> it.connection
                        },
                        notice = RemoteNotice(operation, message, true),
                    )
                }
            } finally {
                mutableState.update {
                    it.copy(
                        operation = null,
                        connection = if (it.connection == ConnectionStatus.Connecting) {
                            ConnectionStatus.Disconnected
                        } else it.connection,
                    )
                }
            }
        }
    }

    fun openAddChannel() = mutableState.update { it.copy(channelDraft = ChannelDraft()) }
    fun openEditChannel(channel: Channel) = mutableState.update {
        it.copy(channelDraft = ChannelDraft(channel.key, channel.name, channel.id.orEmpty(), needsPlaybackId = channel.id == null))
    }
    fun closeAddChannel() = mutableState.update { it.copy(channelDraft = null) }
    fun setChannelName(name: String) = editDraft { it.copy(name = name, error = null) }
    fun setChannelId(id: String) = editDraft { it.copy(id = id, error = null) }

    private fun editDraft(transform: (ChannelDraft) -> ChannelDraft) {
        mutableState.update { it.copy(channelDraft = it.channelDraft?.let(transform)) }
    }

    fun saveChannel() {
        val current = state.value
        val draft = current.channelDraft ?: return
        val candidate = try {
            val id = draft.id.trim().ifBlank { null }
            require(id != null) { "Enter a verified JioTV+ channel ID." }
            Channel(draft.key ?: "custom:${java.util.UUID.randomUUID()}", draft.name.trim(), id)
        } catch (e: IllegalArgumentException) {
            editDraft { it.copy(error = e.message) }
            return
        }
        val error = when {
            current.channels.any { it.id == candidate.id && it.key != candidate.key } -> "That channel ID already exists."
            draft.key == null && current.channels.size >= 40 -> "You can save up to 40 channels."
            else -> null
        }
        if (error != null) {
            editDraft { it.copy(error = error) }
            return
        }
        val revised = if (draft.key == null) current.channels + candidate
        else current.channels.map { if (it.key == draft.key) candidate else it }
        updateChannels(revised)
    }

    fun confirmRemove() = mutableState.update { current ->
        val channel = current.channels.firstOrNull { it.key == current.channelDraft?.key }
        current.copy(channelDraft = null, removingChannel = channel)
    }
    fun cancelRemove() = mutableState.update { it.copy(removingChannel = null) }

    fun removeChannel() {
        val current = state.value
        val channel = current.removingChannel ?: return
        updateChannels(current.channels.filterNot { it.key == channel.key })
    }

    private fun updateChannels(channels: List<Channel>) {
        saveChannels(channels)
        mutableState.update { it.copy(channels = channels, channelDraft = null, removingChannel = null) }
    }

    companion object {
        val factory = viewModelFactory {
            initializer {
                val application: Application = checkNotNull(this[APPLICATION_KEY])
                val settings = RemoteSettings(application)
                val remote = AdbRemoteClient(application)
                RemoteViewModel(settings.connection, settings::loadChannels, settings::saveChannels, remote::run, remote::pair)
            }
        }
    }
}
