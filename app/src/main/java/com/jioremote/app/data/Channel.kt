package com.jioremote.app.data

internal data class Channel(val key: String, val name: String, val id: String?) {
    init {
        require(id == null || id.matches(Regex("[0-9]{1,12}"))) { "Channel ID must contain only digits" }
        require(name.isNotBlank() && name.length <= 48) { "Channel name must be 1–48 characters" }
    }
}

internal enum class DefaultChannel(val key: String, val title: String, val verifiedId: String? = null) {
    AsianetHd("asianet_hd", "Asianet HD"),
    Flowers("flowers", "Flowers"),
    MazhavilHd("mazhavil_hd", "Mazhavil Manorama HD"),
    AsianetMoviesHd("asianet_movies_hd", "Asianet Movies HD"),
    AsianetPlus("asianet_plus", "Asianet Plus"),
    SunTv("sun_tv", "Sun TV", "304189");

    fun create() = Channel(key, title, verifiedId)
}

internal object ChannelCatalogue {
    const val version = 1

    fun load(saved: List<Channel>?, savedVersion: Int): List<Channel> {
        if (saved == null) return DefaultChannel.entries.map { it.create() }
        if (savedVersion >= version) return saved

        val matched = mutableSetOf<DefaultChannel>()
        val preserved = saved.map { channel ->
            val default = DefaultChannel.entries.firstOrNull {
                it !in matched && (it.title.equals(channel.name, ignoreCase = true) ||
                    (it.verifiedId != null && it.verifiedId == channel.id) ||
                    (it == DefaultChannel.Flowers && channel.name.equals("Flowers TV", ignoreCase = true)))
            }
            if (default == null) channel else {
                matched.add(default)
                channel.copy(key = default.key)
            }
        }
        // A legacy saved list without Sun TV means the user removed it.
        val newDefaults = DefaultChannel.entries.filter { it != DefaultChannel.SunTv && it !in matched }
        return preserved + newDefaults.map { it.create() }
    }
}
