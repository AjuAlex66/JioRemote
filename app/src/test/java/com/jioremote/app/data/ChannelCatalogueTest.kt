package com.jioremote.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChannelCatalogueTest {
    @Test fun freshCatalogueContainsRequestedChannelsWithOnlyVerifiedDefaultId() {
        val channels = ChannelCatalogue.load(saved = null, savedVersion = 0)
        assertEquals(
            listOf("Asianet HD", "Flowers", "Mazhavil Manorama HD", "Asianet Movies HD", "Asianet Plus", "Sun TV"),
            channels.map { it.name },
        )
        assertEquals(listOf("304189"), channels.mapNotNull { it.id })
        assertEquals(channels.size, channels.distinctBy { it.key }.size)
    }

    @Test fun legacyMigrationKeepsCustomIdsAndRecognisesAlreadyConfiguredDefaults() {
        val custom = Channel("legacy:123", "My channel", "123")
        val flowers = Channel("legacy:456", "Flowers TV", "456")
        val sun = Channel("legacy:304189", "My Sun TV", "304189")
        val migrated = ChannelCatalogue.load(listOf(custom, flowers, sun), savedVersion = 0)

        assertEquals(custom, migrated[0])
        assertEquals(flowers.copy(key = DefaultChannel.Flowers.key), migrated[1])
        assertEquals(sun.copy(key = DefaultChannel.SunTv.key), migrated[2])
        assertEquals(7, migrated.size)
        assertNull(migrated.single { it.key == DefaultChannel.AsianetHd.key }.id)
    }

    @Test fun migrationDoesNotRestoreRemovedSunTvOrAddDeletedDefaultsAgain() {
        val migrated = ChannelCatalogue.load(emptyList(), savedVersion = 0)
        assertEquals(5, migrated.size)
        val edited = migrated.drop(1).map {
            if (it.key == DefaultChannel.Flowers.key) it.copy(name = "Favourite", id = "123") else it
        }
        assertEquals(edited, ChannelCatalogue.load(edited, ChannelCatalogue.version))
        assertEquals(emptyList<Channel>(), ChannelCatalogue.load(emptyList(), ChannelCatalogue.version))
    }
}
