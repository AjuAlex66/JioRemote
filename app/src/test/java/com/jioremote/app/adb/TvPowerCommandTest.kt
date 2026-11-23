package com.jioremote.app.adb

import com.flyfishxu.kadb.shell.AdbShellPacket
import java.io.EOFException
import java.net.SocketException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TvPowerCommandTest {
    @Test fun writesExactlyOneValidatedCommandForEachAction() {
        val sent = mutableListOf<String>()
        for (action in TvPowerAction.entries) {
            assertEquals(TvPowerResult.Dispatched, dispatchTvPower(action, { sent += it }) {
                AdbShellPacket.Exit(byteArrayOf(0))
            })
        }
        assertEquals(listOf("exec reboot\n", "exec reboot -p\n"), sent)
    }

    @Test fun distinguishesPermissionUnsupportedAndNonzeroExitFailures() {
        assertEquals(
            TvPowerResult.PermissionDenied,
            readPackets(AdbShellPacket.StdError("reboot: Permission denied".encodeToByteArray()), AdbShellPacket.Exit(byteArrayOf(1))),
        )
        assertEquals(
            TvPowerResult.Unsupported,
            readPackets(AdbShellPacket.StdOut("reboot: invalid option -p".encodeToByteArray()), AdbShellPacket.Exit(byteArrayOf(1))),
        )
        assertEquals(TvPowerResult.Rejected, readPackets(AdbShellPacket.Exit(byteArrayOf(1))))
    }

    @Test fun connectionLossAfterWriteCountsAsDispatchForEitherAction() {
        for (action in TvPowerAction.entries) {
            assertEquals(TvPowerResult.Dispatched, dispatchTvPower(action, {}) { throw EOFException() })
            assertEquals(TvPowerResult.Dispatched, dispatchTvPower(action, {}) { throw SocketException("Connection reset") })
        }
    }

    @Test fun preservesRejectionEvenWhenConnectionClosesBeforeExitPacket() {
        assertEquals(
            TvPowerResult.PermissionDenied,
            readPackets(AdbShellPacket.StdError("Operation not permitted".encodeToByteArray())),
        )
    }

    @Test fun neverReportsOrRetriesDispatchWhenWriteWasNotAcknowledged() {
        var writes = 0
        assertThrows(EOFException::class.java) {
            dispatchTvPower(TvPowerAction.PowerOff, {
                writes++
                throw EOFException()
            }) { error("A failed write must not wait for a reply") }
        }
        assertEquals(1, writes)
    }

    private fun readPackets(vararg packets: AdbShellPacket): TvPowerResult {
        val input = packets.iterator()
        return dispatchTvPower(TvPowerAction.PowerOff, {}) {
            if (input.hasNext()) input.next() else throw EOFException()
        }
    }
}
