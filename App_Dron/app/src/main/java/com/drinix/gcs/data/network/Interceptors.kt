package com.drinix.gcs.data.network

import com.drinix.gcs.data.SessionManager
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Agrega el header Authorization: Bearer <token> si hay token disponible.
 * Si el servidor responde 401 con un token adjunto, la sesión dejó de ser
 * válida y se notifica para volver al login.
 */
class AuthInterceptor @Inject constructor(
    private val session: SessionManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath
        val isLogin = path.endsWith("/api/auth/login")
        val isAuthPath = path.contains("/api/auth/")
        val token = session.token
        // El login nunca lleva un token previo (evita arrastrar una sesión caducada).
        val hadToken = !isLogin && !token.isNullOrBlank()
        val request = if (hadToken) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else original
        val response = chain.proceed(request)
        // Un 401 en rutas normales con token adjunto significa sesión caducada.
        // En /api/auth/* no (p. ej. credenciales incorrectas al iniciar sesión).
        if (hadToken && !isAuthPath && response.code == 401) {
            response.close()
            session.notifySessionExpired()
            throw SessionExpiredException()
        }
        return response
    }
}

/** Excepción interna para cortar la llamada cuando el servidor rechaza el token. */
class SessionExpiredException : java.io.IOException("Sesión caducada")

/**
 * Reescribe host/puerto de cada petición al valor actual de la configuración.
 * La base URL de Retrofit es un placeholder; este interceptor aplica la IP real.
 * Las peticiones con host explícito (p. ej. el login) no se reescriben.
 */
class HostRewriteInterceptor @Inject constructor(
    private val session: SessionManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.url.host != PLACEHOLDER_HOST) {
            return chain.proceed(original)
        }
        val newUrl = original.url.newBuilder()
            .host(session.host)
            .port(session.port)
            .build()
        return chain.proceed(original.newBuilder().url(newUrl).build())
    }

    private companion object {
        const val PLACEHOLDER_HOST = "drinix.placeholder"
    }
}
