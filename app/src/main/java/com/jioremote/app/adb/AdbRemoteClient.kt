package com.jioremote.app.adb

import android.content.Context
import com.flyfishxu.kadb.Kadb
import com.flyfishxu.kadb.cert.KadbCert
import com.flyfishxu.kadb.cert.OkioFilePrivateKeyStore
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
