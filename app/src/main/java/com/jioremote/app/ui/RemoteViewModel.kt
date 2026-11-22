package com.jioremote.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jioremote.app.adb.AdbRemoteClient
import com.jioremote.app.adb.RemoteCommands
import com.jioremote.app.data.Channel
import com.jioremote.app.data.RemoteSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val ipv4 = Regex("""\d{1,3}(\.\d{1,3}){3}""")

internal fun validatedHost(raw: String): String {
    val host = raw.trim()
    val octets = host.split('.').mapNotNull(String::toIntOrNull)
    val isValidIpv4 = ipv4.matches(host) && octets.size == 4 && octets.all { it in 0..255 }
    val isTrustedLan = isValidIpv4 && (
        octets[0] == 10 ||
            (octets[0] == 172 && octets[1] in 16..31) ||
            (octets[0] == 192 && octets[1] == 168) ||
            (octets[0] == 169 && octets[1] == 254)
        )
    require(isTrustedLan) {
        "Enter a private local-network TV IPv4 address (for example 192.168.1.20)"
    }
    return host
}

internal fun validatedPort(raw: String): Int = raw.toIntOrNull()
    ?.takeIf { it in 1..65535 }
    ?: throw IllegalArgumentException("Port must be between 1 and 65535")

data class RemoteUiState(
    val host: String = "",
    val port: String = "5555",
    val pairingPort: String = "",
    val pairingCode: String = "",
    val channels: List<Channel> = emptyList(),
    val isBusy: Boolean = false,
    val verified: Boolean = false,
    val status: String = "Enter your TV IP and connect",
    val error: String? = null,
)

class RemoteViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = RemoteSettings(application)
    private val remote = AdbRemoteClient(application)
    private val mutableState = MutableStateFlow(
        RemoteUiState(host = settings.host, port = settings.port, channels = settings.loadChannels())
    )
    val state = mutableState.asStateFlow()

    fun setHost(value: String) = mutableState.update { it.copy(host = value, verified = false) }
    fun setPort(value: String) = mutableState.update { it.copy(port = value, verified = false) }
    fun setPairingPort(value: String) = mutableState.update { it.copy(pairingPort = value) }
    fun setPairingCode(value: String) = mutableState.update { it.copy(pairingCode = value) }
    fun clearMessage() = mutableState.update { it.copy(error = null) }

    private fun perform(task: suspend (String, Int) -> String) {
        if (mutableState.value.isBusy) return
        viewModelScope.launch {
            mutableState.update { it.copy(isBusy = true, error = null) }
            try {
                val snapshot = mutableState.value
                val host = validatedHost(snapshot.host)
                val port = validatedPort(snapshot.port)
                settings.saveConnection(host, port.toString())
                val status = task(host, port)
                mutableState.update { it.copy(verified = true, status = status) }
            } catch (e: Exception) {
                mutableState.update {
                    it.copy(verified = false, error = e.message ?: "Connection failed. Check TV IP and ADB authorization.")
                }
            } finally {
                mutableState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun connect() = perform { host, port ->
        val response = remote.run(host, port, RemoteCommands.probe)
        check(response.contains("jioremote-ready")) { "Unexpected TV response" }
        "Connection verified • $host:$port"
    }

    fun pair() {
        if (mutableState.value.isBusy) return
        viewModelScope.launch {
            mutableState.update { it.copy(isBusy = true, error = null) }
            try {
                val current = mutableState.value
                val host = validatedHost(current.host)
                val port = validatedPort(current.pairingPort)
                val code = current.pairingCode.trim()
                require(code.matches(Regex("""\d{6}"""))) { "Enter the 6-digit code shown on the TV" }
                remote.pair(host, port, code)
                mutableState.update {
                    it.copy(status = "Paired. Enter the TV connection port, then tap Connect.", pairingCode = "")
                }
            } catch (e: Exception) {
                mutableState.update { it.copy(error = e.message ?: "Pairing failed") }
            } finally {
                mutableState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun launch() = perform { host, port ->
        remote.run(host, port, RemoteCommands.launchApp)
        "JioTV+ launch command sent"
    }

    fun play(channel: Channel) = perform { host, port ->
        val output = remote.run(host, port, RemoteCommands.channel(channel.id))
        check("Error:" !in output && "Exception" !in output) { output }
        "Requested ${channel.name} • TV playback not verified by the app"
    }

    fun keyEvent(code: Int) = perform { host, port ->
        remote.run(host, port, RemoteCommands.key(code))
        "Remote key sent"
    }

    fun addChannel(name: String, id: String): String? {
        val candidate = try { Channel(id.trim(), name.trim()) }
        catch (e: Exception) { return e.message ?: "Invalid channel" }
        val current = mutableState.value.channels
        if (current.any { it.id == candidate.id }) return "Channel ID already exists"
        if (current.size >= 40) return "Maximum 40 channels"
        val revised = current + candidate
        settings.saveChannels(revised)
        mutableState.update { it.copy(channels = revised) }
        return null
    }

    fun removeChannel(id: String) {
        val revised = mutableState.value.channels.filterNot { it.id == id }
        settings.saveChannels(revised)
        mutableState.update { it.copy(channels = revised) }
    }
}
