package com.drinix.gcs

import com.drinix.gcs.data.model.AuthResponseMapper
import com.drinix.gcs.data.model.AuthResult
import com.drinix.gcs.data.model.LoginResponse
import com.drinix.gcs.data.model.UserDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthResponseMapperTest {

    @Test
    fun `401 maps to invalid credentials`() {
        val result = AuthResponseMapper.map(
            isSuccessful = false,
            httpCode = 401,
            body = null,
            fallbackUsername = "admin",
        )
        assertTrue(result is AuthResult.InvalidCredentials)
    }

    @Test
    fun `successful response maps to success with username`() {
        val body = LoginResponse(
            token = "t",
            user = UserDto("admin", "admin"),
            expiresAt = 1893456000.0,
        )
        val result = AuthResponseMapper.map(true, 200, body, "fallback")
        assertTrue(result is AuthResult.Success)
        assertEquals("admin", (result as AuthResult.Success).username)
        assertEquals(1893456000L, result.expiresAtEpochSeconds)
    }

    @Test
    fun `successful response without user falls back to typed username`() {
        val result = AuthResponseMapper.map(true, 200, LoginResponse(token = "t"), "operador")
        assertEquals("operador", (result as AuthResult.Success).username)
    }

    @Test
    fun `successful response without token is unexpected`() {
        val result = AuthResponseMapper.map(true, 200, LoginResponse(token = ""), "admin")
        assertTrue(result is AuthResult.Unexpected)
    }

    @Test
    fun `server error maps to unexpected`() {
        val result = AuthResponseMapper.map(false, 500, null, "admin")
        assertTrue(result is AuthResult.Unexpected)
    }
}
