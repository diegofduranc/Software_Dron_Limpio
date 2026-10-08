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
import kotlinx.coroutines.Job
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
 * Gestiona el WebSocket de telemetría con reconexión automática
 * y soporte de Telemetría Simulada para pruebas locales sin servidor.
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
    private var mockJob: Job? = null

    // Base de la telemetría simulada (por defecto Bogotá; se actualiza con la ubicación del dispositivo)
    @Volatile private var mockBaseLat = 4.7110
    @Volatile private var mockBaseLon = -74.0721

    /** Reposiciona la telemetría simulada cerca de la ubicación del usuario. */
    fun setMockBaseLocation(lat: Double, lon: Double) {
        mockBaseLat = lat
        mockBaseLon = lon
        // Si la simulación está corriendo, reiniciarla en la nueva base
        if (mockJob != null) {
            stopMockTelemetry()
            startMockTelemetry()
        }
    }

    private val _telemetry = MutableStateFlow<Telemetry?>(null)
    val telemetry: StateFlow<Telemetry?> = _telemetry.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val listener = object : WebSocketListener() {
        override fun onOpen(ws: WebSocket, response: Response) {
            Log.d(TAG, "WS conectado")
            stopMockTelemetry()
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
            Log.d(TAG, "WS cerrado: $reason")
            _connectionState.value = ConnectionState.DISCONNECTED
            scheduleReconnect()
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            Log.w(TAG, "WS fallo: ${t.message}")
            _connectionState.value = ConnectionState.DISCONNECTED
            startMockTelemetry()
        }
    }

    /** Conecta (o reconecta) el WebSocket. */
    fun connect() {
        shouldReconnect = true
        reconnectAttempts = 0
        openSocket()
    }

    /** Cierra y desactiva la reconexión automática. */
    fun disconnect() {
        shouldReconnect = false
        stopMockTelemetry()
        webSocket?.close(1000, "cierre manual")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private fun openSocket() {
        webSocket?.close(1000, "reconnect")
        _connectionState.value = ConnectionState.CONNECTING
        val request = Request.Builder().url(session.wsTelemetryUrl()).build()
        webSocket = client.newWebSocket(request, listener)
    }

    private fun scheduleReconnect() {
        if (!shouldReconnect) return
        scope.launch {
            val backoffMs = minOf(30_000L, 1_000L shl reconnectAttempts.coerceAtMost(5))
            reconnectAttempts++
            Log.d(TAG, "Reintento WS en ${backoffMs}ms (intento $reconnectAttempts)")
            delay(backoffMs)
            if (shouldReconnect) openSocket()
        }
    }

    private fun startMockTelemetry() {
        stopMockTelemetry()
        mockJob = scope.launch {
            _connectionState.value = ConnectionState.CONNECTED
            var lat = mockBaseLat
            var lon = mockBaseLon
            var heading = 0.0
            var battery = 92

            while (shouldReconnect) {
                // Rumbo cambia suavemente: el dron avanza con la ruta, sin girar en el sitio
                heading = (heading + 2.0) % 360.0
                lat += 0.00012 * Math.cos(Math.toRadians(heading))
                lon += 0.00012 * Math.sin(Math.toRadians(heading))
                if (battery > 15 && Math.random() < 0.05) battery--

                _telemetry.value = Telemetry(
                    voltage = 15.8,
                    batteryRemaining = battery,
                    mode = "LOITER",
                    armed = true,
                    lat = lat,
                    lon = lon,
                    alt = 12.5,
                    satellites = 14,
                    heading = heading,
                    groundspeed = 3.2,
                    seq = 1,
                    fixType = 3,
                )
                delay(1000)
            }
        }
    }

    private fun stopMockTelemetry() {
        mockJob?.cancel()
        mockJob = null
    }

    private companion object {
        const val TAG = "TelemetryRepo"
    }
}
