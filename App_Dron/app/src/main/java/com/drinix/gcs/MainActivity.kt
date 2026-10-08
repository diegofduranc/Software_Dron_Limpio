package com.drinix.gcs

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.drinix.gcs.theme.DrinixTheme
import com.drinix.gcs.ui.DroneViewModel
import com.drinix.gcs.ui.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Tema oscuro: íconos de la barra de estado claros para que no se oscurezcan
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            DrinixTheme {
                val vm: DroneViewModel = hiltViewModel()

                // Solicitar permisos de ubicación y archivos al iniciar la app
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { result ->
                    // Si concedieron ubicación, anclar la simulación cerca del usuario
                    if (result.values.any { it }) alignMockToDeviceLocation(vm)
                }
                LaunchedEffect(Unit) {
                    // Si ya tenía permiso, alinear con la ubicación sin volver a pedir
                    alignMockToDeviceLocation(vm)
                    val permissions = buildList {
                        add(Manifest.permission.ACCESS_FINE_LOCATION)
                        add(Manifest.permission.ACCESS_COARSE_LOCATION)
                        when {
                            Build.VERSION.SDK_INT >= 33 -> {
                                add(Manifest.permission.READ_MEDIA_IMAGES)
                                add(Manifest.permission.READ_MEDIA_VIDEO)
                            }
                            Build.VERSION.SDK_INT <= 32 -> {
                                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                                if (Build.VERSION.SDK_INT <= 28) {
                                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                }
                            }
                        }
                    }
                    permissionLauncher.launch(permissions.toTypedArray())
                }
                // FLAG_KEEP_SCREEN_ON según preferencia
                val keepOn by vm.keepScreenOnFlow.collectAsState(initial = false)
                if (keepOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(viewModel = vm)
                }
            }
        }
    }

    /** Ancla la telemetría simulada a la última ubicación conocida del dispositivo. */
    @android.annotation.SuppressLint("MissingPermission")
    private fun alignMockToDeviceLocation(vm: DroneViewModel) {
        val fine = androidx.core.content.ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarse = androidx.core.content.ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return
        com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this)
            .lastLocation.addOnSuccessListener { loc ->
                if (loc != null) vm.setMockBaseLocation(loc.latitude, loc.longitude)
            }
    }
}
