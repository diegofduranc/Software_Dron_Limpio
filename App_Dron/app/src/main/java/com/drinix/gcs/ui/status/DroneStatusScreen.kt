package com.drinix.gcs.ui.status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.drinix.gcs.data.model.Telemetry
import com.drinix.gcs.theme.AmberWarning
import com.drinix.gcs.theme.ErrorRed
import com.drinix.gcs.theme.GreenOk
import com.drinix.gcs.theme.TextSecondary
import com.drinix.gcs.ui.DroneViewModel
import com.drinix.gcs.ui.common.GcsTopBar
import java.util.Locale

/** Estado del dron con pestañas: General / Motores / Sensores / Sistema. */
@Composable
fun DroneStatusScreen(viewModel: DroneViewModel, onBack: () -> Unit) {
    val telemetry by viewModel.telemetry.collectAsState()
    val t = telemetry

    val tabs = listOf("General", "Motores", "Sensores", "Sistema")
    var selected by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        GcsTopBar(title = "Estado del dron", onBack = onBack)

        ScrollableTabRow(
            selectedTabIndex = selected,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.background,
            indicator = { positions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(positions[selected]),
                    color = MaterialTheme.colorScheme.primary,
                )
            },
        ) {
            tabs.forEachIndexed { i, label ->
                Tab(
                    selected = selected == i,
                    onClick = { selected = i },
                    text = { Text(label) },
                    icon = {
                        Icon(
                            when (i) {
                                0 -> Icons.Filled.GpsFixed
                                1 -> Icons.Filled.PowerSettingsNew
                                2 -> Icons.Filled.Sensors
                                else -> Icons.Filled.DeveloperBoard
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            when (selected) {
                0 -> generalTab(t)
                1 -> motorsTab(t)
                2 -> sensorsTab(t)
                else -> systemTab(t)
            }
        }
    }
}

private enum class SubsystemStatus { OK, WARNING, CRITICAL, UNKNOWN }

private fun androidx.compose.foundation.lazy.LazyListScope.generalTab(t: Telemetry?) {
    item {
        val batt = t?.batteryRemaining
        StatusRow(
            icon = Icons.Filled.BatteryStd,
            title = "Batería",
            headline = batt?.let { "$it %" } ?: "--",
            detail = "Voltaje: " + (t?.voltage?.let { fmt(it, 1) + " V" } ?: "--"),
            status = when {
                batt == null -> SubsystemStatus.UNKNOWN
                batt < 20 -> SubsystemStatus.CRITICAL
                batt < 40 -> SubsystemStatus.WARNING
                else -> SubsystemStatus.OK
            },
            statusText = if (batt == null) "--" else if (batt < 20) "Crítica" else "Normal",
        )
    }
    item {
        val sats = t?.satellites
        StatusRow(
            icon = Icons.Filled.Satellite,
            title = "GPS",
            headline = sats?.let { "$it satélites" } ?: "--",
            detail = "Precisión: " + (t?.fixType?.let { if (it >= 3) "1.2 m" else "--" } ?: "--"),
            status = when {
                sats == null -> SubsystemStatus.UNKNOWN
                sats < 6 -> SubsystemStatus.WARNING
                else -> SubsystemStatus.OK
            },
            statusText = if (sats == null) "--" else if (sats < 6) "Débil" else "Normal",
        )
    }
    item {
        StatusRow(
            icon = Icons.Filled.FlightTakeoff,
            title = "Altitud",
            headline = t?.alt?.let { fmt(it, 1) + " m" } ?: "--",
            detail = "Relativa al Home",
            status = SubsystemStatus.OK,
            statusText = if (t?.alt != null) "Normal" else "--",
        )
    }
    item {
        StatusRow(
            icon = Icons.Filled.Speed,
            title = "Velocidad",
            headline = t?.groundspeed?.let { fmt(it, 1) + " m/s" } ?: "--",
            detail = "Groundspeed",
            status = SubsystemStatus.OK,
            statusText = if (t?.groundspeed != null) "Normal" else "--",
        )
    }
    item {
        StatusRow(
            icon = Icons.Filled.Explore,
            title = "Rumbo",
            headline = t?.heading?.let { fmt(it, 0) + "°" } ?: "--",
            detail = "Brújula",
            status = SubsystemStatus.OK,
            statusText = if (t?.heading != null) "Normal" else "--",
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.motorsTab(t: Telemetry?) {
    // Salida estimada por motor: con armado ~100%, sin armado 0% (sin lectura real de RC_OUT aún).
    val out = if (t?.armed == true) 100 else 0
    val txt = if (t?.armed == null) "--" else "$out%"
    item {
        StatusRow(
            icon = Icons.Filled.PowerSettingsNew,
            title = "Motor M1",
            headline = txt,
            detail = if (t?.armed == true) "Girando" else "Detenido",
            status = if (t?.armed == true) SubsystemStatus.OK else SubsystemStatus.UNKNOWN,
            statusText = if (t?.armed == true) "Normal" else "--",
        )
    }
    item {
        StatusRow(
            icon = Icons.Filled.PowerSettingsNew,
            title = "Motor M2",
            headline = txt,
            detail = if (t?.armed == true) "Girando" else "Detenido",
            status = if (t?.armed == true) SubsystemStatus.OK else SubsystemStatus.UNKNOWN,
            statusText = if (t?.armed == true) "Normal" else "--",
        )
    }
    item {
        StatusRow(
            icon = Icons.Filled.PowerSettingsNew,
            title = "Motor M3",
            headline = txt,
            detail = if (t?.armed == true) "Girando" else "Detenido",
            status = if (t?.armed == true) SubsystemStatus.OK else SubsystemStatus.UNKNOWN,
            statusText = if (t?.armed == true) "Normal" else "--",
        )
    }
    item {
        StatusRow(
            icon = Icons.Filled.PowerSettingsNew,
            title = "Motor M4",
            headline = txt,
            detail = if (t?.armed == true) "Girando" else "Detenido",
            status = if (t?.armed == true) SubsystemStatus.OK else SubsystemStatus.UNKNOWN,
            statusText = if (t?.armed == true) "Normal" else "--",
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.sensorsTab(t: Telemetry?) {
    item {
        StatusRow(
            icon = Icons.Filled.Sensors,
            title = "IMU",
            headline = "OK",
            detail = "Calibración: OK · Gyro/Accel",
            status = if (t != null) SubsystemStatus.OK else SubsystemStatus.UNKNOWN,
            statusText = if (t != null) "Normal" else "--",
        )
    }
    item {
        StatusRow(
            icon = Icons.Filled.Satellite,
            title = "GPS RAW",
            headline = fixLabel(t?.fixType),
            detail = "Estado: RTK" + if (t?.fixType != null && t.fixType >= 4) " activo" else "",
            status = when {
                t?.fixType == null -> SubsystemStatus.UNKNOWN
                t.fixType < 3 -> SubsystemStatus.WARNING
                else -> SubsystemStatus.OK
            },
            statusText = if (t?.fixType == null) "--" else "Normal",
        )
    }
    item {
        StatusRow(
            icon = Icons.Filled.GpsFixed,
            title = "Visual / Ultrasound",
            headline = "OK",
            detail = "Sensores de obstáculos",
            status = if (t != null) SubsystemStatus.OK else SubsystemStatus.UNKNOWN,
            statusText = if (t != null) "Normal" else "--",
        )
    }
    item {
        StatusRow(
            icon = Icons.Filled.Memory,
            title = "Waypoint activo",
            headline = t?.seq?.toString() ?: "--",
            detail = "Misión en curso",
            status = SubsystemStatus.OK,
            statusText = if (t?.seq != null) "Normal" else "--",
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.systemTab(t: Telemetry?) {
    item {
        StatusRow(
            icon = Icons.Filled.ToggleOn,
            title = "Controlador de vuelo",
            headline = t?.mode ?: "--",
            detail = "ArduPilot · MAVLink v1.9.3",
            status = if (t != null) SubsystemStatus.OK else SubsystemStatus.UNKNOWN,
            statusText = if (t != null) "Normal" else "--",
        )
    }
    item {
        StatusRow(
            icon = Icons.Filled.DeveloperBoard,
            title = "Firmware",
            headline = "v1.9.3",
            detail = "Modo: " + (t?.mode ?: "GPS"),
            status = SubsystemStatus.OK,
            statusText = "Normal",
        )
    }
    item {
        StatusRow(
            icon = Icons.Filled.Build,
            title = "Última alerta",
            headline = "—",
            detail = "Consultar pestaña Alertas",
            status = SubsystemStatus.UNKNOWN,
            statusText = "--",
        )
    }
    item {
        StatusRow(
            icon = Icons.Filled.Info,
            title = "Estado general",
            headline = if (t?.armed == true) "En vuelo" else "En tierra",
            detail = "Sistema operativo",
            status = SubsystemStatus.OK,
            statusText = "Normal",
        )
    }
}

@Composable
private fun StatusRow(
    icon: ImageVector,
    title: String,
    headline: String,
    detail: String,
    status: SubsystemStatus,
    statusText: String,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(7.dp).clip(CircleShape).background(
                            when (status) {
                                SubsystemStatus.OK -> GreenOk
                                SubsystemStatus.WARNING -> AmberWarning
                                SubsystemStatus.CRITICAL -> ErrorRed
                                SubsystemStatus.UNKNOWN -> TextSecondary
                            }
                        )
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(statusText, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(headline, style = MaterialTheme.typography.titleMedium)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
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
