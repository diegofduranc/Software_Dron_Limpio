package com.drinix.gcs.data

/** Validación de los datos de conexión, sin dependencias de Android. */
object ConnectionValidator {

    private val IPV4 = Regex("^((25[0-5]|2[0-4]\\d|1?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1?\\d?\\d)$")
    private val HOSTNAME = Regex("^[a-zA-Z0-9]([a-zA-Z0-9\\-.]*[a-zA-Z0-9])?$")

    fun isValidHost(host: String): Boolean {
        val h = host.trim()
        if (h.isEmpty()) return false
        return IPV4.matches(h) || HOSTNAME.matches(h)
    }

    fun isValidPort(port: Int): Boolean = port in 1..65535

    fun isValidPort(raw: String?): Boolean = raw?.trim()?.toIntOrNull()?.let { isValidPort(it) } == true

    /** Valida que la URL empiece por http:// o https:// */
    fun isValidUrl(url: String): Boolean {
        val u = url.trim()
        return u.startsWith("http://") || u.startsWith("https://")
    }
}
