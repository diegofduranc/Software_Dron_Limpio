package com.drinix.gcs.ui.telemetry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.drinix.gcs.data.model.ConnectionState
import com.drinix.gcs.theme.AmberWarning
import com.drinix.gcs.theme.ErrorRed
import com.drinix.gcs.theme.TextPrimary
import com.drinix.gcs.ui.DroneViewModel
import com.drinix.gcs.ui.common.ConnectionChip
import com.drinix.gcs.ui.common.MetricCard
import com.drinix.gcs.ui.common.vibrate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelemetryScreen(viewModel: DroneViewModel) {
    val telemetry by viewModel.telemetry.collectAsState()
    val connState by viewModel.connectionState.collectAsState()
    val context = LocalContext.current

    // Vibración al entrar en batería baja (solo en el flanco)
    var lowBattAlerted by remember { mutableStateOf(false) }
    val batt = telemetry?.batteryRemaining
    LaunchedEffect(batt) {
        if (batt != null && batt < 20 && !lowBattAlerted) {
            lowBattAlerted = true
            vibrate(context, 600)
        } else if (batt != null && batt >= 20) {
            lowBattAlerted = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Telemetría", style = MaterialTheme.typography.titleMedium) },
            navigationIcon = {
                Icon(Icons.Filled.Menu, contentDescription = null, modifier = Modifier.padding(start = 16.dp))
            },
            actions = { ConnectionChip(connState) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )

        if (connState == ConnectionState.DISCONNECTED) {
            OutlinedButton(
                onClick = { viewModel.reconnect() },
                modifier = Modifier.padding(horizontal = 16.dp),
            ) { Text("Reintentar conexión") }
        }

        val t = telemetry
        val sats = t?.satellites
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(2) }) {
                MetricCard(
                    title = "Batería",
                    icon = Icons.Filled.BatteryStd,
                    value = batt?.let { "$it %" } ?: "--",
                    subtitle = t?.voltage?.let { fmt(it, 2) + " V" },
                    valueColor = if (batt != null && batt < 20) ErrorRed else TextPrimary,
                    progress = batt?.let { it / 100f },
                    progressColor = if (batt != null && batt < 20) ErrorRed
                    else MaterialTheme.colorScheme.secondary,
                )
            }
            item {
                MetricCard(
                    title = "GPS",
                    icon = Icons.Filled.Satellite,
                    value = if (t?.lat != null && t.lon != null)
                        "${fmt(t.lat, 5)}, ${fmt(t.lon, 5)}" else "--",
                    subtitle = "Sats: ${sats ?: "--"} · Fix: ${fixLabel(t?.fixType)}",
                    valueColor = if (sats != null && sats < 6) AmberWarning else TextPrimary,
                )
            }
            item {
                MetricCard(
                    title = "Altitud",
                    icon = Icons.Filled.FlightTakeoff,
                    value = t?.alt?.let { fmt(it, 1) + " m" } ?: "--",
                )
            }
            item {
                MetricCard(
                    title = "Velocidad",
                    icon = Icons.Filled.Speed,
                    value = t?.groundspeed?.let { fmt(it, 1) + " m/s" } ?: "--",
                )
            }
            item {
                MetricCard(
                    title = "Modo de vuelo",
                    icon = Icons.Filled.ToggleOn,
                    value = t?.mode ?: "--",
                )
            }
            item {
                val armed = t?.armed
                MetricCard(
                    title = "Estado",
                    icon = Icons.Filled.PowerSettingsNew,
                    value = when (armed) {
                        true -> "ARMADO"
                        false -> "DESARMADO"
                        null -> "--"
                    },
                    valueColor = if (armed == true) ErrorRed else TextPrimary,
                )
            }
            item {
                MetricCard(
                    title = "Heading",
                    icon = Icons.Filled.Explore,
                    value = t?.heading?.let { fmt(it, 0) + "°" } ?: "--",
                )
            }
            item {
                MetricCard(
                    title = "WP activo",
                    icon = Icons.Filled.FormatListNumbered,
                    value = t?.seq?.toString() ?: "--",
                )
            }
        }
    }
}

private fun fmt(v: Double, decimals: Int): String =
    String.format(Locale.US, "%.${decimals}f", v)

private fun fixLabel(fix: Int?): String = when (fix) {
    null -> "--"
    0, 1 -> "sin fix"
    2 -> "2D"
    3 -> "3D"
    4 -> "DGPS"
    5 -> "RTK Float"
    6 -> "RTK Fix"
    else -> fix.toString()
}
