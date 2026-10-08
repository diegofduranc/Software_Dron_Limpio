package com.drinix.gcs.data.model

import com.google.gson.annotations.SerializedName

/** Telemetría del dron recibida por REST o WebSocket. */
data class Telemetry(
    @SerializedName("voltage") val voltage: Double? = null,
    @SerializedName("battery_remaining") val batteryRemaining: Int? = null,
    @SerializedName("mode") val mode: String? = null,
    @SerializedName("armed") val armed: Boolean? = null,
    @SerializedName("lat") val lat: Double? = null,
    @SerializedName("lon") val lon: Double? = null,
    @SerializedName("alt") val alt: Double? = null,
    @SerializedName("satellites") val satellites: Int? = null,
    @SerializedName("heading") val heading: Double? = null,
    @SerializedName("groundspeed") val groundspeed: Double? = null,
    @SerializedName("seq") val seq: Int? = null,
    /** GPS fix type (3 = 3D fix). Puede no venir. */
    @SerializedName("fix_type") val fixType: Int? = null,
)
