package com.drinix.gcs.data

import com.drinix.gcs.data.model.ArmRequest
import com.drinix.gcs.data.model.CalibrationStatus
import com.drinix.gcs.data.model.CommandResult
import com.drinix.gcs.data.model.GotoRequest
import com.drinix.gcs.data.model.MissionUploadRequest
import com.drinix.gcs.data.model.MotorTestRequest
import com.drinix.gcs.data.model.RcRequest
import com.drinix.gcs.data.model.SetModeRequest
import com.drinix.gcs.data.model.TakeoffRequest
import com.drinix.gcs.data.model.Waypoint
import com.drinix.gcs.data.model.EnvironmentRequest
import com.drinix.gcs.data.network.DroneApi
import com.drinix.gcs.data.network.SessionExpiredException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response

@Singleton
class CommandRepository @Inject constructor(
    private val api: DroneApi,
) {

    suspend fun sendArm(arm: Boolean) = if (arm) {
        execute("ARM activado") { api.arm(ArmRequest(true)) }
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
        execute("RC enviado") { api.rc(RcRequest(roll, pitch, throttle, yaw)) }
    suspend fun testMotor(motor: Int, throttle: Float, duration: Float) =
        execute("Prueba del motor $motor iniciada") {
            api.motorTest(MotorTestRequest(motor, throttle, duration))
        }
    suspend fun uploadMission(waypoints: List<Waypoint>) =
        execute("${waypoints.size} waypoints subidos") { api.uploadMission(MissionUploadRequest(waypoints)) }
    suspend fun setEnvironment(environment: String) =
        execute("Entorno configurado") { api.setEnvironment(EnvironmentRequest(environment)) }
    suspend fun calibrateCompass() =
        execute("Calibración de brújula iniciada") { api.calibrateCompass() }
    suspend fun calibrateSystem() =
        execute("Calibración integral iniciada") { api.calibrateSystem() }
    suspend fun calibrateHome() =
        execute("Posición cero configurada") { api.calibrateHome() }
    suspend fun verifyGps() =
        execute("Estado GPS consultado") { api.verifyGps() }
    suspend fun releaseRc() =
        execute("Control RC liberado") { api.releaseRc() }
    suspend fun getCalibrationStatus(): CalibrationStatus {
        return try {
            val resp = api.getCalibrationStatus()
            val body = resp.body()
            body ?: CalibrationStatus("error", "Sin respuesta del servidor")
        } catch (e: IOException) {
            CalibrationStatus("error", "Servidor inalcanzable")
        } catch (e: Exception) {
            CalibrationStatus("error", "Respuesta inesperada del servidor")
        }
    }

    private suspend fun execute(
        actionName: String,
        call: suspend () -> Response<com.drinix.gcs.data.model.CommandResponse>,
    ): CommandResult {
        return try {
            val resp = call()
            val body = resp.body()
            when {
                resp.isSuccessful && body?.success == true -> CommandResult.Success(body.message)
                body != null && body.message.isNotBlank() -> CommandResult.Error(body.message)
                resp.isSuccessful -> CommandResult.Error("Respuesta inesperada del servidor")
                else -> CommandResult.Error("Error del servidor (HTTP ${resp.code()})")
            }
        } catch (e: SessionExpiredException) {
            CommandResult.Error("Sesión caducada. Inicia sesión de nuevo.")
        } catch (e: IOException) {
            CommandResult.Error("Servidor inalcanzable (timeout)")
        } catch (e: Exception) {
            CommandResult.Error("Respuesta inesperada del servidor")
        }
    }
}
