package com.jioremote.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelCatalogueTest {
    @Test fun freshInstallSeedsPublishedChannelNumbersWithoutLegacyPlaybackIds() {
        val channels = ChannelCatalogue.load(saved = null, savedVersion = 0)
        assertEquals(69, channels.size)
        assertEquals(DefaultChannel.entries.map { it.title }, channels.map { it.name })
        assertEquals("1827", channels.single { it.key == DefaultChannel.AsianetMoviesHd.key }.number)
        assertEquals("716", channels.single { it.key == DefaultChannel.AnimalPlanetHd.key }.number)
        assertEquals("420", channels.single { it.key == DefaultChannel.MoviesNowHd.key }.number)
        assertEquals("1507", channels.single { it.key == DefaultChannel.StarVijayHd.key }.number)
        assertEquals("555", channels.single { it.key == DefaultChannel.Flowers.key }.number)
        assertTrue(channels.all { it.number != null && it.legacyPlaybackId == null })
        assertEquals(channels.size, channels.map { it.key }.toSet().size)
        assertEquals(channels.size, channels.map { it.number }.toSet().size)
        assertEquals(channels.size, channels.map { it.name.lowercase() }.toSet().size)
        assertTrue(channels.all { it.number?.matches(Regex("[0-9]{1,12}")) == true })
        assertFalse(channels.any { it.number == "304189" })
    }

    @Test fun upgradingVersionTwoAppendsOnlyNewChannelsAndPreservesOldOrderAndEdits() {
        val custom = Channel("custom:123", "My movie", "9898", "saved-custom-logo.png")
        val preexistingNumber = Channel("custom:420", "My Hollywood", "420")
        val edited = Channel("asianet_hd", "My Asianet", "1234", "edited-logo.png")
        val previouslyRemoved = DefaultChannel.Flowers
        val old = listOf(custom, preexistingNumber, edited, Channel("asianet_movies_hd", "Asianet Movies HD", "1827"))
        val migrated = ChannelCatalogue.load(old, savedVersion = 2)
        assertEquals(old, migrated.take(old.size))
        assertFalse(migrated.any { it.key == previouslyRemoved.key })
        assertFalse(migrated.any { it.key == DefaultChannel.MoviesNowHd.key }) // already stored under custom key
        assertEquals("716", migrated.single { it.key == DefaultChannel.AnimalPlanetHd.key }.number)
        assertEquals("1540", migrated.single { it.key == DefaultChannel.KtvHd.key }.number)
        assertEquals(migrated, ChannelCatalogue.load(migrated, savedVersion = ChannelCatalogue.version))
    }

    @Test fun upgradeDoesNotTreatOldSunTvPlaybackIdAsNumericChannelNumber() {
        val sun = Channel("sun_tv", "Sun TV", null, legacyPlaybackId = "304189")
        val upgraded = ChannelCatalogue.load(listOf(sun), savedVersion = 2)
        assertNull(upgraded.first().number)
        assertEquals("304189", upgraded.first().legacyPlaybackId)
    }

    @Test fun backfillsOldUntouchedCoreChannelWithoutOverwritingEditedOne() {
        val blank = Channel("asianet_plus", "Asianet Plus", null)
        val custom = Channel("flowers", "Flowers", "777")
        val migrated = ChannelCatalogue.load(listOf(blank, custom), 2)
        assertEquals("1809", migrated.first().number)
        assertEquals("777", migrated[1].number)
    }

    @Test fun versionOneMigrationPreservesHistoricalIdsAndCustomChannels() {
        val sun = Channel("sun_tv", "My Sun", null, legacyPlaybackId = "304189")
        val custom = Channel("custom:abc", "My channel", null, "custom.png", "1022")
        val movies = Channel("asianet_movies_hd", "My movies", null)
        val migrated = ChannelCatalogue.load(listOf(sun, custom, movies), 1)
        assertEquals(listOf(sun, custom, movies), migrated.take(3))
        assertFalse(migrated.any { it.key == DefaultChannel.AsianetHd.key })
        assertEquals("1828", migrated.single { it.key == DefaultChannel.AsianetMoviesSd.key }.number)
    }

    @Test fun oldCatalogueUpgradeHonorsDeletedSunTv() {
        val custom = Channel("legacy:456", "Flowers TV", null, legacyPlaybackId = "456")
        val migrated = ChannelCatalogue.load(listOf(custom), 0)
        assertEquals(custom.copy(key = DefaultChannel.Flowers.key), migrated.first())
        assertFalse(migrated.any { it.key == DefaultChannel.SunTv.key })
    }

    @Test fun preservesLogoEditsAndReservesNumbersBeforeBackfilling() {
        val logo = Channel("flowers", "Flowers", null, "saved-logo.png")
        val blank = Channel("asianet_plus", "Asianet Plus", null)
        val existingNumber = Channel("custom:number", "My local channel", "1809")
        val existingName = Channel("custom:name", "movies now hd", "8899")
        val saved = listOf(logo, blank, existingNumber, existingName)
        val migrated = ChannelCatalogue.load(saved, 2)
        assertEquals(saved, migrated.take(saved.size))
        assertFalse(migrated.any { it.key == DefaultChannel.MoviesNowHd.key })
        assertEquals(1, migrated.count { it.number == "1809" })
    }

    @Test fun originallyMissingDefaultsDoNotDuplicateCustomNumbersDuringUpgrade() {
        val custom = Channel("legacy:custom", "My local channel", "1809")
        val migrated = ChannelCatalogue.load(listOf(custom), 0)
        assertEquals(custom, migrated.first())
        assertNull(migrated.single { it.key == DefaultChannel.AsianetPlus.key }.number)
        assertEquals(1, migrated.count { it.number == "1809" })
    }

    @Test fun deletionsAreNotRestoredOnSubsequentVersionThreeLoads() {
        val fresh = ChannelCatalogue.load(null, 0)
        val edited = fresh.filterNot { it.key in setOf(DefaultChannel.SunTv.key, DefaultChannel.MnxHd.key) }
        assertEquals(edited, ChannelCatalogue.load(edited, ChannelCatalogue.version))
        assertEquals(emptyList<Channel>(), ChannelCatalogue.load(emptyList(), ChannelCatalogue.version))
    }
}
