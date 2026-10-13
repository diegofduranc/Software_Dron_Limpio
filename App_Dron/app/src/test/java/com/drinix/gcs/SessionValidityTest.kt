package com.drinix.gcs

import com.drinix.gcs.data.model.SessionValidity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionValidityTest {

    private val now = 1_000_000L

    @Test
    fun `null token is invalid`() {
        assertFalse(SessionValidity.isTokenValid(null, null, now))
    }

    @Test
    fun `blank token is invalid even with future expiry`() {
        assertFalse(SessionValidity.isTokenValid("   ", now + 3600, now))
    }

    @Test
    fun `token with unknown expiry is valid`() {
        assertTrue(SessionValidity.isTokenValid("abc", null, now))
    }

    @Test
    fun `token well before expiry is valid`() {
        assertTrue(SessionValidity.isTokenValid("abc", now + 3600, now))
    }

    @Test
    fun `expired token is invalid`() {
        assertFalse(SessionValidity.isTokenValid("abc", now - 1, now))
    }

    @Test
    fun `token inside the skew margin is invalid`() {
        assertFalse(SessionValidity.isTokenValid("abc", now + 10, now))
    }
}
