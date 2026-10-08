package com.drinix.gcs.ui.alerts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.drinix.gcs.data.model.AlertSeverity
import com.drinix.gcs.data.model.FlightAlert
import com.drinix.gcs.theme.AmberWarning
import com.drinix.gcs.theme.BlueAccent
import com.drinix.gcs.theme.ErrorRed
import com.drinix.gcs.theme.GreenOk
import com.drinix.gcs.theme.TextSecondary
import com.drinix.gcs.ui.DroneViewModel
import com.drinix.gcs.ui.common.GcsTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Lista de alertas de vuelo generadas desde la telemetría (mockup "Alertas"). */
@Composable
fun AlertsScreen(viewModel: DroneViewModel, onBack: () -> Unit) {
    val alerts by viewModel.alerts.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        GcsTopBar(title = "Alertas", onBack = onBack)

        if (alerts.isEmpty()) {
            Text(
                "Sin alertas. Todo en orden.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(24.dp),
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            items(alerts) { alert -> AlertRow(alert) }
        }
    }
}

@Composable
private fun AlertRow(alert: FlightAlert) {
    val (icon, color) = when (alert.severity) {
        AlertSeverity.CRITICAL -> Icons.Filled.Error to ErrorRed
        AlertSeverity.WARNING -> Icons.Filled.Warning to AmberWarning
        AlertSeverity.INFO -> Icons.Filled.Info to BlueAccent
        AlertSeverity.SUCCESS -> Icons.Filled.CheckCircle to GreenOk
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            Surface(color = color.copy(alpha = 0.18f), shape = CircleShape) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.padding(10.dp).size(24.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Text(
                        alert.title,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        formatTime(alert.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
                Text(
                    alert.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }
}

private fun formatTime(ts: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
