package com.drinix.gcs.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.drinix.gcs.data.model.Telemetry
import com.drinix.gcs.theme.BlueAccent
import com.drinix.gcs.theme.GreenOk
import com.drinix.gcs.ui.DroneViewModel
import com.drinix.gcs.ui.common.ConnectionChip
import com.drinix.gcs.ui.common.MetricCard
import com.drinix.gcs.ui.common.StatusBanner
import com.drinix.gcs.ui.home.formatDuration
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Locale

/** Pestaña Mapa: mini mapa + tarjetas de telemetría en vuelo (mockup central). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: DroneViewModel,
    onExpandMap: () -> Unit,
) {
    val telemetry by viewModel.telemetry.collectAsState()
    val connState by viewModel.connectionState.collectAsState()
    val distance by viewModel.distanceToHome.collectAsState()
    val flightSeconds by viewModel.flightSeconds.collectAsState()

    // Siempre iniciar en Bogotá; no saltar a la telemetría del dron
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(BOGOTA, DEFAULT_ZOOM)
    }
    val deviceLocation = rememberDeviceLocationState()

    // Altura del mini mapa adaptable: ~32% de la pantalla (se adapta a rotación y dispositivo)
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val mapHeight = (maxHeight * 0.32f).coerceIn(160.dp, 320.dp)
        Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Telemetría en vuelo", style = MaterialTheme.typography.titleMedium) },
            navigationIcon = {
                Icon(Icons.Filled.Menu, contentDescription = null, modifier = Modifier.padding(start = 16.dp))
            },
            actions = {
                ConnectionChip(connState)
                val batt = telemetry?.batteryRemaining
                if (batt != null) {
                    Text(
                        " $batt%",
                        color = if (batt < 20) MaterialTheme.colorScheme.error
                        else GreenOk,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(end = 16.dp),
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )

        // Mini mapa
        Box(modifier = Modifier.fillMaxWidth().height(mapHeight)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = com.google.maps.android.compose.MapProperties(
                    mapType = com.google.maps.android.compose.MapType.SATELLITE,
                    isMyLocationEnabled = deviceLocation.hasPermission,
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    compassEnabled = false,
                    myLocationButtonEnabled = false,
                ),
            ) {
                DroneMapContent(viewModel, telemetry)
            }
            // Acciones flotantes
            Column(
                modifier = Modifier.align(Alignment.CenterEnd).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MapActionButton(Icons.Filled.Layers, "Pantalla completa", onExpandMap)
                MapActionButton(Icons.Filled.MyLocation, "Mi ubicación") {
                    deviceLocation.centerOnDevice(cameraPositionState)
                }
            }
        }

        // Tarjetas de telemetría
        val banner = flightStatusBanner(telemetry)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 110.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                MetricCard(
                    "Altitud", Icons.Filled.FlightTakeoff,
                    telemetry?.alt?.let { fmt(it, 0) + " m" } ?: "--",
                )
            }
            item {
                MetricCard(
                    "Velocidad", Icons.Filled.Speed,
                    telemetry?.groundspeed?.let { fmt(it, 1) + " m/s" } ?: "--",
                )
            }
            item {
                MetricCard(
                    "Dist. a Home", Icons.Filled.Straighten,
                    distance?.let { fmt(it, 0) + " m" } ?: "--",
                )
            }
            item {
                val batt = telemetry?.batteryRemaining
                MetricCard(
                    "Batería dron", Icons.Filled.BatteryStd,
                    batt?.let { "$it %" } ?: "--",
                    progress = batt?.let { it / 100f },
                )
            }
            item {
                MetricCard(
                    "Satélites GPS", Icons.Filled.Satellite,
                    telemetry?.satellites?.let { "$it sat" } ?: "--",
                )
            }
            item {
                MetricCard(
                    "Rumbo", Icons.Filled.Explore,
                    telemetry?.heading?.let { fmt(it, 0) + "°" } ?: "--",
                )
            }
            // Banner de estado de vuelo
            item(span = { GridItemSpan(maxLineSpan) }) {
                StatusBanner(level = banner.first, text = banner.second)
            }
            item {
                MetricCard(
                    "Tiempo de vuelo", Icons.Filled.Timer,
                    formatDuration(flightSeconds),
                )
            }
        }
        }
    }
}

@Composable
private fun MapActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    SmallFloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = BlueAccent,
    ) {
        Icon(icon, contentDescription = label)
    }
}

/** Determina el banner: 2 crítico, 1 advertencia, 0 normal. */
private fun flightStatusBanner(t: Telemetry?): Pair<Int, String> {
    val batt = t?.batteryRemaining
    val sats = t?.satellites
    return when {
        t == null -> 0 to "Esperando telemetría..."
        batt != null && batt < 20 -> 2 to "¡Batería baja! Considera aterrizar"
        t.armed == true && sats != null && sats < 6 -> 1 to "Vuelo con pocos satélites GPS"
        t.armed == true -> 0 to "Vuelo normal"
        else -> 0 to "Dron desarmado"
    }
}

@Composable
fun MapActionButtonIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Icon(icon, contentDescription = null)
}

private fun fmt(v: Double, decimals: Int): String =
    String.format(Locale.US, "%.${decimals}f", v)
