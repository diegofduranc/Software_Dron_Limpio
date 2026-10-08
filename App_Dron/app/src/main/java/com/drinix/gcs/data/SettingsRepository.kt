package com.drinix.gcs.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
    val keepScreenOnFlow: Flow<Boolean> = store.data.map { it[KEY_KEEP_SCREEN_ON] ?: false }
    val droneNameFlow: Flow<String> = store.data.map { it[KEY_DRONE_NAME] ?: DEFAULT_DRONE_NAME }

    suspend fun setHost(host: String) = store.edit { it[KEY_HOST] = host }
    suspend fun setPort(port: Int) = store.edit { it[KEY_PORT] = port }
    suspend fun setToken(token: String?) = store.edit {
        if (token == null) it.remove(KEY_TOKEN) else it[KEY_TOKEN] = token
    }
    suspend fun setKeepScreenOn(enabled: Boolean) = store.edit { it[KEY_KEEP_SCREEN_ON] = enabled }
    suspend fun setDroneName(name: String) = store.edit { it[KEY_DRONE_NAME] = name.trim() }

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
        private val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        private val KEY_DRONE_NAME = stringPreferencesKey("drone_name")
        private val KEY_MISSIONS = stringPreferencesKey("saved_missions")
    }
}
