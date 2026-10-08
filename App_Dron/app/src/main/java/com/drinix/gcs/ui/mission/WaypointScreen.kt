package com.drinix.gcs.ui.mission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.drinix.gcs.data.model.SavedMission
import com.drinix.gcs.data.model.Waypoint
import com.drinix.gcs.theme.AmberWarning
import com.drinix.gcs.theme.BlueAccent
import com.drinix.gcs.ui.DroneViewModel
import com.drinix.gcs.ui.common.GcsTopBar
import com.drinix.gcs.ui.map.BOGOTA
import com.drinix.gcs.ui.map.DEFAULT_ZOOM
import com.drinix.gcs.ui.map.rememberDeviceLocationState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import java.util.Locale
import kotlinx.coroutines.delay

private const val MAX_WAYPOINTS = 10

/**
 * Misión por waypoints: tocar el mapa crea puntos (máx. 10), altura y velocidad
 * configurables, al terminar pregunta si se cierra la ruta, y lista de misiones
 * guardadas para revisar / cargar.
 */
@Composable
fun WaypointScreen(viewModel: DroneViewModel, onBack: () -> Unit) {
    val telemetry by viewModel.telemetry.collectAsState()
    val savedMissions by viewModel.savedMissions.collectAsState(initial = emptyList())
    val waypoints = remember { mutableListOf<Waypoint>().toMutableStateList() }

    var confirmSave by remember { mutableStateOf(false) }   // ¿cerrar ruta?
    var confirmAuto by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmDeleteMission by remember { mutableStateOf<SavedMission?>(null) }
    var limitMessage by remember { mutableStateOf<String?>(null) }

    // Parámetros de misión configurables por el usuario (sin valores forzados)
    var missionAlt by remember { mutableFloatStateOf(15f) }     // metros
    var missionSpeed by remember { mutableFloatStateOf(5f) }    // m/s

    // Siempre iniciar en Bogotá; no saltar a la telemetría del dron
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(BOGOTA, DEFAULT_ZOOM)
    }
    val deviceLocation = rememberDeviceLocationState()

    Column(modifier = Modifier.fillMaxSize()) {
        GcsTopBar(title = "Misión por waypoints", onBack = onBack)

        Text(
            "Toca el mapa para agregar puntos (máx. $MAX_WAYPOINTS). Ajusta altura y velocidad abajo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        // Mapa: tocar = agregar waypoint
        Box(modifier = Modifier.fillMaxWidth().height(230.dp)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapType = MapType.SATELLITE,
                    isMyLocationEnabled = deviceLocation.hasPermission,
                ),
                uiSettings = MapUiSettings(zoomControlsEnabled = false, compassEnabled = false),
                onMapClick = { latLng ->
                    if (waypoints.size >= MAX_WAYPOINTS) {
                        limitMessage = "Máximo $MAX_WAYPOINTS puntos por misión"
                    } else {
                        val seq = waypoints.size + 1
                        waypoints.add(
                            Waypoint(seq, latLng.latitude, latLng.longitude, missionAlt.toDouble())
                        )
                        limitMessage = null
                    }
                },
            ) {
                // Ruta dibujada en tiempo real (cierra al origen si el usuario lo decidió al guardar)
                if (waypoints.size >= 2) {
                    Polyline(
                        points = waypoints.sortedBy { it.seq }.map { LatLng(it.lat, it.lon) },
                        color = Color(0xFF2F7CF6),
                        width = 5f,
                    )
                }
                waypoints.forEach { wp ->
                    val isActive = telemetry?.seq == wp.seq
                    Marker(
                        state = rememberMarkerState(position = LatLng(wp.lat, wp.lon)),
                        title = "WP ${wp.seq}",
                        snippet = "Alt: ${fmt(wp.alt)} m",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            if (isActive) BitmapDescriptorFactory.HUE_GREEN
                            else BitmapDescriptorFactory.HUE_ORANGE
                        ),
                    )
                }
            }
            // Botón Mi ubicación
            SmallFloatingActionButton(
                onClick = { deviceLocation.centerOnDevice(cameraPositionState) },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = BlueAccent,
                modifier = Modifier.align(Alignment.CenterEnd).padding(8.dp),
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Mi ubicación")
            }
        }

        limitMessage?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // ── Parámetros de la misión ────────────────────────────────
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Parámetros de la misión", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Altura de vuelo: ${fmt(missionAlt.toDouble())} m",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Slider(
                            value = missionAlt,
                            onValueChange = { alt ->
                                missionAlt = alt
                                // Aplicar a todos los waypoints existentes
                                for (i in waypoints.indices) {
                                    waypoints[i] = waypoints[i].copy(alt = alt.toDouble())
                                }
                            },
                            valueRange = 5f..100f,
                        )
                        Text(
                            "Velocidad: ${String.format(Locale.US, "%.1f", missionSpeed)} m/s",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Slider(
                            value = missionSpeed,
                            onValueChange = { missionSpeed = it },
                            valueRange = 1f..15f,
                        )
                    }
                }
            }

            // ── Puntos actuales ────────────────────────────────────────
            item {
                Text(
                    "Waypoints (${waypoints.size}/$MAX_WAYPOINTS)",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            itemsIndexed(
                items = waypoints,
                key = { index, wp -> "wp$index-${wp.lat}-${wp.lon}" },
            ) { index, wp ->
                WaypointRow(
                    waypoint = wp,
                    isActive = telemetry?.seq == wp.seq,
                    onMoveUp = {
                        if (index > 0) {
                            val tmp = waypoints[index]
                            waypoints[index] = waypoints[index - 1]
                            waypoints[index - 1] = tmp
                            renumber(waypoints)
                        }
                    },
                    onMoveDown = {
                        if (index < waypoints.size - 1) {
                            val tmp = waypoints[index]
                            waypoints[index] = waypoints[index + 1]
                            waypoints[index + 1] = tmp
                            renumber(waypoints)
                        }
                    },
                    onDelete = {
                        waypoints.removeAt(index)
                        renumber(waypoints)
                    },
                )
            }

            // ── Misiones guardadas ─────────────────────────────────────
            if (savedMissions.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(6.dp))
                    Text("Misiones guardadas", style = MaterialTheme.typography.titleSmall)
                }
                items(savedMissions, key = { it.name }) { mission ->
                    SavedMissionRow(
                        mission = mission,
                        onLoad = {
                            waypoints.clear()
                            waypoints.addAll(mission.waypoints)
                            mission.waypoints.firstOrNull()?.let { wp ->
                                cameraPositionState.move(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(wp.lat, wp.lon), DEFAULT_ZOOM
                                    )
                                )
                            }
                            missionAlt = mission.waypoints.firstOrNull()?.alt?.toFloat() ?: missionAlt
                            missionSpeed = mission.speed.toFloat()
                        },
                        onUpload = {
                            viewModel.uploadMission(mission.waypoints)
                        },
                        onDelete = { confirmDeleteMission = mission },
                    )
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
                // Guardar → pregunta si cerrar la ruta
                Button(
                    onClick = { confirmSave = true },
                    enabled = waypoints.size >= 2,
                    modifier = Modifier.weight(1f),
                ) { Text("Terminar y guardar") }
                Spacer(Modifier.width(10.dp))
                OutlinedButton(
                    onClick = { confirmAuto = true },
                    enabled = waypoints.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Subir y volar")
                }
            }
            TextButton(
                onClick = { confirmClear = true },
                enabled = waypoints.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Limpiar puntos", color = MaterialTheme.colorScheme.error) }
        }
    }

    // ── Diálogo: ¿cerrar la ruta? ─────────────────────────────────────
    if (confirmSave) {
        AlertDialog(
            onDismissRequest = { confirmSave = false },
            title = { Text("¿Cerrar la ruta?") },
            text = {
                Text(
                    "Si cierras la ruta, el dron volverá al punto 1 después del último waypoint.\n\n" +
                        "Waypoints: ${waypoints.size} · Altura: ${fmt(missionAlt.toDouble())} m · " +
                        "Velocidad: ${String.format(Locale.US, "%.1f", missionSpeed)} m/s"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    saveMission(viewModel, waypoints, missionAlt, missionSpeed, closed = true)
                    waypoints.clear()
                    confirmSave = false
                }) { Text("SÍ, CERRAR") }
            },
            dismissButton = {
                TextButton(onClick = {
                    saveMission(viewModel, waypoints, missionAlt, missionSpeed, closed = false)
                    waypoints.clear()
                    confirmSave = false
                }) { Text("NO, DEJAR ABIERTA") }
            },
        )
    }

    if (confirmAuto) {
        AlertDialog(
            onDismissRequest = { confirmAuto = false },
            title = { Text("Subir y volar") },
            text = { Text("Se subirán ${waypoints.size} waypoints y se cambiará el modo a Automático (misión).") },
            confirmButton = {
                TextButton(onClick = {
                    renumber(waypoints)
                    viewModel.uploadMission(waypoints.toList())
                    viewModel.setMode("AUTO")
                    confirmAuto = false
                }) { Text("SUBIR Y VOLAR") }
            },
            dismissButton = {
                TextButton(onClick = { confirmAuto = false }) { Text("Cancelar") }
            },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Limpiar puntos") },
            text = { Text("Se eliminarán ${waypoints.size} waypoints de la lista.") },
            confirmButton = {
                TextButton(onClick = { waypoints.clear(); confirmClear = false }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancelar") }
            },
        )
    }

    confirmDeleteMission?.let { mission ->
        AlertDialog(
            onDismissRequest = { confirmDeleteMission = null },
            title = { Text("Eliminar misión") },
            text = { Text("¿Eliminar \"${mission.name}\" de las misiones guardadas?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMission(mission.name)
                    confirmDeleteMission = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteMission = null }) { Text("Cancelar") }
            },
        )
    }
}

