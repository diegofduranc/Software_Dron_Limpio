package com.drinix.gcs.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "drinix_settings"
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val store = context.dataStore

    val hostFlow: Flow<String> = store.data.map { it[KEY_HOST] ?: DEFAULT_HOST }
    val portFlow: Flow<Int> = store.data.map { it[KEY_PORT] ?: DEFAULT_PORT }
    val tokenFlow: Flow<String?> = store.data.map { it[KEY_TOKEN] }
    /** Expiración del token en epoch (segundos). null si no se conoce. */
    val expiresAtFlow: Flow<Long?> = store.data.map { it[KEY_EXPIRES_AT]?.takeIf { v -> v > 0L } }
    /** Nombre del usuario autenticado (nunca la contraseña). */
    val usernameFlow: Flow<String?> = store.data.map { it[KEY_USERNAME] }
    val keepScreenOnFlow: Flow<Boolean> = store.data.map { it[KEY_KEEP_SCREEN_ON] ?: false }
    val droneNameFlow: Flow<String> = store.data.map { it[KEY_DRONE_NAME] ?: DEFAULT_DRONE_NAME }
    val droneModelFlow: Flow<String> = store.data.map { it[KEY_DRONE_MODEL] ?: DEFAULT_DRONE_MODEL }
    val droneImageUrlFlow: Flow<String?> = store.data.map { it[KEY_DRONE_IMAGE_URL] }
    val environmentFlow: Flow<String> = store.data.map { it[KEY_ENVIRONMENT] ?: DEFAULT_ENVIRONMENT }
    val darkModeFlow: Flow<Boolean> = store.data.map { it[KEY_DARK_MODE] ?: DEFAULT_DARK_MODE }
    val joystickDeadzoneFlow: Flow<Float> = store.data.map { (it[KEY_JOYSTICK_DEADZONE] ?: DEFAULT_JOYSTICK_DEADZONE_INT) / 1000f }
    val joystickSensitivityFlow: Flow<Float> = store.data.map { (it[KEY_JOYSTICK_SENSITIVITY] ?: DEFAULT_JOYSTICK_SENSITIVITY_INT) / 1000f }
    /** URL base completa del backend (ej: http://192.168.1.100:8000 o https://Raberry-innovacion.local:8000). */
    val baseUrlFlow: Flow<String> = store.data.map { it[KEY_BASE_URL] ?: DEFAULT_BASE_URL }

    suspend fun setHost(host: String) = store.edit { it[KEY_HOST] = host }
    suspend fun setPort(port: Int) = store.edit { it[KEY_PORT] = port }
    suspend fun setToken(token: String?) = store.edit {
        if (token == null) it.remove(KEY_TOKEN) else it[KEY_TOKEN] = token
    }
    suspend fun setExpiresAt(epochSeconds: Long?) = store.edit {
        if (epochSeconds == null || epochSeconds <= 0L) it.remove(KEY_EXPIRES_AT)
        else it[KEY_EXPIRES_AT] = epochSeconds
    }
    suspend fun setUsername(name: String?) = store.edit {
        if (name.isNullOrBlank()) it.remove(KEY_USERNAME) else it[KEY_USERNAME] = name
    }

    /** Borra token, expiración y usuario. No hay otros secretos guardados. */
    suspend fun clearSession() = store.edit {
        it.remove(KEY_TOKEN)
        it.remove(KEY_EXPIRES_AT)
        it.remove(KEY_USERNAME)
    }
    suspend fun setKeepScreenOn(enabled: Boolean) = store.edit { it[KEY_KEEP_SCREEN_ON] = enabled }
    suspend fun setDroneName(name: String) = store.edit { it[KEY_DRONE_NAME] = name.trim() }
    suspend fun setDroneModel(model: String) = store.edit { it[KEY_DRONE_MODEL] = model.trim() }
    suspend fun setDroneImageUrl(url: String?) = store.edit {
        if (url.isNullOrBlank()) it.remove(KEY_DRONE_IMAGE_URL) else it[KEY_DRONE_IMAGE_URL] = url
    }
    suspend fun setEnvironment(environment: String) = store.edit {
        it[KEY_ENVIRONMENT] = environment
    }
    suspend fun setDarkMode(enabled: Boolean) = store.edit { it[KEY_DARK_MODE] = enabled }
    suspend fun setJoystickDeadzone(value: Float) = store.edit { it[KEY_JOYSTICK_DEADZONE] = (value * 1000).toInt() }
    suspend fun setJoystickSensitivity(value: Float) = store.edit { it[KEY_JOYSTICK_SENSITIVITY] = (value * 1000).toInt() }
    suspend fun setBaseUrl(url: String) = store.edit { it[KEY_BASE_URL] = url.trim() }

    // ----- Misiones guardadas -----
    private val gson = com.google.gson.Gson()

    val missionsFlow: Flow<List<com.drinix.gcs.data.model.SavedMission>> =
        store.data.map { prefs ->
            val json = prefs[KEY_MISSIONS] ?: return@map emptyList()
            try {
                val type = object : com.google.gson.reflect.TypeToken<List<com.drinix.gcs.data.model.SavedMission>>() {}.type
                gson.fromJson<List<com.drinix.gcs.data.model.SavedMission>>(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun saveMission(mission: com.drinix.gcs.data.model.SavedMission) = store.edit { prefs ->
        val currentJson = prefs[KEY_MISSIONS]
        val current = try {
            val type = object : com.google.gson.reflect.TypeToken<List<com.drinix.gcs.data.model.SavedMission>>() {}.type
            gson.fromJson<List<com.drinix.gcs.data.model.SavedMission>>(currentJson, type) ?: emptyList()
        } catch (e: Exception) { emptyList<com.drinix.gcs.data.model.SavedMission>() }
        prefs[KEY_MISSIONS] = gson.toJson(current + mission)
    }

    suspend fun deleteMission(name: String) = store.edit { prefs ->
        val currentJson = prefs[KEY_MISSIONS] ?: return@edit
        try {
            val type = object : com.google.gson.reflect.TypeToken<List<com.drinix.gcs.data.model.SavedMission>>() {}.type
            val current: List<com.drinix.gcs.data.model.SavedMission> =
                gson.fromJson(currentJson, type) ?: emptyList()
            prefs[KEY_MISSIONS] = gson.toJson(current.filterNot { it.name == name })
        } catch (_: Exception) { }
    }

    companion object {
        const val DEFAULT_HOST = "172.20.10.2"
        const val DEFAULT_PORT = 8000
        const val DEFAULT_DRONE_NAME = "DRON-01"

        private val KEY_HOST = stringPreferencesKey("host_ip")
        private val KEY_PORT = intPreferencesKey("port")
        private val KEY_TOKEN = stringPreferencesKey("jwt_token")
        private val KEY_EXPIRES_AT = longPreferencesKey("token_expires_at")
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        private val KEY_DRONE_NAME = stringPreferencesKey("drone_name")
        private val KEY_DRONE_MODEL = stringPreferencesKey("drone_model")
        private val KEY_DRONE_IMAGE_URL = stringPreferencesKey("drone_image_url")
        private val KEY_ENVIRONMENT = stringPreferencesKey("environment")
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        private val KEY_JOYSTICK_DEADZONE = intPreferencesKey("joystick_deadzone")
        private val KEY_JOYSTICK_SENSITIVITY = intPreferencesKey("joystick_sensitivity")
        private val KEY_BASE_URL = stringPreferencesKey("base_url")
        private val KEY_MISSIONS = stringPreferencesKey("saved_missions")
        const val DEFAULT_ENVIRONMENT = "outdoor"
        const val DEFAULT_DRONE_MODEL = "ArduPilot Pixhawk"
        const val DEFAULT_DARK_MODE = true
        const val DEFAULT_JOYSTICK_DEADZONE_INT = 100 // 0.1f
        const val DEFAULT_JOYSTICK_SENSITIVITY_INT = 1000 // 1.0f
        const val DEFAULT_BASE_URL = "http://Raberry-innovacion.local:8000"
    }
}
