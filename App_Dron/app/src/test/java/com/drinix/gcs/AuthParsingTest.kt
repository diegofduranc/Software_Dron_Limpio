package com.drinix.gcs

import com.drinix.gcs.data.model.LoginResponse
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthParsingTest {

    private val gson = Gson()

    @Test
    fun `parses login response`() {
        val json = """
            {"token":"abc.def.ghi","user":{"username":"admin","role":"admin"},"expires_at":1893456000.0}
        """.trimIndent()

        val parsed = gson.fromJson(json, LoginResponse::class.java)

        assertEquals("abc.def.ghi", parsed.token)
        assertEquals("admin", parsed.user?.username)
        assertEquals("admin", parsed.user?.role)
        assertEquals(1893456000L, parsed.expiresAt.toLong())
    }

    @Test
    fun `parses response without user`() {
        val parsed = gson.fromJson("""{"token":"t","expires_at":10.5}""", LoginResponse::class.java)

        assertEquals("t", parsed.token)
        assertNull(parsed.user)
        assertEquals(10L, parsed.expiresAt.toLong())
    }

    @Test
    fun `missing expiry defaults to zero`() {
        val parsed = gson.fromJson("""{"token":"t"}""", LoginResponse::class.java)
        assertEquals(0.0, parsed.expiresAt, 0.0)
    }
}
