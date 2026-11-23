package com.jioremote.app.data

import java.io.IOException
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ChannelLogoStoreTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun logoSurvivesReopeningAndCleanupKeepsReferencedFiles() {
        val original = ChannelLogoStore(folder.root)
        val first = original.write { it.write(byteArrayOf(6, 7, 8)) }
        val replacement = original.write { it.write(byteArrayOf(1, 2, 3)) }
        val outside = folder.newFile("unrelated-data.txt")

        val reopened = ChannelLogoStore(folder.root)
        assertArrayEquals(byteArrayOf(1, 2, 3), reopened.file(replacement)?.readBytes())
        reopened.removeUnused(setOf(replacement))
        assertFalse(reopened.file(first)!!.exists())
        assertTrue(reopened.file(replacement)!!.exists())
        assertTrue(outside.exists())
        assertNull(reopened.file("../outside.png"))
    }

    @Test fun failedImportLeavesNoPartialFile() {
        val store = ChannelLogoStore(folder.root)
        assertTrue(runCatching {
            store.write {
                it.write(1)
                throw IOException("interrupted")
            }
        }.isFailure)
        assertTrue(folder.root.listFiles()!!.isEmpty())
    }
}
