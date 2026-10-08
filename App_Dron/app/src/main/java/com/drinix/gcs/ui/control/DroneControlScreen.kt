package com.drinix.gcs.ui.control

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.drinix.gcs.theme.AmberWarning
import com.drinix.gcs.theme.ErrorRed
import com.drinix.gcs.theme.GreenOk
import com.drinix.gcs.ui.DroneViewModel
import com.drinix.gcs.ui.common.GcsTopBar
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val FLIGHT_MODES = listOf(
    "STABILIZE", "ALT_HOLD", "LOITER", "GUIDED", "AUTO", "RTL", "LAND", "POSHOLD",
)

/** Traducción de modos de vuelo a español con descripción breve. */
private fun flightModeLabel(mode: String): String = when (mode) {
    "STABILIZE" -> "Estabilizar"
    "ALT_HOLD" -> "Mantener altura"
    "LOITER" -> "Esperar en el punto"
    "GUIDED" -> "Guiado (ir a puntos)"
    "AUTO" -> "Automático (misión)"
    "RTL" -> "Volver a casa"
    "LAND" -> "Aterrizar"
    "POSHOLD" -> "Mantener posición"
    else -> mode
}

private fun flightModeDescription(mode: String): String = when (mode) {
    "STABILIZE" -> "Control manual. El dron se nivela solo, pero tú controlas altura y posición."
    "ALT_HOLD" -> "Control manual de posición; el dron mantiene la altura automáticamente."
    "LOITER" -> "El dron se queda quieto en su punto actual (GPS). Suéltalo y espera."
    "GUIDED" -> "El dron obedece órdenes de la app, p. ej. volar a un punto tocado en el mapa."
    "AUTO" -> "Ejecuta la misión de waypoints cargada, sin intervención manual."
    "RTL" -> "El dron regresa solo al punto de despegue y aterriza."
    "LAND" -> "Inicia el aterrizaje en el punto donde está."
    "POSHOLD" -> "Mantiene la posición; mueves el dron suavemente con los joysticks."
    else -> ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DroneControlScreen(viewModel: DroneViewModel, onBack: () -> Unit) {
    // Confirmaciones
    var confirmArm by remember { mutableStateOf(false) }
    var confirmDisarm by remember { mutableStateOf(false) }
    var confirmTakeoff by remember { mutableStateOf(false) }
    var confirmLand by remember { mutableStateOf(false) }
    var confirmRtl by remember { mutableStateOf(false) }
    var takeoffAlt by remember { mutableFloatStateOf(10f) }

    // Joysticks: valores PWM actuales
    var throttlePwm by remember { mutableIntStateOf(1000) }
    var yawPwm by remember { mutableIntStateOf(1500) }
    var rollPwm by remember { mutableIntStateOf(1500) }
    var pitchPwm by remember { mutableIntStateOf(1500) }
    var rcActive by remember { mutableStateOf(false) }

    // Envío de RC a 5 Hz mientras haya toque activo
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Selector de modo
            var expanded by remember { mutableStateOf(false) }
            var selectedMode by remember { mutableStateOf(FLIGHT_MODES[2]) }
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
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    FLIGHT_MODES.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(flightModeLabel(mode))
                                    Text(
                                        flightModeDescription(mode),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            onClick = {
                                selectedMode = mode
                                expanded = false
                                viewModel.setMode(mode)
                            },
                        )
                    }
                }
            }
            // Explicación del modo seleccionado
            Text(
                flightModeDescription(selectedMode),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CommandButton("ARMAR", GreenOk, Modifier.weight(1f)) { confirmArm = true }
                CommandButton("DESARMAR", ErrorRed, Modifier.weight(1f)) { confirmDisarm = true }
            }
            CommandButton("DESPEGAR", GreenOk, Modifier.fillMaxWidth()) { confirmTakeoff = true }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CommandButton("ATERRIZAR", AmberWarning, Modifier.weight(1f)) { confirmLand = true }
                CommandButton("RTL", AmberWarning, Modifier.weight(1f)) { confirmRtl = true }
            }

            Text(
                "RC: R=$rollPwm P=$pitchPwm T=$throttlePwm Y=$yawPwm",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            // Joysticks lado a lado
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("THROTTLE / YAW", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(6.dp))
                    VirtualJoystick(
                        label = "T / Y",
                        throttleMode = true,
                        onMove = { x, y ->
                            rcActive = true
                            yawPwm = normalizedToPwm(x)
                            throttlePwm = throttleToPwm((y + 1f) / 2f)
                        },
                        onRelease = {
                            yawPwm = 1500 // yaw recentra; throttle se mantiene
                            viewModel.sendRc(rollPwm, pitchPwm, throttlePwm, yawPwm)
                            rcActive = false
                        },
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PITCH / ROLL", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(6.dp))
                    VirtualJoystick(
                        label = "P / R",
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
            Spacer(Modifier.height(24.dp))
        }
    }

    // Diálogos de confirmación
    if (confirmArm) ConfirmDialog("Armar motores", "¿Confirmas ARMAR el dron?",
        onConfirm = { viewModel.arm(true) }) { confirmArm = false }
    if (confirmDisarm) ConfirmDialog("Desarmar motores", "¿Confirmas DESARMAR el dron?",
        onConfirm = { viewModel.arm(false) }) { confirmDisarm = false }
    if (confirmLand) ConfirmDialog("Aterrizar", "¿Iniciar secuencia de ATERRIZAJE?",
        onConfirm = { viewModel.land() }) { confirmLand = false }
    if (confirmRtl) ConfirmDialog("Return to Launch", "¿Volver al punto de origen (RTL)?",
        onConfirm = { viewModel.rtl() }) { confirmRtl = false }

    if (confirmTakeoff) {
        AlertDialog(
            onDismissRequest = { confirmTakeoff = false },
            title = { Text("Despegar") },
            text = {
                Column {
                    Text("Altitud objetivo: ${fmt1(takeoffAlt)} m")
                    Slider(
                        value = takeoffAlt,
                        onValueChange = { takeoffAlt = it },
                        valueRange = 2f..20f,
                        steps = 17,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.takeoff(takeoffAlt.toDouble())
                    confirmTakeoff = false
                }) { Text("DESPEGAR") }
            },
            dismissButton = {
                TextButton(onClick = { confirmTakeoff = false }) { Text("Cancelar") }
            },
        )
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
        modifier = modifier.height(56.dp),
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
            TextButton(onClick = { onConfirm(); onDismiss() }) { Text("Confirmar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

private fun fmt1(v: Float): String = String.format(Locale.US, "%.1f", v)
