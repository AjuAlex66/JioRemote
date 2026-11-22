package com.jioremote.app.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

class RemoteSettings(context: Context) {
    private val prefs = context.getSharedPreferences("jioremote", Context.MODE_PRIVATE)
    val host: String get() = prefs.getString("host", "") ?: ""
    val port: String get() = prefs.getString("port", "5555") ?: "5555"

    fun saveConnection(host: String, port: String) {
        prefs.edit { putString("host", host).putString("port", port) }
    }

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
