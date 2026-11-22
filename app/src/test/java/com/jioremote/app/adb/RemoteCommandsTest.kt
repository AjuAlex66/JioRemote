package com.jioremote.app.adb

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteCommandsTest {
    @Test fun knownSunTvDeepLinkUsesVerifiedIdAndPackage() {
        assertEquals(
            "am start -W -a android.intent.action.VIEW -d 'jiotvplus://304189?resume=true&source=watchnext' -p com.jio.media.jiotvplus",
            RemoteCommands.channel("304189")
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMaliciousChannelId() {
        RemoteCommands.channel("304189'; reboot")
    }

    @Test fun acceptsBackKeyOnlyFromAllowlist() {
        assertEquals("input keyevent 4", RemoteCommands.key(4))
        assertTrue(runCatching { RemoteCommands.key(999) }.isFailure)
    }

    @Test fun muteEmitsAndroidVolumeMuteKey() {
        assertEquals("input keyevent 164", RemoteCommands.key(164))
    }
}
