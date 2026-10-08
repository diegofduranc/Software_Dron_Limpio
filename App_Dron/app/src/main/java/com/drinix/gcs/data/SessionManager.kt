package com.drinix.gcs.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Mantiene en memoria (thread-safe) el host, puerto y token actuales.
 * Se sincroniza con DataStore y es leído por los interceptores de OkHttp
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

    init {
        // Carga inicial síncrona para que los interceptores tengan valores
        // desde la primera petición.
        runBlocking {
            host = settings.hostFlow.first()
            port = settings.portFlow.first()
            token = settings.tokenFlow.first()
        }
        // Mantener sincronizado ante cambios.
        scope.launch { settings.hostFlow.collect { host = it } }
        scope.launch { settings.portFlow.collect { port = it } }
        scope.launch { settings.tokenFlow.collect { token = it } }
    }

    fun baseHttpUrl(): String = "http://$host:$port/"
    fun wsTelemetryUrl(): String = "ws://$host:$port/ws/telemetry?token=${token.orEmpty()}"
}
