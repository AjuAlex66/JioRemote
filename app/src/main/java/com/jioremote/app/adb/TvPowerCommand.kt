package com.jioremote.app.adb

import com.flyfishxu.kadb.shell.AdbShellPacket
import java.io.EOFException
import java.net.SocketException

internal enum class TvPowerAction(val command: String) {
    Reboot("exec reboot\n"),
    PowerOff("exec reboot -p\n"),
}

internal enum class TvPowerResult { Dispatched, PermissionDenied, Unsupported, Rejected }

internal fun dispatchTvPower(
    action: TvPowerAction,
    write: (String) -> Unit,
    read: () -> AdbShellPacket,
): TvPowerResult {
    // Open an idle shell in the caller. Kadb can retry opening a stream, but this write is never retried.
    write(action.command)

    val output = StringBuilder()
    try {
        while (true) {
            when (val packet = read()) {
                is AdbShellPacket.Exit -> return rejectionFrom(output)
                    ?: if (packet.payload.single().toInt() == 0) TvPowerResult.Dispatched else TvPowerResult.Rejected
                is AdbShellPacket.StdOut, is AdbShellPacket.StdError -> {
                    val remaining = 2048 - output.length
                    if (remaining > 0) output.append(packet.payload.decodeToString().take(remaining))
                }
            }
        }
    } catch (_: EOFException) {
        // The shell can disappear after receiving the request, before sending its exit packet.
        return rejectionFrom(output) ?: TvPowerResult.Dispatched
    } catch (_: SocketException) {
        return rejectionFrom(output) ?: TvPowerResult.Dispatched
    }
}

private fun rejectionFrom(output: CharSequence): TvPowerResult? {
    val text = output.toString().lowercase()
    return when {
        listOf("permission denied", "not permitted", "access denied", "not allowed").any(text::contains) ->
            TvPowerResult.PermissionDenied
        listOf("not found", "unsupported", "not supported", "unknown option", "invalid option", "usage:").any(text::contains) ->
            TvPowerResult.Unsupported
        "failed" in text || "error:" in text -> TvPowerResult.Rejected
        else -> null
    }
}
