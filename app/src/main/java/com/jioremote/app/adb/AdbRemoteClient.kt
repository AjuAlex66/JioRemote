package com.jioremote.app.adb

import android.content.Context
import com.flyfishxu.kadb.Kadb
import com.flyfishxu.kadb.cert.KadbCert
import com.flyfishxu.kadb.cert.OkioFilePrivateKeyStore
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath

/** Runs ADB directly from the phone. A persistent key avoids reauthorizing every launch. */
class AdbRemoteClient(context: Context) {
    init {
        KadbCert.configure(
            store = OkioFilePrivateKeyStore(
                context.filesDir.resolve("adb-private-key.pem").absolutePath.toPath()
            )
        )
    }

    suspend fun pair(host: String, pairingPort: Int, code: String) = withContext(Dispatchers.IO) {
        Kadb.pair(host, pairingPort, code)
    }

    internal suspend fun power(host: String, port: Int, action: TvPowerAction): TvPowerResult = withContext(Dispatchers.IO) {
        var result: TvPowerResult? = null
        try {
            Kadb.create(host, port, connectTimeout = 5_000, socketTimeout = 8_000).use { adb ->
                if (!adb.supportsFeature("shell_v2")) return@use TvPowerResult.Unsupported
                adb.openShell().use { shell ->
                    dispatchTvPower(action, shell::write, shell::read).also { result = it }
                }
            }
        } catch (error: IOException) {
            // Closing the shell can race with the TV going offline after a result was received.
            result ?: throw error
        }
    }

    suspend fun run(host: String, port: Int, command: String): String = withContext(Dispatchers.IO) {
        Kadb.create(host, port, connectTimeout = 5_000, socketTimeout = 8_000).use { adb ->
            val result = adb.shell(command)
            check(result.exitCode == 0) {
                "TV command failed: ${result.errorOutput.ifBlank { result.output }}"
            }
            result.allOutput.trim()
        }
    }
}
