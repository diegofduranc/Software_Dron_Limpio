package com.drinix.gcs.data.network

import com.drinix.gcs.data.model.ArmRequest
import com.drinix.gcs.data.model.CalibrationStatus
import com.drinix.gcs.data.model.CommandResponse
import com.drinix.gcs.data.model.GotoRequest
import com.drinix.gcs.data.model.MissionUploadRequest
import com.drinix.gcs.data.model.MotorTestRequest
import com.drinix.gcs.data.model.RcRequest
import com.drinix.gcs.data.model.SetModeRequest
import com.drinix.gcs.data.model.TakeoffRequest
import com.drinix.gcs.data.model.Telemetry
import com.drinix.gcs.data.model.EnvironmentRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface DroneApi {

    @GET("api/telemetry")
    suspend fun getTelemetry(): Response<Telemetry>

    @POST("api/command/arm")
    suspend fun arm(@Body body: ArmRequest): Response<CommandResponse>

    @POST("api/command/disarm")
    suspend fun disarm(): Response<CommandResponse>

    @POST("api/command/takeoff")
    suspend fun takeoff(@Body body: TakeoffRequest): Response<CommandResponse>

    @POST("api/command/land")
    suspend fun land(): Response<CommandResponse>

    @POST("api/command/rtl")
    suspend fun rtl(): Response<CommandResponse>

    @POST("api/command/set_mode")
    suspend fun setMode(@Body body: SetModeRequest): Response<CommandResponse>

    @POST("api/command/goto")
    suspend fun goto(@Body body: GotoRequest): Response<CommandResponse>

    @POST("api/command/rc")
    suspend fun rc(@Body body: RcRequest): Response<CommandResponse>

    @POST("api/command/motor_test")
    suspend fun motorTest(@Body body: MotorTestRequest): Response<CommandResponse>

    @POST("api/mission/upload")
    suspend fun uploadMission(@Body body: MissionUploadRequest): Response<CommandResponse>

    @POST("api/calibration/environment")
    suspend fun setEnvironment(@Body body: EnvironmentRequest): Response<CommandResponse>

    @POST("api/calibration/compass")
    suspend fun calibrateCompass(): Response<CommandResponse>

    @POST("api/calibration/all")
    suspend fun calibrateSystem(): Response<CommandResponse>

    @POST("api/calibration/home")
    suspend fun calibrateHome(): Response<CommandResponse>

    @POST("api/calibration/gps")
    suspend fun verifyGps(): Response<CommandResponse>

    @GET("api/calibration/status")
    suspend fun getCalibrationStatus(): Response<CalibrationStatus>

    @POST("api/rc/release")
    suspend fun releaseRc(): Response<CommandResponse>
}
