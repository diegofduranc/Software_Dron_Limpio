package com.drinix.gcs.data.model

import com.google.gson.annotations.SerializedName

/** Respuesta estándar de los comandos REST del backend. */
data class CommandResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
)

data class ArmRequest(@SerializedName("arm") val arm: Boolean)

data class TakeoffRequest(@SerializedName("altitude") val altitude: Double)

data class SetModeRequest(@SerializedName("mode") val mode: String)

data class GotoRequest(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
    @SerializedName("alt") val alt: Double,
)

/** Override RC: valores PWM (centro 1500, rango 1000..2000). */
data class RcRequest(
    @SerializedName("roll") val roll: Int,
    @SerializedName("pitch") val pitch: Int,
    @SerializedName("throttle") val throttle: Int,
    @SerializedName("yaw") val yaw: Int,
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
    @SerializedName("speed") val speed: Double? = null,
    @SerializedName("closed") val closed: Boolean = false,
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
