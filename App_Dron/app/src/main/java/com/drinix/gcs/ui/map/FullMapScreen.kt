package com.drinix.gcs.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.drinix.gcs.theme.GreenOk
import com.drinix.gcs.theme.TextSecondary
import com.drinix.gcs.ui.DroneViewModel
import com.drinix.gcs.ui.common.GcsTopBar
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Locale

/** Mapa a pantalla completa con leyenda, zoom y long-press para goto (mockup derecho). */
@Composable
fun FullMapScreen(
    viewModel: DroneViewModel,
    onBack: () -> Unit,
) {
    val telemetry by viewModel.telemetry.collectAsState()

    // Siempre iniciar en Bogotá; no saltar a la telemetría del dron
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(BOGOTA, DEFAULT_ZOOM)
    }
    val deviceLocation = rememberDeviceLocationState()

    var pendingGoto by remember { mutableStateOf<LatLng?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = com.google.maps.android.compose.MapProperties(
                mapType = com.google.maps.android.compose.MapType.SATELLITE,
                isMyLocationEnabled = deviceLocation.hasPermission,
            ),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, compassEnabled = true),
            onMapLongClick = { latLng -> pendingGoto = latLng },
        ) {
            DroneMapContent(viewModel, telemetry)
        }

        // Barra superior
        GcsTopBar(title = "Mapa de vuelo", onBack = onBack)

        // Leyenda: abajo-izquierda en vertical; arriba-izquierda compacta en horizontal
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(if (isLandscape) Alignment.TopStart else Alignment.BottomStart)
                .padding(
                    start = 12.dp,
                    top = if (isLandscape) 64.dp else 0.dp,
                    bottom = if (isLandscape) 0.dp else 76.dp,
                ),
        ) {
            Column(modifier = Modifier.padding(if (isLandscape) 8.dp else 10.dp)) {
                LegendRow(Color(0xFF2F7CF6), "Ruta del vuelo")
                LegendRow(Color(0xFF2F7CF6), "Tu dron (azul)")
                LegendRow(Color(0xFFEF4444), "Dirección de vuelo (nariz roja)")
                LegendRow(GreenOk, "Punto de despegue (Home)")
                LegendRow(Color(0xFF38BDF8).copy(alpha = 0.5f), "Zona de operación")
            }
        }

        // Altitud: barra inferior en vertical; chip compacto abajo-derecha en horizontal
        if (isLandscape) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Filled.Flight, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        telemetry?.alt?.let { String.format(Locale.US, "%.0f m", it) } ?: "--",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp),
                ) {
                    Icon(Icons.Filled.Flight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Altitud sobre el terreno", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Text(
                            telemetry?.alt?.let { String.format(Locale.US, "%.0f m", it) } ?: "--",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
            }
        }

        // Zoom +/- derecha
        Column(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ZoomButton("+") { cameraPositionState.move(CameraUpdateFactory.zoomIn()) }
            ZoomButton("−") { cameraPositionState.move(CameraUpdateFactory.zoomOut()) }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp),
            ) {
                IconButton(onClick = { deviceLocation.centerOnDevice(cameraPositionState) }) {
                    Icon(Icons.Filled.MyLocation, contentDescription = "Mi ubicación")
                }
            }
        }
    }

    // Diálogo ¿Volar aquí?
    pendingGoto?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingGoto = null },
            title = { Text("¿Volar aquí?") },
            text = {
                Text(
                    "Destino: %.5f, %.5f\nAltitud: %.0f m".format(
                        target.latitude, target.longitude, telemetry?.alt ?: 10.0,
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.goto(
                        target.latitude,
                        target.longitude,
                        telemetry?.alt?.takeIf { it > 0 } ?: 10.0,
                    )
                    pendingGoto = null
                }) { Text("VOLAR") }
            },
            dismissButton = {
                TextButton(onClick = { pendingGoto = null }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun LegendRow(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp),
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ZoomButton(symbol: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp),
    ) {
        IconButton(onClick = onClick) {
            Icon(
                if (symbol == "+") Icons.Filled.Add else Icons.Filled.Remove,
                contentDescription = if (symbol == "+") "Acercar" else "Alejar",
            )
        }
    }
}
