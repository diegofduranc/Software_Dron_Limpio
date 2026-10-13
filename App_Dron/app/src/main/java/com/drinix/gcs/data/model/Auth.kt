package com.drinix.gcs.data.model

import com.google.gson.annotations.SerializedName

/** Cuerpo de POST /api/auth/login. La contraseña solo vive en memoria. */
data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
)

data class UserDto(
    @SerializedName("username") val username: String,
    @SerializedName("role") val role: String? = null,
)

/** Respuesta de POST /api/auth/login. */
data class LoginResponse(
    @SerializedName("token") val token: String = "",
    @SerializedName("user") val user: UserDto? = null,
    @SerializedName("expires_at") val expiresAt: Double = 0.0,
)

/** Resultado de un intento de login, ya traducido para la UI. */
sealed class AuthResult {
    data class Success(val username: String, val expiresAtEpochSeconds: Long?) : AuthResult()
    object InvalidCredentials : AuthResult()
    object Unreachable : AuthResult()
    data class Unexpected(val detail: String?) : AuthResult()
}

/** Estado de autenticación de la sesión. */
enum class AuthState { LOADING, UNAUTHENTICATED, AUTHENTICATED, EXPIRED }

/**
 * Lógica de validez de token, aislada para poder probarla sin Android.
 * Un token es válido si no está vacío y, cuando se conoce su expiración,
 * está por delante de "now" con un margen de seguridad.
 */
object SessionValidity {

    /** Segundos de margen para no usar un token a punto de expirar. */
    const val EXPIRY_SKEW_SECONDS = 30L

    fun isTokenValid(
        token: String?,
        expiresAtEpochSeconds: Long?,
        nowEpochSeconds: Long = System.currentTimeMillis() / 1000L,
    ): Boolean {
        if (token.isNullOrBlank()) return false
        if (expiresAtEpochSeconds == null) return true
        return expiresAtEpochSeconds > nowEpochSeconds + EXPIRY_SKEW_SECONDS
    }
}
