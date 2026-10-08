package com.drinix.gcs.data.network

import com.drinix.gcs.data.model.ArmRequest
import com.drinix.gcs.data.model.CommandResponse
import com.drinix.gcs.data.model.GotoRequest
import com.drinix.gcs.data.model.MissionUploadRequest
import com.drinix.gcs.data.model.RcRequest
import com.drinix.gcs.data.model.SetModeRequest
import com.drinix.gcs.data.model.TakeoffRequest
import com.drinix.gcs.data.model.Telemetry
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface DroneApi {

    @GET("api/telemetry")
    suspend fun getTelemetry(): Response<Telemetry>

    @POST("api/arm")
    suspend fun arm(@Body body: ArmRequest): Response<CommandResponse>

    @POST("api/disarm")
    suspend fun disarm(): Response<CommandResponse>

    @POST("api/takeoff")
    suspend fun takeoff(@Body body: TakeoffRequest): Response<CommandResponse>

    @POST("api/land")
    suspend fun land(): Response<CommandResponse>

    @POST("api/rtl")
    suspend fun rtl(): Response<CommandResponse>

    @POST("api/mode")
    suspend fun setMode(@Body body: SetModeRequest): Response<CommandResponse>

    @POST("api/goto")
    suspend fun goto(@Body body: GotoRequest): Response<CommandResponse>

    @POST("api/rc/control")
    suspend fun rc(@Body body: RcRequest): Response<CommandResponse>

    @POST("api/nav/mission")
    suspend fun uploadMission(@Body body: MissionUploadRequest): Response<CommandResponse>
}
