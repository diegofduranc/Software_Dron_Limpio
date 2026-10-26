package com.drinix.gcs.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.drinix.gcs.data.ConnectionValidator
import com.drinix.gcs.data.SettingsRepository
import com.drinix.gcs.theme.TextSecondary
import com.drinix.gcs.ui.DroneViewModel
import com.drinix.gcs.ui.auth.ScanDevicesDialog
import com.drinix.gcs.ui.common.GcsTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: DroneViewModel,
    onBack: () -> Unit,
) {
    val username by viewModel.usernameFlow.collectAsState(initial = null)
    val expiresAt by viewModel.authExpiresAtFlow.collectAsState(initial = null)
    val keepScreenOn by viewModel.keepScreenOnFlow.collectAsState(initial = false)
    val environment by viewModel.environmentFlow.collectAsState(initial = SettingsRepository.DEFAULT_ENVIRONMENT)
    val darkMode by viewModel.darkModeFlow.collectAsState(initial = SettingsRepository.DEFAULT_DARK_MODE)
    val joystickDeadzone by viewModel.joystickDeadzoneFlow.collectAsState(initial = 0.1f)
    val joystickSensitivity by viewModel.joystickSensitivityFlow.collectAsState(initial = 1.0f)
    val droneName by viewModel.droneNameFlow.collectAsState(initial = SettingsRepository.DEFAULT_DRONE_NAME)
    val droneModel by viewModel.droneModelFlow.collectAsState(initial = SettingsRepository.DEFAULT_DRONE_MODEL)
    val droneImageUrl by viewModel.droneImageUrlFlow.collectAsState(initial = null)
    val baseUrl by viewModel.baseUrlFlow.collectAsState(initial = SettingsRepository.DEFAULT_BASE_URL)
    var environmentExpanded by remember { mutableStateOf(false) }
    var editDrone by remember { mutableStateOf(false) }
    var editServer by remember { mutableStateOf(false) }
    var serverUrl by remember(baseUrl) { mutableStateOf(baseUrl) }
    var serverUrlError by remember { mutableStateOf(false) }
    var showScanDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        GcsTopBar(title = "Ajustes", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Sesión
            Text("Sesión", style = MaterialTheme.typography.titleSmall)
            Text(
                "Usuario: ${username ?: "—"}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Válido hasta: ${formatExpiry(expiresAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            OutlinedButton(
                onClick = { viewModel.logout() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("Cerrar sesión") }

            Divider(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))

            // Servidor (URL base del backend)
            Text("Servidor", style = MaterialTheme.typography.titleSmall)
            Text(
                "URL del backend FastAPI (ej: http://192.168.1.100:8000 o https://Raberry-innovacion.local:8000)",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it; serverUrlError = false },
                    singleLine = true,
                    label = { Text("URL del servidor") },
                    supportingText = { if (serverUrlError) Text("URL inválida (debe empezar por http:// o https://)", color = MaterialTheme.colorScheme.error) },
                    isError = serverUrlError,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = { showScanDialog = true },
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text("Escanear red")
                }
            }
            Button(
                onClick = {
                    val u = serverUrl.trim()
                    serverUrlError = !ConnectionValidator.isValidUrl(u)
                    if (!serverUrlError) {
                        viewModel.setBaseUrl(u)
                        editServer = false
                    }
                },
                enabled = !serverUrlError,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("Guardar servidor") }

            // Diálogo de escaneo de red
            if (showScanDialog) {
                ScanDevicesDialog(
                    onDeviceSelected = { baseUrl ->
                        showScanDialog = false
                        serverUrl = baseUrl
                        viewModel.setBaseUrl(baseUrl)
                    },
                    onDismiss = { showScanDialog = false },
                    viewModel = viewModel,
                )
            }

            Divider(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))

            // Pantalla
            Text("Pantalla", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Switch(
                    checked = keepScreenOn,
                    onCheckedChange = { viewModel.setKeepScreenOn(it) },
                )
                Column {
                    Text("Mantener encendida", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Evita que el display se apague durante el vuelo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Divider(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))

            // Apariencia
            Text("Apariencia", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Switch(
                    checked = darkMode,
                    onCheckedChange = { viewModel.setDarkMode(it) },
                )
                Column {
                    Text("Tema oscuro", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Cambia entre modo claro y oscuro",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Divider(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))

            // Sensibilidad de joysticks
            Text("Controles de vuelo", style = MaterialTheme.typography.titleSmall)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Zona muerta", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Text("${(joystickDeadzone * 100).roundToInt()}%", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Slider(
                    value = joystickDeadzone,
                    onValueChange = { viewModel.setJoystickDeadzone(it) },
                    valueRange = 0f..0.5f,
                    steps = 11,
                )
                Text(
                    "Ignora movimientos pequeños del joystick para evitar deriva.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Sensibilidad", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Text("${(joystickSensitivity * 100).roundToInt()}%", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Slider(
                    value = joystickSensitivity,
                    onValueChange = { viewModel.setJoystickSensitivity(it) },
                    valueRange = 0.5f..2.0f,
                    steps = 15,
                )
                Text(
                    "Ajusta qué tan rápido responde el dron al mover el joystick.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Divider(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))

            // Preparación de vuelo
            Text("Preparación de vuelo", style = MaterialTheme.typography.titleSmall)
            Text(
                "Selecciona si el dron está en casa/interior o afuera.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            ExposedDropdownMenuBox(
                expanded = environmentExpanded,
                onExpandedChange = { environmentExpanded = it },
            ) {
                androidx.compose.material3.OutlinedTextField(
                    value = if (environment == "indoor") "Hogar / interior" else "Afuera / exterior",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Entorno") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(environmentExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                DropdownMenu(
                    expanded = environmentExpanded,
                    onDismissRequest = { environmentExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Hogar / interior") },
                        onClick = {
                            environmentExpanded = false
                            viewModel.setEnvironment("indoor")
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Afuera / exterior") },
                        onClick = {
                            environmentExpanded = false
                            viewModel.setEnvironment("outdoor")
                        },
                    )
                }
            }
        }
    }
}

private fun formatExpiry(epochSeconds: Long?): String {
    if (epochSeconds == null || epochSeconds <= 0L) return "—"
    val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return fmt.format(Date(epochSeconds * 1000L))
}
