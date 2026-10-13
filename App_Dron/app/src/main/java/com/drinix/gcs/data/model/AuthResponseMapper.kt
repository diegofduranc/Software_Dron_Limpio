package com.drinix.gcs.data.model

/**
 * Traduce la respuesta HTTP del login a un [AuthResult], sin dependencias
 * de Android, para poder probarlo de forma aislada.
 */
object AuthResponseMapper {

    fun map(
        isSuccessful: Boolean,
        httpCode: Int,
        body: LoginResponse?,
        fallbackUsername: String,
    ): AuthResult {
        return when {
            isSuccessful && !body?.token.isNullOrBlank() -> {
                val expires = body!!.expiresAt.takeIf { it > 0.0 }?.toLong()
                val name = body.user?.username?.takeIf { it.isNotBlank() } ?: fallbackUsername
                AuthResult.Success(name, expires)
            }
            httpCode == 401 -> AuthResult.InvalidCredentials
            else -> AuthResult.Unexpected("HTTP $httpCode")
        }
    }
}
