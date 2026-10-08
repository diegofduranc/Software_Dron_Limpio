package com.drinix.gcs.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.drinix.gcs.data.SettingsRepository
import com.drinix.gcs.data.model.ConnectionState
import com.drinix.gcs.theme.AmberWarning
import com.drinix.gcs.theme.Color1
import com.drinix.gcs.theme.Color2
import com.drinix.gcs.theme.ErrorRed
import com.drinix.gcs.theme.GreenOk
import com.drinix.gcs.theme.IconCyan
import com.drinix.gcs.theme.IconLilac
import com.drinix.gcs.theme.TextSecondary
import com.drinix.gcs.ui.DroneViewModel
import com.drinix.gcs.ui.common.AppIcons
import com.drinix.gcs.ui.common.MetricCard
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: DroneViewModel,
    onOpenSettings: () -> Unit,
    onOpenAlerts: () -> Unit,
) {
    val telemetry by viewModel.telemetry.collectAsState()
    val connState by viewModel.connectionState.collectAsState()
    val droneName by viewModel.droneNameFlow.collectAsState(initial = SettingsRepository.DEFAULT_DRONE_NAME)
    val distance by viewModel.distanceToHome.collectAsState()
    val flightSeconds by viewModel.flightSeconds.collectAsState()

    val connected = connState == ConnectionState.CONNECTED
    val connecting = connState == ConnectionState.CONNECTING

    var editName by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Barra de marca estilo mockup: menú + logo + engranaje
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color2.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(AppIcons.Drone, contentDescription = null, tint = Color1)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Drinix GCS", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Telemetría de Drones",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onOpenAlerts) {
                    Icon(Icons.Filled.Menu, contentDescription = "Alertas")
                }
            },
            actions = {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Ajustes")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // --- Tarjeta del dron (nombre editable) ---
            item(span = { GridItemSpan(3) }) {
                DroneCard(
                    name = droneName,
                    id = "${viewModel.host}:${viewModel.port}",
                    mode = telemetry?.mode,
                    armed = telemetry?.armed,
                    connected = connected,
                    onEditName = { editName = true },
                )
            }

            // --- Estado de conexión ---
            item(span = { GridItemSpan(3) }) {
                Text("Estado de conexión", style = MaterialTheme.typography.titleSmall)
            }
            item(span = { GridItemSpan(3) }) {
                Button(
                    onClick = { viewModel.toggleConnection() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Filled.Link, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            connecting -> "Conectando..."
                            connected -> "Desconectar"
                            else -> "Conectar"
                        }
                    )
                }
            }

            // --- Grid 3x2 de métricas con iconos propios ---
            item {
                val batt = telemetry?.batteryRemaining
                val battColor = when {
                    batt == null || !connected -> TextSecondary
                    batt < 20 -> ErrorRed
                    batt < 40 -> AmberWarning
                    else -> GreenOk
                }
                MetricCard(
                    title = "Batería dron",
                    icon = AppIcons.Battery,
                    iconTint = battColor,
                    value = if (connected && batt != null) "$batt %" else "--",
                    progress = batt?.let { it / 100f },
                    progressColor = battColor,
                )
            }
            item {
                val sats = telemetry?.satellites
                MetricCard(
                    title = "GPS",
                    icon = AppIcons.Satellite,
                    iconTint = IconCyan,
                    value = if (connected) "${sats ?: 0} satélites" else "--",
                )
            }
            item {
                MetricCard(
                    title = "Señal",
                    icon = AppIcons.Signal,
                    iconTint = if (connected) GreenOk else TextSecondary,
                    value = if (connected) "OK" else "--",
                )
            }
            item {
                MetricCard(
                    title = "Altitud",
                    icon = AppIcons.Altitude,
                    iconTint = IconLilac,
                    value = metric(connected) { telemetry?.alt?.let { fmt(it, 0) + " m" } },
                )
            }
            item {
                MetricCard(
                    title = "Velocidad",
                    icon = AppIcons.Speed,
                    iconTint = IconLilac,
                    value = metric(connected) { telemetry?.groundspeed?.let { fmt(it, 1) + " m/s" } },
                )
            }
            item {
                MetricCard(
                    title = "Distancia",
                    icon = AppIcons.Distance,
                    iconTint = IconLilac,
                    value = metric(connected) { distance?.let { "${it.roundToInt()} m" } },
                )
            }

            item(span = { GridItemSpan(3) }) {
                if (flightSeconds > 0) {
                    Text(
                        "Tiempo de vuelo: ${formatDuration(flightSeconds)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    // Diálogo para personalizar el nombre del dron
    if (editName) {
        var name by remember(droneName) { mutableStateOf(droneName) }
        AlertDialog(
            onDismissRequest = { editName = false },
            title = { Text("Nombre del dron") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    singleLine = true,
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) viewModel.setDroneName(name)
                        editName = false
                    },
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { editName = false }) { Text("Cancelar") }
            },
        )
    }
}

private inline fun metric(connected: Boolean, crossinline value: () -> String?): String =
    if (connected) value() ?: "--" else "--"

@Composable
private fun DroneCard(
    name: String,
    id: String,
    mode: String?,
    armed: Boolean?,
    connected: Boolean,
    onEditName: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color2.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    AppIcons.Drone,
                    contentDescription = null,
                    tint = Color1,
                    modifier = Modifier.size(38.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onEditName, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Editar nombre",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Text(
                    "Modelo: ArduPilot",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Text(
                    "ID: $id",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                if (mode != null) {
                    Text(
                        "Modo: $mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
            val (label, color) = when {
                !connected -> "Desconectado" to Color.Gray
                armed == true -> "Armado" to ErrorRed
                else -> "Listo" to GreenOk
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(color, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            }
        }
    }
}

private fun fmt(v: Double, decimals: Int): String =
    String.format(Locale.US, "%.${decimals}f", v)

fun formatDuration(totalSeconds: Long): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", m, s)
}
