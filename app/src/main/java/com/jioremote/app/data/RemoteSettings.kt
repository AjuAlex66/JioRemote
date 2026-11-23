package com.jioremote.app.data

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private const val LEGACY_SETTINGS = "jioremote"

private val Context.tvSettings by preferencesDataStore(
    name = "tv_connection",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, LEGACY_SETTINGS, setOf("host", "port")))
    },
)

internal data class TvConnection(val host: String = "", val port: String = "5555")

internal class TvConnectionStore(private val dataStore: DataStore<Preferences>) {
    suspend fun load(): TvConnection {
        val values = dataStore.data.first()
        return TvConnection(values[hostKey].orEmpty(), values[portKey] ?: TvConnection().port)
    }

    suspend fun save(connection: TvConnection) {
        dataStore.edit {
            it[hostKey] = connection.host
            it[portKey] = connection.port
        }
    }

    private companion object {
        val hostKey = stringPreferencesKey("host")
        val portKey = stringPreferencesKey("port")
    }
}

internal class RemoteSettings(context: Context) {
    val connection = TvConnectionStore(context.tvSettings)
    private val prefs = context.getSharedPreferences(LEGACY_SETTINGS, Context.MODE_PRIVATE)

    fun loadChannels(): List<Channel> {
        val version = prefs.getInt("channels_catalogue_version", 0)
        val saved = prefs.getString("channels", null)?.let { decodeChannels(it, version) }
        return ChannelCatalogue.load(saved, version).also {
            if (version < ChannelCatalogue.version) saveChannels(it)
        }
    }

    private fun decodeChannels(raw: String, version: Int): List<Channel>? {
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return null
        return buildList {
            for (i in 0 until array.length()) {
                val entry = array.optJSONObject(i) ?: continue
                val legacy = entry.optString(if (version < 2) "id" else "legacyPlaybackId").ifBlank { null }
                val key = entry.optString("key").ifBlank { "legacy:${legacy ?: i}" }
                val number = if (version < 2) null else entry.optString("number").ifBlank { null }
                val logo = entry.optString("logoFile").ifBlank { null }
                runCatching { Channel(key, entry.getString("name"), number, logo, legacy) }.getOrNull()?.let(::add)
            }
        }.distinctBy { it.key }
    }

    // Confirm the new references reached disk before removing replaced logo files.
    @SuppressLint("UseKtx")
    fun saveChannels(channels: List<Channel>) {
        val array = JSONArray()
        channels.forEach { channel ->
            array.put(JSONObject()
                .put("key", channel.key)
                .put("name", channel.name)
                .put("number", channel.number)
                .put("logoFile", channel.logoFile)
                .put("legacyPlaybackId", channel.legacyPlaybackId))
        }
        check(prefs.edit().putString("channels", array.toString())
            .putInt("channels_catalogue_version", ChannelCatalogue.version).commit()) {
            "Couldn’t save channels."
        }
    }
}
