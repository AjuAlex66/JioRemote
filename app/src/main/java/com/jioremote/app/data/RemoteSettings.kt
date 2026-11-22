package com.jioremote.app.data

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
        val raw = prefs.getString("channels", null) ?: return listOf(Channel("304189", "Sun TV"))
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val entry = array.getJSONObject(i)
                    runCatching { Channel(entry.getString("id"), entry.getString("name")) }
                        .getOrNull()?.let(::add)
                }
            }.distinctBy { it.id }
        } catch (_: Exception) {
            listOf(Channel("304189", "Sun TV"))
        }
    }

    fun saveChannels(channels: List<Channel>) {
        val array = JSONArray()
        channels.forEach { channel ->
            array.put(JSONObject().put("id", channel.id).put("name", channel.name))
        }
        prefs.edit { putString("channels", array.toString()) }
    }
}
