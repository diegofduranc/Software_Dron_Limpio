package com.drinix.gcs.data.model

import com.google.gson.annotations.SerializedName

/** Respuesta estándar de los comandos REST del backend. */
data class CommandResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
)

data class ArmRequest(@SerializedName("force") val force: Boolean)

data class TakeoffRequest(@SerializedName("altitude") val altitude: Double)

data class SetModeRequest(@SerializedName("mode") val mode: String)

data class GotoRequest(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("altitude") val altitude: Double,
)

/** Override RC: valores PWM (centro 1500, rango 1000..2000). */
data class RcRequest(
    @SerializedName("roll") val roll: Double,
    @SerializedName("pitch") val pitch: Double,
    @SerializedName("throttle") val throttle: Double,
    @SerializedName("yaw") val yaw: Double,
)

/** Waypoint de misión. */
data class Waypoint(
    @SerializedName("seq") val seq: Int,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
    @SerializedName("alt") val alt: Double,
)

data class MissionUploadRequest(
    @SerializedName("waypoints") val waypoints: List<Waypoint>,
)

/** Misión guardada localmente por el usuario. */
data class SavedMission(
    val name: String,
    val waypoints: List<Waypoint>,
    val speed: Double,
    val closed: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Frame WebSocket: {"type":"telemetry","data":{...}} */
data class WsFrame(
    @SerializedName("type") val type: String?,
    @SerializedName("data") val data: com.google.gson.JsonObject?,
)

/** Resultado de un comando para la UI. */
sealed class CommandResult {
    data class Success(val message: String) : CommandResult()
    data class Error(val message: String) : CommandResult()
}

/** Estado de la conexión WebSocket. */
enum class ConnectionState { CONNECTED, CONNECTING, DISCONNECTED }

/** Severidad de una alerta de vuelo. */
enum class AlertSeverity { INFO, SUCCESS, WARNING, CRITICAL }

/** Alerta generada a partir de la telemetría. */
data class FlightAlert(
    val title: String,
    val message: String,
    val severity: AlertSeverity,
    val timestamp: Long = System.currentTimeMillis(),
)
