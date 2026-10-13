package com.drinix.gcs.data

import com.drinix.gcs.data.model.SessionValidity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Mantiene en memoria (thread-safe) el host, puerto, token, usuario y expiración
 * actuales. Se sincroniza con DataStore y es leído por los interceptores de OkHttp
 * y por el manejador del WebSocket.
 */
@Singleton
class SessionManager @Inject constructor(
    private val settings: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile var host: String = SettingsRepository.DEFAULT_HOST
        private set
    @Volatile var port: Int = SettingsRepository.DEFAULT_PORT
        private set
    @Volatile var token: String? = null
        private set
    @Volatile var expiresAtEpochSeconds: Long? = null
        private set
    @Volatile var username: String? = null
        private set

    /** Se emite cuando el servidor rechaza la sesión (401 REST o 403/1008 WS). */
    private val _sessionExpired = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    init {
        // Carga inicial síncrona para que los interceptores tengan valores
        // desde la primera petición.
        runBlocking {
            host = settings.hostFlow.first()
            port = settings.portFlow.first()
            token = settings.tokenFlow.first()
            expiresAtEpochSeconds = settings.expiresAtFlow.first()
            username = settings.usernameFlow.first()
        }
        // Mantener sincronizado ante cambios.
        scope.launch { settings.hostFlow.collect { host = it } }
        scope.launch { settings.portFlow.collect { port = it } }
        scope.launch { settings.tokenFlow.collect { token = it } }
        scope.launch { settings.expiresAtFlow.collect { expiresAtEpochSeconds = it } }
        scope.launch { settings.usernameFlow.collect { username = it } }
    }

    /** ¿Hay una sesión utilizable ahora mismo? */
    fun isTokenValid(): Boolean = SessionValidity.isTokenValid(token, expiresAtEpochSeconds)

    /**
     * Aplica de inmediato (en memoria) una sesión recién autenticada, para que
     * la siguiente petición/WS ya use estos valores sin esperar a DataStore.
     */
    fun applySession(
        host: String,
        port: Int,
        token: String,
        expiresAtEpochSeconds: Long?,
        username: String?,
    ) {
        this.host = host
        this.port = port
        this.token = token
        this.expiresAtEpochSeconds = expiresAtEpochSeconds
        this.username = username
    }

    fun baseHttpUrl(): String = "http://$host:$port/"

    /** URL del WebSocket de telemetría, o null si no hay token utilizable. */
    fun wsTelemetryUrl(): String? {
        val t = token
        if (t.isNullOrBlank()) return null
        return "ws://$host:$port/ws/telemetry?token=$t"
    }

    /** Avisa de que la sesión dejó de ser válida. */
    fun notifySessionExpired() {
        _sessionExpired.tryEmit(Unit)
    }
}
