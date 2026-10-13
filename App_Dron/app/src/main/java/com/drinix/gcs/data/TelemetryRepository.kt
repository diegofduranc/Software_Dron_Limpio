package com.drinix.gcs.data

import android.util.Log
import com.drinix.gcs.data.model.ConnectionState
import com.drinix.gcs.data.model.Telemetry
import com.drinix.gcs.data.model.WsFrame
import com.google.gson.Gson
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * Gestiona el WebSocket de telemetría con reconexión automática.
 * No abre el socket si no hay token válido; ante 403/1008 cierra la sesión
 * y deja de reintentar hasta que el usuario vuelva a autenticarse.
 */
@Singleton
class TelemetryRepository @Inject constructor(
    private val session: SessionManager,
    private val gson: Gson,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var webSocket: WebSocket? = null
    private var reconnectAttempts = 0
    @Volatile private var shouldReconnect = false

    private val _telemetry = MutableStateFlow<Telemetry?>(null)
    val telemetry: StateFlow<Telemetry?> = _telemetry.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val listener = object : WebSocketListener() {
        override fun onOpen(ws: WebSocket, response: Response) {
            Log.d(TAG, "WS conectado")
            reconnectAttempts = 0
            _connectionState.value = ConnectionState.CONNECTED
        }

        override fun onMessage(ws: WebSocket, text: String) {
            try {
                val frame = gson.fromJson(text, WsFrame::class.java)
                if (frame.type == "telemetry" && frame.data != null) {
                    val t = gson.fromJson(frame.data, Telemetry::class.java)
                    _telemetry.value = t
                }
            } catch (e: Exception) {
                Log.w(TAG, "Frame WS inválido: ${e.message}")
            }
        }

        override fun onClosing(ws: WebSocket, code: Int, reason: String) {
            ws.close(1000, null)
        }

        override fun onClosed(ws: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WS cerrado: $code $reason")
            _connectionState.value = ConnectionState.DISCONNECTED
            if (code == WS_CODE_POLICY_VIOLATION) {
                handleSessionExpired()
            } else {
                scheduleReconnect()
            }
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            Log.w(TAG, "WS fallo: ${t.message} (http=${response?.code})")
            _connectionState.value = ConnectionState.DISCONNECTED
            if (response?.code == HTTP_FORBIDDEN) {
                handleSessionExpired()
            } else {
                scheduleReconnect()
            }
        }
    }

    /** Conecta (o reconecta) el WebSocket. No hace nada sin un token válido. */
    fun connect() {
        if (!session.isTokenValid()) {
            Log.d(TAG, "connect() ignorado: sin token válido")
            shouldReconnect = false
            _connectionState.value = ConnectionState.DISCONNECTED
            return
        }
        shouldReconnect = true
        reconnectAttempts = 0
        openSocket()
    }

    /** Cierra y desactiva la reconexión automática. */
    fun disconnect() {
        shouldReconnect = false
        webSocket?.close(1000, "cierre manual")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private fun openSocket() {
        val url = session.wsTelemetryUrl()
        if (url == null) {
            Log.d(TAG, "openSocket() abortado: URL nula (sin token)")
            shouldReconnect = false
            _connectionState.value = ConnectionState.DISCONNECTED
            return
        }
        webSocket?.close(1000, "reconnect")
        _connectionState.value = ConnectionState.CONNECTING
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, listener)
    }

    private fun scheduleReconnect() {
        if (!shouldReconnect) return
        if (!session.isTokenValid()) {
            handleSessionExpired()
            return
        }
        scope.launch {
            val backoffMs = minOf(30_000L, 1_000L shl reconnectAttempts.coerceAtMost(5))
            reconnectAttempts++
            Log.d(TAG, "Reintento WS en ${backoffMs}ms (intento $reconnectAttempts)")
            delay(backoffMs)
            if (shouldReconnect) openSocket()
        }
    }

    /** El servidor rechazó la sesión: detener reintentos y avisar. */
    private fun handleSessionExpired() {
        shouldReconnect = false
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
        session.notifySessionExpired()
    }

    private companion object {
        const val TAG = "TelemetryRepo"
        const val HTTP_FORBIDDEN = 403
        const val WS_CODE_POLICY_VIOLATION = 1008
    }
}
