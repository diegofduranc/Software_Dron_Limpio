package com.drinix.gcs.di

import com.drinix.gcs.data.SessionManager
import com.drinix.gcs.data.network.AuthInterceptor
import com.drinix.gcs.data.network.DroneApi
import com.drinix.gcs.data.network.HostRewriteInterceptor
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideOkHttp(
        session: SessionManager,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(session))
        .addInterceptor(HostRewriteInterceptor(session))
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            // La host real se reescribe en HostRewriteInterceptor
            .baseUrl("http://drinix.placeholder/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideDroneApi(retrofit: Retrofit): DroneApi =
        retrofit.create(DroneApi::class.java)
}
