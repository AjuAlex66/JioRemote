package com.jioremote.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChannelCatalogueTest {
    @Test fun freshCatalogueOnlyAssignsTheVerifiedRemoteNumber() {
        val channels = ChannelCatalogue.load(saved = null, savedVersion = 0)
        assertEquals(DefaultChannel.entries.map { it.title }, channels.map { it.name })
        assertEquals(listOf("1827"), channels.mapNotNull { it.number })
        assertEquals("1827", channels.single { it.key == DefaultChannel.AsianetMoviesHd.key }.number)
        assertNull(channels.single { it.key == DefaultChannel.SunTv.key }.number)
    }

    @Test fun versionOneMigrationPreservesOrderNamesAndHistoricalIdsWithoutRestoringDeletedDefaults() {
        val sun = Channel("sun_tv", "My Sun", null, legacyPlaybackId = "304189")
        val custom = Channel("custom:abc", "My channel", null, "custom.png", "1022")
        val movies = Channel("asianet_movies_hd", "My movies", null)
        val saved = listOf(sun, custom, movies)
        assertEquals(listOf(sun, custom, movies.copy(number = "1827")), ChannelCatalogue.load(saved, 1))
        assertEquals(emptyList<Channel>(), ChannelCatalogue.load(emptyList(), 1))
    }

    @Test fun conservativelyPreservesEditedMovieChannelWithAHistoricalPlaybackId() {
        val movies = Channel("asianet_movies_hd", "My movies", null, legacyPlaybackId = "123456")
        assertEquals(listOf(movies), ChannelCatalogue.load(listOf(movies), 1))
    }

    @Test fun originalCatalogueMigrationHonorsDeletedSunTv() {
        val custom = Channel("legacy:456", "Flowers TV", null, legacyPlaybackId = "456")
        val migrated = ChannelCatalogue.load(listOf(custom), 0)
        assertEquals(custom.copy(key = DefaultChannel.Flowers.key), migrated.first())
        assertEquals(5, migrated.size)
        assertEquals(migrated.drop(1), ChannelCatalogue.load(migrated.drop(1), ChannelCatalogue.version))
    }
}
