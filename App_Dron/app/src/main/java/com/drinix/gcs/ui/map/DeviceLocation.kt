package com.drinix.gcs.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState

/** Punto de partida por defecto del mapa: Bogotá, Colombia. */
val BOGOTA = LatLng(4.7110, -74.0721)
const val DEFAULT_ZOOM = 16f

/** Estado de ubicación del dispositivo + acciones para centrar el mapa en ella. */
class DeviceLocationState(
    private val context: Context,
    private val permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    /** Callback temporal a ejecutar cuando se conceda el permiso. */
    private var onPermissionGranted: (() -> Unit)? = null,
) {
    var hasPermission by mutableStateOf(hasLocationPermission(context))
        private set

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    fun onPermissionResult(granted: Map<String, Boolean>) {
        hasPermission = granted.values.any { it }
        if (hasPermission) onPermissionGranted?.invoke()
        onPermissionGranted = null
    }

    /** Centra la cámara en la ubicación GPS real del dispositivo. */
    fun centerOnDevice(camera: CameraPositionState) {
        if (!hasPermission) {
            onPermissionGranted = { centerOnDevice(camera) }
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
            return
        }
        fetchLastLocation { latLng ->
            camera.move(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM))
        }
    }

    /** Última ubicación conocida (o la actual si no hay caché). */
    @SuppressLint("MissingPermission")
    fun fetchLastLocation(onResult: (LatLng) -> Unit) {
        if (!hasLocationPermission(context)) return
        fusedClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                onResult(LatLng(loc.latitude, loc.longitude))
            } else {
                fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener { cur ->
                        if (cur != null) onResult(LatLng(cur.latitude, cur.longitude))
                    }
            }
        }
    }
}

@Composable
fun rememberDeviceLocationState(): DeviceLocationState {
    val context = LocalContext.current
    lateinit var state: DeviceLocationState
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> state.onPermissionResult(result) }
    state = remember { DeviceLocationState(context.applicationContext, launcher) }
    return state
}

private fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}
