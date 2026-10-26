package com.drinix.gcs.data

import com.drinix.gcs.data.model.AuthResponseMapper
import com.drinix.gcs.data.model.AuthResult
import com.drinix.gcs.data.model.LoginRequest
import com.drinix.gcs.data.network.AuthApi
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestiona el inicio y cierre de sesión contra el backend.
 * La contraseña nunca se persiste ni se registra en logs.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val session: SessionManager,
    private val settings: SettingsRepository,
) {

    suspend fun login(
        baseUrl: String,
        username: String,
        password: String,
    ): AuthResult {
        val cleanBaseUrl = baseUrl.trim().removeSuffix("/")
        return try {
            // URL absoluta construida desde la base URL configurada
            val url = "$cleanBaseUrl/api/auth/login"
            val response = api.login(url, LoginRequest(username.trim(), password))
            val body = response.body()
            val result = AuthResponseMapper.map(
                isSuccessful = response.isSuccessful,
                httpCode = response.code(),
                body = body,
                fallbackUsername = username.trim(),
            )
            if (result is AuthResult.Success && body != null) {
                session.applySession(cleanBaseUrl, body.token, result.expiresAtEpochSeconds, result.username)
                persistSession(cleanBaseUrl, body.token, result.expiresAtEpochSeconds, result.username)
            }
            result
        } catch (e: IOException) {
            AuthResult.Unreachable
        } catch (e: Exception) {
            AuthResult.Unexpected(e.message)
        }
    }

    /** Inicia sesión como invitado (sin contraseña) para pruebas. */
    suspend fun loginAsGuest(baseUrl: String): AuthResult {
        val cleanBaseUrl = baseUrl.trim().removeSuffix("/")
        return try {
            val url = "$cleanBaseUrl/api/auth/guest-login"
            val response = api.guestLogin(url)
            val body = response.body()
            val result = AuthResponseMapper.map(
                isSuccessful = response.isSuccessful,
                httpCode = response.code(),
                body = body,
                fallbackUsername = "guest",
            )
            if (result is AuthResult.Success && body != null) {
                session.applySession(cleanBaseUrl, body.token, result.expiresAtEpochSeconds, result.username)
                persistSession(cleanBaseUrl, body.token, result.expiresAtEpochSeconds, result.username)
            }
            result
        } catch (e: IOException) {
            AuthResult.Unreachable
        } catch (e: Exception) {
            AuthResult.Unexpected(e.message)
        }
    }

    private suspend fun persistSession(
        baseUrl: String,
        token: String,
        expiresAtEpochSeconds: Long?,
        username: String,
    ) {
        settings.setBaseUrl(baseUrl)
        settings.setUsername(username)
        settings.setToken(token)
        settings.setExpiresAt(expiresAtEpochSeconds)
    }

    /** Cierra sesión en el servidor (best-effort) y borra siempre la sesión local. */
    suspend fun logout() {
        try {
            api.logout()
        } catch (_: Exception) {
            // Aunque falle la llamada, la sesión local se borra igualmente.
        }
        clearLocalSession()
    }

    suspend fun clearLocalSession() = settings.clearSession()
}
