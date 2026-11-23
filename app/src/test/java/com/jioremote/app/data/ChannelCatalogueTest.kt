package com.jioremote.app.data

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ChannelCatalogueTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun freshInstallHasExactlyTheSixOriginalNumbers() {
        val channels = ChannelCatalogue.load(saved = null, savedVersion = 0)

        assertEquals(
            listOf(
                Channel("asianet_hd", "Asianet HD", "1806"),
                Channel("flowers", "Flowers", "555"),
                Channel("mazhavil_hd", "Mazhavil Manorama HD", "1818"),
                Channel("asianet_movies_hd", "Asianet Movies HD", "1827"),
                Channel("asianet_plus", "Asianet Plus", "1809"),
                Channel("sun_tv", "Sun TV", "1503"),
            ),
            channels,
        )
        assertEquals(4, ChannelCatalogue.version)
    }

    @Test fun versionFourDropsAll63RetiredKeysEvenIfEdited() {
        val defaults = DefaultChannel.entries.map { it.create() }
        val expanded = ChannelCatalogue.retiredV3Keys.map { Channel(it, "Edited old default", "99", "old.png") }
        assertEquals(63, expanded.size)
        assertEquals(69, (defaults + expanded).distinctBy { it.key }.size)
        val v3 = expanded.take(15) + defaults + expanded.drop(15)

        assertEquals(defaults, ChannelCatalogue.load(v3, savedVersion = 3))
    }

    @Test fun keepsSavedValuesOrderAndCustomChannelsWithRetiredNamesOrNumbers() {
        val edited = Channel("asianet_hd", "My Asianet", "00123", "saved-logo.png")
        val sun = Channel("sun_tv", "My Sun", null, legacyPlaybackId = "304189")
        val sameName = Channel("custom:1", "Movies Now HD", "9001")
        val sameNumber = Channel("custom:2", "My documentary", "716", "custom-logo.png")
        val movie = DefaultChannel.AsianetMoviesHd.create()
        val expected = listOf(sameNumber, movie, edited, sameName, sun)
        val saved = expected.toMutableList().apply {
            add(1, Channel("movies_now_hd", "Movies Now HD", "420"))
            add(4, Channel("animal_planet_hd", "Animal Planet HD", "716"))
        }

        val migrated = ChannelCatalogue.load(saved, savedVersion = 3)
        assertEquals(expected, migrated)
        assertEquals(migrated, ChannelCatalogue.load(migrated, savedVersion = 3))
        assertEquals(migrated, ChannelCatalogue.load(migrated, savedVersion = 4))
    }

    @Test fun olderAndCurrentSavedListsKeepDeletionsAndDoNotBackfillEditedNumbers() {
        val original = Channel("flowers", "Flowers", null)
        val custom = Channel("custom:1", "My channel", "1806")
        val saved = listOf(custom, original)
        for (version in 1..4) {
            assertEquals(saved, ChannelCatalogue.load(saved, savedVersion = version))
            assertEquals(emptyList<Channel>(), ChannelCatalogue.load(emptyList(), savedVersion = version))
        }
    }

    @Test fun legacyUpgradeKeepsSunPlaybackIdSeparateAndHonorsItsDeletion() {
        val sun = Channel("legacy:304189", "My Sun TV", null, legacyPlaybackId = "304189")
        val flowers = Channel("legacy:456", "Flowers TV", null, legacyPlaybackId = "456")
        val assigned = Channel("custom:1", "My local channel", "1809")

        val upgraded = ChannelCatalogue.load(listOf(sun, flowers, assigned), savedVersion = 0)
        assertEquals(sun.copy(key = "sun_tv"), upgraded.first())
        assertEquals(flowers, upgraded[1])
        assertEquals(assigned, upgraded[2])
        assertNull(upgraded.single { it.key == "asianet_plus" }.number)
        assertFalse(upgraded.any { it.key == "flowers" || it.number == "304189" })
        assertFalse(ChannelCatalogue.load(listOf(flowers), savedVersion = 0).any { it.key == "sun_tv" })
    }

    @Test fun cleanupAfterMigrationKeepsLogosStillReferencedByAnySurvivor() {
        val logos = ChannelLogoStore(folder.root)
        val shared = logos.write { it.write(1) }
        val orphan = logos.write { it.write(2) }
        val original = DefaultChannel.Flowers.create().copy(logoFile = shared)
        val custom = Channel("custom:3", "My flowers", "400", shared)
        val saved = listOf(
            Channel("movies_now_hd", "Movies Now HD", "420", orphan),
            original,
            Channel("animal_planet_hd", "Animal Planet HD", "716", shared),
            custom,
        )

        val migrated = ChannelCatalogue.load(saved, savedVersion = 3)
        logos.removeUnused(migrated.mapNotNull { it.logoFile }.toSet())

        assertEquals(listOf(original, custom), migrated)
        assertTrue(logos.file(shared)!!.exists())
        assertFalse(logos.file(orphan)!!.exists())
    }
}
