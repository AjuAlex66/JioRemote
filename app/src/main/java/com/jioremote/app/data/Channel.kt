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

/**
 * Published JioTV+ channel numbers (not playback/deep-link IDs).
 * Only Asianet Movies HD / 1827 has been confirmed working on this user's TV.
 * All others should be treated as suggested values until tested on that device.
 * Never write legacyPlaybackId into Channel.number.
 */
internal enum class DefaultChannel(val key: String, val title: String, val channelNumber: String) {
    AsianetHd("asianet_hd", "Asianet HD", "1806"),
    Flowers("flowers", "Flowers", "555"),
    MazhavilHd("mazhavil_hd", "Mazhavil Manorama HD", "1818"),
    AsianetMoviesHd("asianet_movies_hd", "Asianet Movies HD", "1827"),
    AsianetPlus("asianet_plus", "Asianet Plus", "1809"),
    SunTv("sun_tv", "Sun TV", "1503");

    fun create() = Channel(key, title, channelNumber)
}

internal object ChannelCatalogue {
    const val version = 4

    // Exact keys used by the 63 additional v3 defaults. Custom rows have their own keys.
    internal val retiredV3Keys = setOf(
        "asianet_sd",
        "mazhavil_sd",
        "surya_tv_hd",
        "surya_tv_sd",
        "zee_keralam_hd",
        "zee_keralam_sd",
        "kairali_tv",
        "we_tv",
        "amrita_tv",
        "surya_movies",
        "asianet_movies_sd",
        "surya_comedy",
        "asianet_news",
        "news18_kerala",
        "manorama_news",
        "mathrubhumi_news",
        "kairali_news",
        "jaihind_tv",
        "jeevan_tv",
        "media_one",
        "janam_tv",
        "kaumudy_tv",
        "twenty_four_news",
        "reporter_tv",
        "safari_tv",
        "dd_malayalam",
        "surya_music",
        "raj_musix_malayalam",
        "kappa_tv",
        "goodness_tv",
        "shalom_tv",
        "harvest_tv",
        "kite_victers",
        "movies_now_hd",
        "mn_plus_hd",
        "mnx_hd",
        "and_flix_hd",
        "and_prive_hd",
        "star_movies_hd",
        "star_movies_select_hd",
        "animal_planet_hd",
        "animal_planet_hindi",
        "discovery_hd_world",
        "discovery_science",
        "national_geographic_hd",
        "nat_geo_wild_hd",
        "history_tv18_hd",
        "sun_tv_sd",
        "star_vijay_hd",
        "zee_tamil",
        "colors_tamil_hd",
        "ktv_hd",
        "ktv_sd",
        "vijay_super",
        "sun_music_hd",
        "sun_news_tamil",
        "polimer_news",
        "zee_thirai",
        "kalaignar_tv",
        "adithya_tv",
        "d_tamil",
        "puthiya_thalaimurai",
        "thanthi_tv",
    )

    fun load(saved: List<Channel>?, savedVersion: Int): List<Channel> {
        if (saved == null) return DefaultChannel.entries.map { it.create() }
        if (savedVersion >= version) return saved

        val retained = saved.filterNot { it.key in retiredV3Keys }
        return if (savedVersion == 0) upgradeLegacyCatalogue(retained) else retained
    }

    private fun upgradeLegacyCatalogue(saved: List<Channel>): List<Channel> {
        // Version 0 stored only Sun TV by playback ID and had no stable key for it.
        val sun = DefaultChannel.SunTv
        val hasSunKey = saved.any { it.key == sun.key }
        val retained = saved.map { channel ->
            if (!hasSunKey && channel.key == "legacy:304189" && channel.legacyPlaybackId == "304189") {
                channel.copy(key = sun.key)
            } else channel
        }
        val keys = retained.mapTo(mutableSetOf()) { it.key }
        val names = retained.mapTo(mutableSetOf()) { it.name.lowercase() }
        val numbers = retained.mapNotNullTo(mutableSetOf()) { it.number }

        // The other five defaults were introduced after v0. Keep any user-created equivalents.
        val additions = DefaultChannel.entries
            .filter {
                it != sun && it.key !in keys && it.title.lowercase() !in names &&
                    (it != DefaultChannel.Flowers || "flowers tv" !in names)
            }
            .map { Channel(it.key, it.title, it.channelNumber.takeIf(numbers::add)) }
        return retained + additions
    }
}
