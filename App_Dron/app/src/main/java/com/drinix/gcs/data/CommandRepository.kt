package com.drinix.gcs.data

import com.drinix.gcs.data.model.ArmRequest
import com.drinix.gcs.data.model.CommandResult
import com.drinix.gcs.data.model.GotoRequest
import com.drinix.gcs.data.model.MissionUploadRequest
import com.drinix.gcs.data.model.RcRequest
import com.drinix.gcs.data.model.SetModeRequest
import com.drinix.gcs.data.model.TakeoffRequest
import com.drinix.gcs.data.model.Waypoint
import com.drinix.gcs.data.network.DroneApi
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response

@Singleton
class CommandRepository @Inject constructor(
    private val api: DroneApi,
) {

    suspend fun sendArm(arm: Boolean) = if (arm) {
        execute("ARM activado") { api.arm(ArmRequest(force = false)) }
    } else {
        execute("ARM desactivado") { api.disarm() }
    }
    suspend fun sendTakeoff(altitude: Double) = execute("Despegue a $altitude m") { api.takeoff(TakeoffRequest(altitude)) }
    suspend fun sendLand() = execute("Aterrizaje iniciado") { api.land() }
    suspend fun sendRtl() = execute("RTL (Return to Launch)") { api.rtl() }
    suspend fun sendSetMode(mode: String) = execute("Modo cambiado a $mode") { api.setMode(SetModeRequest(mode)) }
    suspend fun sendGoto(lat: Double, lon: Double, alt: Double) =
        execute("Volando a destino (%.5f, %.5f)".format(lat, lon)) { api.goto(GotoRequest(lat, lon, alt)) }
    suspend fun sendRc(roll: Int, pitch: Int, throttle: Int, yaw: Int) =
        execute("RC enviado") {
            api.rc(
                RcRequest(
                    roll = ((roll - 1500) / 500.0).coerceIn(-1.0, 1.0),
                    pitch = ((pitch - 1500) / 500.0).coerceIn(-1.0, 1.0),
                    throttle = ((throttle - 1000) / 1000.0).coerceIn(0.0, 1.0),
                    yaw = ((yaw - 1500) / 500.0).coerceIn(-1.0, 1.0),
                ),
            )
        }
    suspend fun uploadMission(waypoints: List<Waypoint>) =
        execute("${waypoints.size} waypoints subidos") { api.uploadMission(MissionUploadRequest(waypoints)) }

    private suspend fun execute(
        actionName: String,
        call: suspend () -> Response<com.drinix.gcs.data.model.CommandResponse>,
    ): CommandResult {
        return try {
            val resp = call()
            val body = resp.body()
            when {
                resp.isSuccessful && body?.success == true -> CommandResult.Success(body.message)
                body != null -> CommandResult.Error(body.message)
                else -> CommandResult.Success("[Demo] $actionName")
            }
        } catch (e: Exception) {
            CommandResult.Success("[Demo] $actionName")
        }
    }
}