/** Guarda la misión con nombre automático (Misión N). */
private fun saveMission(
    viewModel: DroneViewModel,
    waypoints: List<Waypoint>,
    alt: Float,
    speed: Float,
    closed: Boolean,
) {
    val finalWps = waypoints.mapIndexed { i, wp -> wp.copy(seq = i + 1, alt = alt.toDouble()) }
        .let { list -> if (closed && list.size >= 2) list + list.first().copy(seq = list.size + 1) else list }
    val name = "Misión ${System.currentTimeMillis() % 100000}"
    viewModel.saveMission(
        SavedMission(
            name = name,
            waypoints = finalWps,
            speed = speed.toDouble(),
            closed = closed,
        )
    )
}

@Composable
private fun SavedMissionRow(
    mission: SavedMission,
    onLoad: () -> Unit,
    onUpload: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(mission.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${mission.waypoints.size} pts · ${fmt(mission.waypoints.firstOrNull()?.alt ?: 0.0)} m · " +
                        "${String.format(Locale.US, "%.1f", mission.speed)} m/s" +
                        if (mission.closed) " · ruta cerrada" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onLoad) { Text("Ver") }
            TextButton(onClick = onUpload) { Text("Subir") }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = AmberWarning)
            }
        }
    }
}

@Composable
private fun WaypointRow(
    waypoint: Waypoint,
    isActive: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(
                "#${waypoint.seq}",
                style = MaterialTheme.typography.titleSmall,
                color = if (isActive) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(36.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "%.5f, %.5f".format(waypoint.lat, waypoint.lon),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Alt: ${fmt(waypoint.alt)} m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(4.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onMoveUp) {
                    Icon(Icons.Filled.ArrowUpward, contentDescription = "Subir")
                }
                IconButton(onClick = onMoveDown) {
                    Icon(Icons.Filled.ArrowDownward, contentDescription = "Bajar")
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = AmberWarning)
            }
        }
    }
}

private fun renumber(list: MutableList<Waypoint>) {
    for (i in list.indices) {
        list[i] = list[i].copy(seq = i + 1)
    }
}

private fun fmt(v: Double): String = String.format(Locale.US, "%.1f", v)
