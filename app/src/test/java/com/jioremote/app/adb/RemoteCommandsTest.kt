package com.jioremote.app.adb

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteCommandsTest {
    @Test fun knownChannelEmitsDigitsInOrderWithThree150MillisecondPauses() {
        assertEquals(
            "input keyevent KEYCODE_1 && sleep 0.150 && input keyevent KEYCODE_8 && sleep 0.150 && " +
                "input keyevent KEYCODE_2 && sleep 0.150 && input keyevent KEYCODE_7",
            RemoteCommands.channel("1827"),
        )
    }

    @Test fun preservesLeadingZeroAndDoesNotAppendAnExtraKey() {
        assertEquals("input keyevent KEYCODE_0", RemoteCommands.channel("0"))
        assertEquals("input keyevent KEYCODE_0 && sleep 0.150 && input keyevent KEYCODE_1", RemoteCommands.channel("01"))
    }

    @Test fun rejectsMalformedOrUnsafeChannelNumbers() {
        for (input in listOf("", " 1827", "1827 ", "+7", "٣", "1234567890123", "1827; reboot", "1\n8")) {
            assertTrue(input, runCatching { RemoteCommands.channel(input) }.isFailure)
        }
    }

    @Test fun preservesVolumeKeyAllowlist() {
        assertEquals("input keyevent 164", RemoteCommands.key(164))
        assertEquals("input keyevent 25", RemoteCommands.key(25))
        assertEquals("input keyevent 24", RemoteCommands.key(24))
        assertTrue(runCatching { RemoteCommands.key(999) }.isFailure)
    }
}
