package com.jioremote.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionValidationTest {
    @Test fun acceptsPrivateAndLinkLocalTvAddresses() {
        assertEquals("10.0.0.8", validatedHost(" 10.0.0.8 "))
        assertEquals("172.31.255.254", validatedHost("172.31.255.254"))
        assertEquals("192.168.1.20", validatedHost("192.168.1.20"))
        assertEquals("169.254.10.2", validatedHost("169.254.10.2"))
    }

    @Test fun rejectsPublicOrMalformedAddresses() {
        assertTrue(runCatching { validatedHost("8.8.8.8") }.isFailure)
        assertTrue(runCatching { validatedHost("172.32.0.1") }.isFailure)
        assertTrue(runCatching { validatedHost("192.168.1.256") }.isFailure)
        assertTrue(runCatching { validatedHost("tv.local") }.isFailure)
    }

    @Test fun validatesPortRange() {
        assertEquals(5555, validatedPort("5555"))
        assertTrue(runCatching { validatedPort("0") }.isFailure)
        assertTrue(runCatching { validatedPort("65536") }.isFailure)
        assertTrue(runCatching { validatedPort("not-a-port") }.isFailure)
    }
}
