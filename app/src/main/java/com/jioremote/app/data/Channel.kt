package com.jioremote.app.data

private val channelDigits = Regex("[0-9]{1,12}")

internal fun requireChannelNumber(number: String) {
    require(channelDigits.matches(number)) { "Enter a channel number with 1–12 digits." }
}

internal data class Channel(
    val key: String,
    val name: String,
    val number: String?,
    val logoFile: String? = null,
    val legacyPlaybackId: String? = null,
) {
    init {
        number?.let(::requireChannelNumber)
        require(name.isNotBlank() && name.length <= 48) { "Channel name must be 1–48 characters." }
    }
}

internal enum class DefaultChannel(val key: String, val title: String, val channelNumber: String? = null) {
    AsianetHd("asianet_hd", "Asianet HD"),
    Flowers("flowers", "Flowers"),
    MazhavilHd("mazhavil_hd", "Mazhavil Manorama HD"),
    AsianetMoviesHd("asianet_movies_hd", "Asianet Movies HD", "1827"),
    AsianetPlus("asianet_plus", "Asianet Plus"),
    SunTv("sun_tv", "Sun TV");

    fun create() = Channel(key, title, channelNumber)
}

internal object ChannelCatalogue {
    const val version = 2

    fun load(saved: List<Channel>?, savedVersion: Int): List<Channel> {
        if (saved == null) return DefaultChannel.entries.map { it.create() }
        if (savedVersion >= version) return saved

        val existing = if (savedVersion == 0) upgradeLegacyCatalogue(saved) else saved
        return existing.map {
            if (it.key == DefaultChannel.AsianetMoviesHd.key &&
                it.number == null && it.legacyPlaybackId == null
            ) it.copy(number = DefaultChannel.AsianetMoviesHd.channelNumber) else it
        }
    }

    private fun upgradeLegacyCatalogue(saved: List<Channel>): List<Channel> {
        val matched = mutableSetOf<DefaultChannel>()
        val preserved = saved.map { channel ->
            val default = DefaultChannel.entries.firstOrNull {
                it !in matched && (it.title.equals(channel.name, ignoreCase = true) ||
                    (it == DefaultChannel.SunTv && channel.legacyPlaybackId == "304189") ||
                    (it == DefaultChannel.Flowers && channel.name.equals("Flowers TV", ignoreCase = true)))
            }
            if (default == null) channel else {
                matched.add(default)
                channel.copy(key = default.key)
            }
        }
        // Sun TV was the only default before version 1. Honor its deletion.
        return preserved + DefaultChannel.entries.filter { it != DefaultChannel.SunTv && it !in matched }.map { it.create() }
    }
}
