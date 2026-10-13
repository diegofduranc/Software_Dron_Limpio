package com.drinix.gcs

import com.drinix.gcs.data.ConnectionValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionValidatorTest {

    @Test
    fun `blank host is invalid`() {
        assertFalse(ConnectionValidator.isValidHost(""))
        assertFalse(ConnectionValidator.isValidHost("   "))
    }

    @Test
    fun `ipv4 host is valid`() {
        assertTrue(ConnectionValidator.isValidHost("172.20.10.2"))
    }

    @Test
    fun `hostname is valid`() {
        assertTrue(ConnectionValidator.isValidHost("raspberry.local"))
    }

    @Test
    fun `host with spaces is invalid`() {
        assertFalse(ConnectionValidator.isValidHost("bad host"))
    }

    @Test
    fun `port range is enforced`() {
        assertFalse(ConnectionValidator.isValidPort(0))
        assertFalse(ConnectionValidator.isValidPort(65536))
        assertFalse(ConnectionValidator.isValidPort(-1))
        assertTrue(ConnectionValidator.isValidPort(1))
        assertTrue(ConnectionValidator.isValidPort(8000))
        assertTrue(ConnectionValidator.isValidPort(65535))
    }

    @Test
    fun `port string parsing`() {
        assertFalse(ConnectionValidator.isValidPort("abc"))
        assertFalse(ConnectionValidator.isValidPort(null))
        assertTrue(ConnectionValidator.isValidPort("8000"))
    }
}
