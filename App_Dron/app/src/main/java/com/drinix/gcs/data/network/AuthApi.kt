package com.drinix.gcs.data.network

import com.drinix.gcs.data.model.LoginRequest
import com.drinix.gcs.data.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

/** Autenticación contra el backend FastAPI. */
interface AuthApi {

    /**
     * El login usa una URL absoluta para apuntar al host/puerto que el usuario
     * escribió, sin alterar la sesión activa si las credenciales fallan.
     */
    @POST
    suspend fun login(@Url url: String, @Body body: LoginRequest): Response<LoginResponse>

    @POST
    suspend fun guestLogin(@Url url: String): Response<LoginResponse>

    @POST("api/auth/logout")
    suspend fun logout(): Response<Unit>
}
