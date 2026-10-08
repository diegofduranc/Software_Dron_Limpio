package com.drinix.gcs.data.network

import com.drinix.gcs.data.SessionManager
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

/** Agrega el header Authorization: Bearer <token> si hay token disponible. */
class AuthInterceptor @Inject constructor(
    private val session: SessionManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = session.token
        val request = if (!token.isNullOrBlank()) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else original
        return chain.proceed(request)
    }
}

/**
 * Reescribe host/puerto de cada petición al valor actual de la configuración.
 * La base URL de Retrofit es un placeholder; este interceptor aplica la IP real.
 */
class HostRewriteInterceptor @Inject constructor(
    private val session: SessionManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val newUrl = original.url.newBuilder()
            .host(session.host)
            .port(session.port)
            .build()
        return chain.proceed(original.newBuilder().url(newUrl).build())
    }
}
