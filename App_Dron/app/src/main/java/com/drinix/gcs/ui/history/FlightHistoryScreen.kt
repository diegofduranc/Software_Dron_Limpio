package com.drinix.gcs.ui.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.drinix.gcs.theme.AmberWarning
import com.drinix.gcs.theme.Color1
import com.drinix.gcs.theme.Color2
import com.drinix.gcs.theme.GreenOk
import com.drinix.gcs.theme.TextSecondary
import com.drinix.gcs.ui.common.GcsTopBar

/** Registro de un vuelo pasado. */
data class FlightRecord(
    val name: String,
    val date: String,
    val time: String,
    val duration: String,
    val distanceKm: Double,
    val maxAltitudeM: Int,
    val batteryPercent: Int,
    val status: FlightStatus,
)

enum class FlightStatus(val label: String, val color: Color) {
    COMPLETED("Completado", GreenOk),
    INTERRUPTED("Interrumpido", AmberWarning),
}

// Datos de ejemplo (sin backend de historial todavía).
private val sampleFlights = listOf(
    FlightRecord("Vuelo #026", "22 sep. 2025", "15:24", "12:36", 2.8, 120, 65, FlightStatus.COMPLETED),
    FlightRecord("Vuelo #025", "21 sep. 2025", "10:17", "18:42", 4.5, 150, 72, FlightStatus.COMPLETED),
    FlightRecord("Vuelo #024", "19 sep. 2025", "16:03", "09:21", 1.9,  90, 58, FlightStatus.INTERRUPTED),
    FlightRecord("Vuelo #023", "17 sep. 2025", "11:32", "21:15", 6.7, 180, 81, FlightStatus.COMPLETED),
    FlightRecord("Vuelo #022", "15 sep. 2025", "14:09", "14:52", 3.2, 130, 69, FlightStatus.COMPLETED),
)

@Composable
fun FlightHistoryScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        GcsTopBar(title = "Historial de vuelos", onBack = onBack)

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            items(sampleFlights) { flight -> FlightCard(flight) }
        }
    }
}

@Composable
private fun FlightCard(flight: FlightRecord) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(14.dp),
        ) {
            // Miniatura estilizada de la ruta del vuelo
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color2.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Terrain,
                    contentDescription = null,
                    tint = Color1,
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(flight.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    StatusChip(flight.status)
                }
                Text(
                    "${flight.date} · ${flight.time}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FlightMeta(Icons.Filled.Schedule, flight.duration)
                    FlightMeta(Icons.Filled.Straighten, "${flight.distanceKm} km")
                    FlightMeta(Icons.Filled.Terrain, "${flight.maxAltitudeM} m")
                    FlightMeta(Icons.Filled.BatteryChargingFull, "${flight.batteryPercent} %")
                }
            }
        }
    }
}

@Composable
private fun FlightMeta(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(3.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun StatusChip(status: FlightStatus) {
    Surface(color = status.color.copy(alpha = 0.15f), shape = RoundedCornerShape(50)) {
        Text(
            status.label,
            style = MaterialTheme.typography.labelSmall,
            color = status.color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}
