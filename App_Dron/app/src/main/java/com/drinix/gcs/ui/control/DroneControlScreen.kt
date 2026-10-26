package com.drinix.gcs.ui.control

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.drinix.gcs.data.model.ConnectionState
import com.drinix.gcs.theme.AmberWarning
import com.drinix.gcs.theme.ErrorRed
import com.drinix.gcs.theme.GreenOk
import com.drinix.gcs.ui.DroneViewModel
import com.drinix.gcs.ui.common.AppIcons
import com.drinix.gcs.ui.common.ConnectionChip
import com.drinix.gcs.ui.common.GcsTopBar
import com.drinix.gcs.ui.common.vibrate
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val FLIGHT_MODES = listOf(
    "STABILIZE", "ALT_HOLD", "LOITER", "GUIDED", "AUTO", "RTL", "LAND", "POSHOLD",
)

private fun flightModeLabel(mode: String): String = when (mode) {
    "STABILIZE" -> "Estabilizar"
    "ALT_HOLD" -> "Mantener altura"
    "LOITER" -> "Esperar en punto"
    "GUIDED" -> "Guiado"
    "AUTO" -> "Misión automática"
    "RTL" -> "Volver a casa"
    "LAND" -> "Aterrizar"
    "POSHOLD" -> "Mantener posición"
    else -> mode
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DroneControlScreen(viewModel: DroneViewModel, onBack: () -> Unit) {
    val telemetry by viewModel.telemetry.collectAsState()
    val connState by viewModel.connectionState.collectAsState()
    val joystickDeadzone by viewModel.joystickDeadzoneFlow.collectAsState(initial = 0.1f)
    val joystickSensitivity by viewModel.joystickSensitivityFlow.collectAsState(initial = 1.0f)

    val context = LocalContext.current

    var confirmArm by remember { mutableStateOf(false) }
    var confirmDisarm by remember { mutableStateOf(false) }
    var confirmTakeoff by remember { mutableStateOf(false) }
    var confirmLand by remember { mutableStateOf(false) }
    var confirmRtl by remember { mutableStateOf(false) }
    var confirmEmergency by remember { mutableStateOf(false) }
    var takeoffAlt by remember { mutableFloatStateOf(10f) }

    var throttlePwm by remember { mutableIntStateOf(1000) }
    var yawPwm by remember { mutableIntStateOf(1500) }
    var rollPwm by remember { mutableIntStateOf(1500) }
    var pitchPwm by remember { mutableIntStateOf(1500) }
    var rcActive by remember { mutableStateOf(false) }

    LaunchedEffect(rcActive) {
        while (rcActive && isActive) {
            viewModel.sendRc(rollPwm, pitchPwm, throttlePwm, yawPwm)
            delay(200)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GcsTopBar(title = "Control del dron", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── Barra de estado superior ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        "Modo: ${flightModeLabel(telemetry?.mode ?: "—")}",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                    )
                    Text(
                        "ID: ${viewModel.host}:${viewModel.port}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val t = telemetry
                    ConnectionChip(connState)
                    Text(
                        if (t?.armed == true) "ARMADO" else "DESARMADO",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (t?.armed == true) GreenOk else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Text(
                        "🔋 ${t?.batteryRemaining ?: 0}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (t?.batteryRemaining != null && t.batteryRemaining < 20) ErrorRed else GreenOk,
                        maxLines = 1,
                    )
                    Text(
                        "📡 ${t?.satellites ?: 0} sats",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (t?.satellites != null && t.satellites >= 6) GreenOk else AmberWarning,
                        maxLines = 1,
                    )
                }
            }

            // ── Modo de vuelo ──
            var expanded by remember { mutableStateOf(false) }
            var selectedMode by remember { mutableStateOf(FLIGHT_MODES[0]) }
            LaunchedEffect(telemetry?.mode) {
                telemetry?.mode?.takeIf { it in FLIGHT_MODES }?.let { selectedMode = it }
            }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = flightModeLabel(selectedMode),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Modo de vuelo") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                androidx.compose.material3.DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    FLIGHT_MODES.forEach { mode ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(flightModeLabel(mode)) },
                            onClick = {
                                selectedMode = mode
                                expanded = false
                                viewModel.setMode(mode)
                            },
                        )
                    }
                }
            }

            // ── Atajos rápidos de modo ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                QuickModeButton("ESTABILIZAR", "STABILIZE", selectedMode == "STABILIZE", GreenOk) { viewModel.setMode("STABILIZE") }
                QuickModeButton("ALT_HOLD", "ALT_HOLD", selectedMode == "ALT_HOLD", GreenOk) { viewModel.setMode("ALT_HOLD") }
                QuickModeButton("LOITER", "LOITER", selectedMode == "LOITER", GreenOk) { viewModel.setMode("LOITER") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                QuickModeButton("GUIDED", "GUIDED", selectedMode == "GUIDED", GreenOk) { viewModel.setMode("GUIDED") }
                QuickModeButton("RTL", "RTL", selectedMode == "RTL", AmberWarning) { viewModel.setMode("RTL") }
                QuickModeButton("LAND", "LAND", selectedMode == "LAND", AmberWarning) { viewModel.setMode("LAND") }
            }

            Spacer(Modifier.height(2.dp))

            // ── Botones de comandos principales ──
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CommandButton("ARMAR", GreenOk, Modifier.fillMaxWidth().height(56.dp)) { confirmArm = true }
                CommandButton("DESARMAR", ErrorRed, Modifier.fillMaxWidth().height(56.dp)) { confirmDisarm = true }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CommandButton("DESPEGAR", GreenOk, Modifier.fillMaxWidth().height(56.dp)) { confirmTakeoff = true }
                CommandButton("ATERRIZAR", AmberWarning, Modifier.fillMaxWidth().height(56.dp)) { confirmLand = true }
            }
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                CommandButton("RTL", AmberWarning, Modifier.height(56.dp)) { confirmRtl = true }
            }

            // ── Botón de emergencia (PANIC) ──
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                EmergencyButton {
                    confirmEmergency = true
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Joysticks ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("THROTTLE / YAW", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(4.dp))
                    VirtualJoystick(
                        label = "T / Y",
                        throttleMode = true,
                        deadzone = joystickDeadzone,
                        sensitivity = joystickSensitivity,
                        onMove = { x, y ->
                            rcActive = true
                            yawPwm = normalizedToPwm(x)
                            throttlePwm = throttleToPwm((y + 1f) / 2f)
                        },
                        onRelease = {
                            yawPwm = 1500
                            viewModel.sendRc(rollPwm, pitchPwm, throttlePwm, yawPwm)
                            rcActive = false
                        },
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PITCH / ROLL", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(4.dp))
                    VirtualJoystick(
                        label = "P / R",
                        deadzone = joystickDeadzone,
                        sensitivity = joystickSensitivity,
                        onMove = { x, y ->
                            rcActive = true
                            rollPwm = normalizedToPwm(x)
                            pitchPwm = normalizedToPwm(y)
                        },
                        onRelease = {
                            rollPwm = 1500
                            pitchPwm = 1500
                            viewModel.sendRc(rollPwm, pitchPwm, throttlePwm, yawPwm)
                            rcActive = false
                        },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }

    // ── Diálogos de confirmación ──
    if (confirmArm) ConfirmDialog("Armar motores", "¿Confirmas ARMAR el dron?",
        onConfirm = { viewModel.arm(true); vibrate(context, 100) }) { confirmArm = false }
    if (confirmDisarm) ConfirmDialog("Desarmar motores", "¿Confirmas DESARMAR el dron?",
        onConfirm = { viewModel.arm(false); vibrate(context, 100) }) { confirmDisarm = false }
    if (confirmLand) ConfirmDialog("Aterrizar", "¿Iniciar secuencia de ATERRIZAJE?",
        onConfirm = { viewModel.land() }) { confirmLand = false }
    if (confirmRtl) ConfirmDialog("Return to Launch", "¿Volver al punto de origen (RTL)?",
        onConfirm = { viewModel.rtl() }) { confirmRtl = false }
    if (confirmEmergency) ConfirmDialog("EMERGENCIA", "¿Desarmar MOTORES INMEDIATAMENTE?\nEl dron caerá si está en vuelo.",
        onConfirm = {
            viewModel.arm(false)
            confirmEmergency = false
            vibrate(context, 500)
        }) { confirmEmergency = false }

    if (confirmTakeoff) {
        AlertDialog(
            onDismissRequest = { confirmTakeoff = false },
            title = { Text("Despegar") },
            text = {
                Column {
                    Text("Altitud objetivo: ${fmt1(takeoffAlt)} m")
                    androidx.compose.material3.Slider(
                        value = takeoffAlt,
                        onValueChange = { takeoffAlt = it },
                        valueRange = 2f..20f,
                        steps = 17,
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.takeoff(takeoffAlt.toDouble())
                    confirmTakeoff = false
                }) { Text("DESPEGAR") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmTakeoff = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun QuickModeButton(label: String, mode: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) color else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun EmergencyButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(60.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ErrorRed,
            contentColor = Color.White,
        ),
    ) {
        Text("🚨 EMERGENCIA - DESARMAR TODO", style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

@Composable
private fun CommandButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        modifier = modifier,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onConfirm(); onDismiss() }) { Text("Confirmar") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
private fun Divider() {
    Spacer(
        modifier = Modifier.height(1.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.outline),
    )
}

private fun fmt1(v: Float): String = String.format(Locale.US, "%.1f", v)
