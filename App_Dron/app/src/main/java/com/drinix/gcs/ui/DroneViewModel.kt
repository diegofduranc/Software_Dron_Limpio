package com.drinix.gcs.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drinix.gcs.data.AuthRepository
import com.drinix.gcs.data.CommandRepository
import com.drinix.gcs.data.ConnectionValidator
import com.drinix.gcs.data.SessionManager
import com.drinix.gcs.data.SettingsRepository
import com.drinix.gcs.data.TelemetryRepository
import com.drinix.gcs.data.model.AlertSeverity
import com.drinix.gcs.data.model.AuthResult
import com.drinix.gcs.data.model.AuthState
import com.drinix.gcs.data.model.CommandResult
import com.drinix.gcs.data.model.ConnectionState
import com.drinix.gcs.data.model.FlightAlert
import com.drinix.gcs.data.model.Telemetry
import com.drinix.gcs.data.model.Waypoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel compartido: mantiene la telemetría, el estado de conexión,
 * las alertas de vuelo y expone el envío de comandos al backend.
 */
@HiltViewModel
class DroneViewModel @Inject constructor(
    private val telemetryRepo: TelemetryRepository,
    private val commands: CommandRepository,
    private val settings: SettingsRepository,
    private val session: SessionManager,
    private val auth: AuthRepository,
) : ViewModel() {

    val telemetry: StateFlow<Telemetry?> = telemetryRepo.telemetry
    val connectionState: StateFlow<ConnectionState> = telemetryRepo.connectionState

    /** Estado de autenticación: decide si se muestra el login o la app. */
    private val _authState = MutableStateFlow(AuthState.LOADING)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /** Mensaje de error/estado del login (nunca contiene la contraseña). */
    private val _authMessage = MutableStateFlow<String?>(null)
    val authMessage: StateFlow<String?> = _authMessage.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    /** Mensajes de resultado de comandos (para Snackbars). */
    private val _commandEvents = MutableSharedFlow<CommandResult>(extraBufferCapacity = 8)
    val commandEvents: SharedFlow<CommandResult> = _commandEvents.asSharedFlow()

    // ---------- Estado de vuelo derivado ----------
    private val _pathPoints = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val pathPoints: StateFlow<List<Pair<Double, Double>>> = _pathPoints.asStateFlow()

    private val _homePoint = MutableStateFlow<Pair<Double, Double>?>(null)
    val homePoint: StateFlow<Pair<Double, Double>?> = _homePoint.asStateFlow()

    private val _flightSeconds = MutableStateFlow(0L)
    val flightSeconds: StateFlow<Long> = _flightSeconds.asStateFlow()

    private val _distanceToHome = MutableStateFlow<Double?>(null)
    val distanceToHome: StateFlow<Double?> = _distanceToHome.asStateFlow()

    private val _alerts = MutableStateFlow<List<FlightAlert>>(emptyList())
    val alerts: StateFlow<List<FlightAlert>> = _alerts.asStateFlow()

    private var armedSince: Long? = null
    private var wasArmed = false
    private var lowBatt = false
    private var lowGps = false
    private var lastMode: String? = null
    private var wasConnected = false

    init {
        // Solo conectar si ya hay una sesión válida guardada; nunca sin token.
        if (session.isTokenValid()) {
            _authState.value = AuthState.AUTHENTICATED
            telemetryRepo.connect()
        } else {
            _authState.value = AuthState.UNAUTHENTICATED
        }

        // El servidor rechazó la sesión (401 REST o 403/1008 WS).
        viewModelScope.launch {
            session.sessionExpired.collect { handleSessionExpired() }
        }

        // Procesar telemetría: recorrido, Home, distancia, alertas.
        viewModelScope.launch {
            telemetry.collect { t ->
                val lat = t?.lat
                val lon = t?.lon
                if (lat != null && lon != null && (lat != 0.0 || lon != 0.0)) {
                    val current = _pathPoints.value
                    if (current.lastOrNull() != (lat to lon)) {
                        _pathPoints.value = (current + (lat to lon)).takeLast(500)
                    }
                }

                val armed = t?.armed == true
                if (armed && !wasArmed) {
                    armedSince = System.currentTimeMillis()
                    if (lat != null && lon != null) {
                        _homePoint.value = lat to lon // punto de armado = Home
                    }
                }
                if (!armed && wasArmed) {
                    armedSince = null
                    _flightSeconds.value = 0L
                }
                wasArmed = armed

                _homePoint.value?.let { home ->
                    if (lat != null && lon != null) {
                        _distanceToHome.value = haversineMeters(lat, lon, home.first, home.second)
                    }
                }

                processAlerts(t)
            }
        }

        // Alerta al perder la conexión WS.
        viewModelScope.launch {
            connectionState.collect { state ->
                if (state == ConnectionState.CONNECTED) wasConnected = true
                if (state == ConnectionState.DISCONNECTED && wasConnected) {
                    addAlert(
                        AlertSeverity.CRITICAL,
                        "Telemetría desconectada",
                        "Se perdió la conexión con el dron.",
                    )
                }
            }
        }

        // Temporizador de vuelo (corre mientras esté armado).
        viewModelScope.launch {
            while (true) {
                val since = armedSince
                if (since != null) {
                    _flightSeconds.value = (System.currentTimeMillis() - since) / 1000
                }
                delay(1000)
            }
        }
    }

    private fun processAlerts(t: Telemetry?) {
        val batt = t?.batteryRemaining
        if (batt != null && batt < 20 && !lowBatt) {
            lowBatt = true
            addAlert(
                AlertSeverity.CRITICAL,
                "Batería baja",
                "La batería del dron está por debajo del 20%.",
            )
        } else if (batt != null && batt >= 20) {
            lowBatt = false
        }

        val sats = t?.satellites
        if (sats != null && sats < 6 && !lowGps) {
            lowGps = true
            addAlert(
                AlertSeverity.WARNING,
                "Pérdida de señal GPS",
                "Señal GPS débil. Solo $sats satélites.",
            )
        } else if (sats != null && sats >= 6) {
            lowGps = false
        }

        val mode = t?.mode
        if (mode == "RTL" && lastMode != "RTL") {
            addAlert(AlertSeverity.SUCCESS, "RTH iniciado", "Regreso a casa activado.")
        }
        if (mode != null) lastMode = mode
    }

    private fun addAlert(severity: AlertSeverity, title: String, message: String) {
        _alerts.value = (listOf(FlightAlert(title, message, severity)) + _alerts.value).take(30)
    }

    // ---------- Conexión ----------
    fun toggleConnection() {
        when (connectionState.value) {
            ConnectionState.DISCONNECTED -> telemetryRepo.connect()
            else -> telemetryRepo.disconnect()
        }
    }

    fun reconnect() = telemetryRepo.connect()

    // ---------- Autenticación ----------

    /** Inicia sesión: valida datos, guarda host/puerto y token, y conecta el WS. */
    fun login(host: String, port: Int, username: String, password: String) {
        if (_isAuthenticating.value) return
        val cleanHost = host.trim()
        if (!ConnectionValidator.isValidHost(cleanHost)) {
            _authMessage.value = "Host inválido"
            return
        }
        if (!ConnectionValidator.isValidPort(port)) {
            _authMessage.value = "Puerto inválido (1-65535)"
            return
        }
        if (username.isBlank() || password.isEmpty()) {
            _authMessage.value = "Usuario y contraseña obligatorios"
            return
        }
        viewModelScope.launch {
            _isAuthenticating.value = true
            _authMessage.value = null
            when (val result = auth.login(cleanHost, port, username, password)) {
                is AuthResult.Success -> {
                    _authState.value = AuthState.AUTHENTICATED
                    _authMessage.value = null
                    telemetryRepo.connect()
                    _commandEvents.emit(CommandResult.Success("Sesión iniciada como ${result.username}"))
                }
                AuthResult.InvalidCredentials ->
                    _authMessage.value = "Credenciales inválidas"
                AuthResult.Unreachable ->
                    _authMessage.value = "Servidor inalcanzable (timeout)"
                is AuthResult.Unexpected ->
                    _authMessage.value = "Respuesta inesperada del servidor"
            }
            _isAuthenticating.value = false
        }
    }

    /** Cierra sesión en el servidor (best-effort) y borra la sesión local. */
    fun logout() {
        viewModelScope.launch {
            telemetryRepo.disconnect()
            auth.logout()
            _authMessage.value = null
            _authState.value = AuthState.UNAUTHENTICATED
            _commandEvents.emit(CommandResult.Success("Sesión cerrada"))
        }
    }

    private fun handleSessionExpired() {
        viewModelScope.launch {
            telemetryRepo.disconnect()
            auth.clearLocalSession()
            _authMessage.value = "Sesión caducada. Inicia sesión de nuevo."
            _authState.value = AuthState.EXPIRED
        }
    }

    val hostFlow = settings.hostFlow
    val portFlow = settings.portFlow
    val usernameFlow = settings.usernameFlow
    val authExpiresAtFlow = settings.expiresAtFlow
    val keepScreenOnFlow = settings.keepScreenOnFlow
    val droneNameFlow = settings.droneNameFlow
    val savedMissions = settings.missionsFlow

    fun saveMission(mission: com.drinix.gcs.data.model.SavedMission) {
        viewModelScope.launch {
            settings.saveMission(mission)
            _commandEvents.emit(CommandResult.Success("Misión \"${mission.name}\" guardada"))
        }
    }

    fun deleteMission(name: String) {
        viewModelScope.launch {
            settings.deleteMission(name)
            _commandEvents.emit(CommandResult.Success("Misión eliminada"))
        }
    }
    val host: String get() = session.host
    val port: Int get() = session.port

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { settings.setKeepScreenOn(enabled) }
    }

    fun setDroneName(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) settings.setDroneName(name)
        }
    }

    // ---------- Comandos ----------
    fun sendCommand(block: suspend CommandRepository.() -> CommandResult) {
        viewModelScope.launch {
            _commandEvents.emit(commands.block())
        }
    }

    fun arm(arm: Boolean) = sendCommand { sendArm(arm) }
    fun takeoff(altitude: Double) = sendCommand { sendTakeoff(altitude) }
    fun land() = sendCommand { sendLand() }
    fun rtl() = sendCommand { sendRtl() }
    fun setMode(mode: String) = sendCommand { sendSetMode(mode) }
    fun goto(lat: Double, lon: Double, alt: Double) = sendCommand { sendGoto(lat, lon, alt) }
    fun uploadMission(waypoints: List<Waypoint>) = sendCommand { uploadMission(waypoints) }

    /** Envía override RC (los joysticks limitan la frecuencia; sin snackbar). */
    fun sendRc(roll: Int, pitch: Int, throttle: Int, yaw: Int) {
        viewModelScope.launch { commands.sendRc(roll, pitch, throttle, yaw) }
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * asin(sqrt(a))
    }
}
