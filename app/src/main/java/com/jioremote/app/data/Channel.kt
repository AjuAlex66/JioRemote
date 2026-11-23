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
    SunTv("sun_tv", "Sun TV", "1503"),
    AsianetSd("asianet_sd", "Asianet (SD)", "1807"),
    MazhavilSd("mazhavil_sd", "Mazhavil Manorama (SD)", "1819"),
    SuryaTvHd("surya_tv_hd", "Surya TV HD", "1802"),
    SuryaTvSd("surya_tv_sd", "Surya TV", "1803"),
    ZeeKeralamHd("zee_keralam_hd", "Zee Keralam HD", "1811"),
    ZeeKeralamSd("zee_keralam_sd", "Zee Keralam", "1812"),
    KairaliTv("kairali_tv", "Kairali TV", "1820"),
    WeTv("we_tv", "We TV", "1822"),
    AmritaTv("amrita_tv", "Amrita TV", "1823"),
    SuryaMovies("surya_movies", "Surya Movies", "1826"),
    AsianetMoviesSd("asianet_movies_sd", "Asianet Movies (SD)", "1828"),
    SuryaComedy("surya_comedy", "Surya Comedy", "1830"),
    AsianetNews("asianet_news", "Asianet News", "1841"),
    News18Kerala("news18_kerala", "News18 Kerala", "1846"),
    ManoramaNews("manorama_news", "Manorama News", "1843"),
    MathrubhumiNews("mathrubhumi_news", "Mathrubhumi News", "1844"),
    KairaliNews("kairali_news", "Kairali News", "1842"),
    JaihindTv("jaihind_tv", "Jaihind TV", "1847"),
    JeevanTv("jeevan_tv", "Jeevan TV", "1848"),
    MediaOne("media_one", "MediaOne", "1849"),
    JanamTv("janam_tv", "Janam TV", "1492"),
    KaumudyTv("kaumudy_tv", "Kaumudy TV", "1851"),
    TwentyFourNews("twenty_four_news", "Twenty Four News", "1854"),
    ReporterTv("reporter_tv", "Reporter TV", "1855"),
    SafariTv("safari_tv", "Safari TV", "1838"),
    DdMalayalam("dd_malayalam", "DD Malayalam", "1874"),
    SuryaMusic("surya_music", "Surya Music", "1875"),
    RajMusixMalayalam("raj_musix_malayalam", "Raj Musix Malayalam", "1877"),
    KappaTv("kappa_tv", "Kappa TV", "1878"),
    GoodnessTv("goodness_tv", "Goodness TV", "1885"),
    ShalomTv("shalom_tv", "Shalom TV", "1887"),
    HarvestTv("harvest_tv", "Harvest TV 24x7", "1888"),
    KiteVicters("kite_victers", "Kite Victers", "580"),
    MoviesNowHd("movies_now_hd", "Movies Now HD", "420"),
    MnPlusHd("mn_plus_hd", "MN+ HD", "423"),
    MnxHd("mnx_hd", "MNX HD", "425"),
    AndFlixHd("and_flix_hd", "&flix HD", "412"),
    AndPriveHd("and_prive_hd", "&Prive HD", "431"),
    StarMoviesHd("star_movies_hd", "Star Movies HD", "402"),
    StarMoviesSelectHd("star_movies_select_hd", "Star Movies Select HD", "418"),
    AnimalPlanetHd("animal_planet_hd", "Animal Planet HD", "716"),
    AnimalPlanetHindi("animal_planet_hindi", "Animal Planet Hindi", "717"),
    DiscoveryHdWorld("discovery_hd_world", "Discovery HD World", "713"),
    DiscoveryScience("discovery_science", "Discovery Science", "719"),
    NationalGeographicHd("national_geographic_hd", "National Geographic HD", "708"),
    NatGeoWildHd("nat_geo_wild_hd", "Nat Geo Wild HD", "711"),
    HistoryTv18Hd("history_tv18_hd", "History TV18 HD", "720"),
    SunTvSd("sun_tv_sd", "Sun TV (SD)", "1504"),
    StarVijayHd("star_vijay_hd", "Star Vijay HD", "1507"),
    ZeeTamil("zee_tamil", "Zee Tamil", "1511"),
    ColorsTamilHd("colors_tamil_hd", "Colors Tamil HD", "1514"),
    KtvHd("ktv_hd", "KTV HD", "1540"),
    KtvSd("ktv_sd", "KTV", "1541"),
    VijaySuper("vijay_super", "Vijay Super", "1513"),
    SunMusicHd("sun_music_hd", "Sun Music HD", "1579"),
    SunNewsTamil("sun_news_tamil", "Sun News", "1559"),
    PolimerNews("polimer_news", "Polimer News", "1561"),
    ZeeThirai("zee_thirai", "Zee Thirai", "1546"),
    KalaignarTv("kalaignar_tv", "Kalaignar TV", "1518"),
    AdithyaTv("adithya_tv", "Adithya TV", "1543"),
    DTamil("d_tamil", "Discovery Tamil", "1525"),
    PuthiyaThalaimurai("puthiya_thalaimurai", "Puthiya Thalaimurai", "1556"),
    ThanthiTv("thanthi_tv", "Thanthi TV", "1557");

    fun create() = Channel(key, title, channelNumber)
}

internal object ChannelCatalogue {
    const val version = 3

    // These six keys already existed in v2. Never restore them if the user removed them.
    private val originalDefaults = setOf(
        DefaultChannel.AsianetHd,
        DefaultChannel.Flowers,
        DefaultChannel.MazhavilHd,
        DefaultChannel.AsianetMoviesHd,
        DefaultChannel.AsianetPlus,
        DefaultChannel.SunTv,
    )

    fun load(saved: List<Channel>?, savedVersion: Int): List<Channel> {
        if (saved == null) return DefaultChannel.entries.map { it.create() }
        if (savedVersion >= version) return saved

        val existing = if (savedVersion == 0) upgradeLegacyCatalogue(saved) else saved

        // Reserve saved numbers before backfilling so custom channels keep their assignments.
        val numbers = existing.mapNotNullTo(mutableSetOf()) { it.number }
        val enriched = existing.map { channel ->
            val entry = originalDefaults.firstOrNull { it.key == channel.key }
            if (entry != null && channel.number == null && channel.logoFile == null &&
                channel.legacyPlaybackId == null && channel.name.equals(entry.title, ignoreCase = true) &&
                numbers.add(entry.channelNumber)
            ) channel.copy(number = entry.channelNumber) else channel
        }

        // Add only v3 entries, preserving the saved order and honoring deletions.
        val keys = enriched.mapTo(mutableSetOf()) { it.key }
        val names = enriched.mapTo(mutableSetOf()) { it.name.lowercase() }
        val additions = DefaultChannel.entries.filter {
            it !in originalDefaults && it.key !in keys &&
                it.title.lowercase() !in names && it.channelNumber !in numbers
        }
        return enriched + additions.map { it.create() }
    }

    private fun upgradeLegacyCatalogue(saved: List<Channel>): List<Channel> {
        val matched = mutableSetOf<DefaultChannel>()
        val preserved = saved.map { channel ->
            val default = originalDefaults.firstOrNull {
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
        return preserved + originalDefaults
            .filter { it != DefaultChannel.SunTv && it !in matched }
            .map { Channel(it.key, it.title, null) }
    }
}
