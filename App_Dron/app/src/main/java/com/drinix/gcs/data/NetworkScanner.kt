package com.drinix.gcs.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object NetworkScanner {

    private const val DEFAULT_PORT = 8000
    private const val HEALTH_PATH = "/health"
    private const val CONNECT_TIMEOUT_MS = 500L
    private const val MAX_CONCURRENT = 30

    fun getLocalIpPrefix(context: Context): String? {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        if (networkInfo == null || !networkInfo.isConnected) {
            return null
        }

        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        if (wifiInfo == null) {
            return null
        }

        val ipAddress = wifiInfo.ipAddress
        if (ipAddress == 0) {
            return null
        }

        val formattedIp = String.format(
            "%d.%d.%d.%d",
            (ipAddress and 0xFF),
            (ipAddress shr 8 and 0xFF),
            (ipAddress shr 16 and 0xFF),
            (ipAddress shr 24 and 0xFF)
        )

        val lastDot = formattedIp.lastIndexOf('.')
        if (lastDot < 0) {
            return null
        }
        return formattedIp.substring(0, lastDot + 1)
    }

    fun getAllSearchPrefixes(context: Context): List<String> {
        val prefixes = mutableSetOf<String>()
        getLocalIpPrefix(context)?.let { prefixes.add(it) }
        // Common mobile hotspot subnets (Android AP, Xiaomi/MIUI, Samsung, Windows, Linux)
        prefixes.add("192.168.43.")
        prefixes.add("192.168.4.")
        prefixes.add("192.168.12.")
        prefixes.add("192.168.173.")
        prefixes.add("10.42.0.")
        return prefixes.toList()
    }

    suspend fun scanForBackends(
        prefixes: List<String>,
        port: Int = DEFAULT_PORT,
        timeoutMs: Long = CONNECT_TIMEOUT_MS
    ): List<String> {
        return withContext(Dispatchers.IO) {
            val found = java.util.Collections.synchronizedList(mutableListOf<String>())
            val semaphore = java.util.concurrent.Semaphore(MAX_CONCURRENT)
            val jobs = mutableListOf<kotlinx.coroutines.Job>()

            for (prefix in prefixes) {
                for (i in 1 until 255) {
                    val ip = "$prefix$i"
                    semaphore.acquire()
                    val job = launch(Dispatchers.IO) {
                        try {
                            if (isBackendAlive(ip, port, timeoutMs.toInt())) {
                                if (!found.contains(ip)) {
                                    found.add(ip)
                                }
                            }
                        } finally {
                            semaphore.release()
                        }
                    }
                    jobs += job
                }
            }

            jobs.forEach { it.join() }
            found.sorted()
        }
    }

    suspend fun scanForBackends(
        prefix: String,
        port: Int = DEFAULT_PORT,
        timeoutMs: Long = CONNECT_TIMEOUT_MS
    ): List<String> {
        return scanForBackends(listOf(prefix), port, timeoutMs)
    }

    private fun isBackendAlive(ip: String, port: Int, timeoutMs: Int): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL("http://$ip:$port$HEALTH_PATH")
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            connection.requestMethod = "GET"
            connection.doInput = false
            connection.doOutput = false
            connection.instanceFollowRedirects = false
            connection.connect()
            val code = connection.responseCode
            code == 200
        } catch (_: IOException) {
            false
        } catch (_: Exception) {
            false
        } finally {
            connection?.disconnect()
        }
    }
}
