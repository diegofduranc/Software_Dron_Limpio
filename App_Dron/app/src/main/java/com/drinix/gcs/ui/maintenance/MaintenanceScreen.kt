package com.drinix.gcs.ui.maintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.drinix.gcs.data.model.CalibrationStatus
import com.drinix.gcs.ui.DroneViewModel
import com.drinix.gcs.ui.common.GcsTopBar

/** Pantalla de mantenimiento: calibración de sensores y prueba de motores. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceScreen(viewModel: DroneViewModel, onBack: () -> Unit) {
    var confirmFullCalibration by remember { mutableStateOf(false) }
    val calibration by viewModel.calibrationStatus.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        GcsTopBar(title = "Mantenimiento", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Calibración de sensores", style = MaterialTheme.typography.titleMedium)
            Text(
                "Calibra brújula, posición cero y verifica señal GPS antes de volar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { viewModel.calibrateCompass() },
                    modifier = Modifier.weight(1f).height(48.dp),
                ) { Text("Brújula") }
                OutlinedButton(
                    onClick = { viewModel.calibrateHome() },
                    modifier = Modifier.weight(1f).height(48.dp),
                ) { Text("Cero aquí") }
                OutlinedButton(
                    onClick = { viewModel.verifyGps() },
                    modifier = Modifier.weight(1f).height(48.dp),
                ) { Text("Verificar GPS") }
            }
            OutlinedButton(
                onClick = { confirmFullCalibration = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("Calibrar todo el sistema") }

            Divider(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp))

            Text("Prueba individual de motores", style = MaterialTheme.typography.titleMedium)
            Text(
                "Ajusta la potencia de prueba. El dron debe estar desarmado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MotorTestControl(1, viewModel) { viewModel.testMotor(1, it) }
            MotorTestControl(2, viewModel) { viewModel.testMotor(2, it) }
            MotorTestControl(3, viewModel) { viewModel.testMotor(3, it) }
            MotorTestControl(4, viewModel) { viewModel.testMotor(4, it) }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (confirmFullCalibration) {
        AlertDialog(
            onDismissRequest = { confirmFullCalibration = false },
            title = { Text("Calibrar todo el sistema") },
            text = {
                Text(
                    "El dron debe estar desarmado y quieto. Se iniciará la calibración de giroscopio, "
                        + "brújula y acelerómetro.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.calibrateSystem()
                    confirmFullCalibration = false
                }) { Text("INICIAR") }
            },
            dismissButton = {
                TextButton(onClick = { confirmFullCalibration = false }) { Text("Cancelar") }
            },
        )
    }

    // ── Diálogos de estado de calibración (polling) ──
    val calib = calibration
    when (calib?.status) {
        "running" -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Calibrando sensores") },
                text = {
                    Column {
                        Text(calib.message)
                        Spacer(modifier = Modifier.height(12.dp))
                        CircularProgressIndicator(Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {},
                dismissButton = {},
            )
        }
        "success", "error" -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearCalibrationStatus() },
                title = { Text(if (calib.status == "success") "Éxito" else "Error") },
                text = { Text(calib.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearCalibrationStatus() }) { Text("Aceptar") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.clearCalibrationStatus() }) { Text("Cerrar") }
                },
            )
        }
        else -> Unit
    }
}

@Composable
private fun MotorTestControl(
    motor: Int,
    viewModel: DroneViewModel,
    onTest: (Float) -> Unit,
) {
    var power by remember { mutableStateOf(10f) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Motor M$motor: ${power.toInt()}%", modifier = Modifier.weight(1f))
            androidx.compose.material3.TextButton(onClick = { onTest(power) }) { Text("PROBAR") }
        }
        Slider(
            value = power,
            onValueChange = { power = it },
            valueRange = 0f..100f,
            steps = 19,
        )
    }
}
