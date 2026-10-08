package com.drinix.gcs.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.drinix.gcs.data.SettingsRepository
import com.drinix.gcs.ui.DroneViewModel
import com.drinix.gcs.ui.common.GcsTopBar

@Composable
fun SettingsScreen(
    viewModel: DroneViewModel,
    onBack: () -> Unit,
) {
    val savedHost by viewModel.hostFlow.collectAsState(initial = SettingsRepository.DEFAULT_HOST)
    val savedPort by viewModel.portFlow.collectAsState(initial = SettingsRepository.DEFAULT_PORT)
    val keepScreenOn by viewModel.keepScreenOnFlow.collectAsState(initial = false)

    // Estados de edición (null = sin tocar, usa el valor guardado)
    var host by remember { mutableStateOf<String?>(null) }
    var port by remember { mutableStateOf<String?>(null) }
    var hostError by remember { mutableStateOf(false) }
    var portError by remember { mutableStateOf(false) }

    val hostValue = host ?: savedHost
    val portValue = port ?: savedPort.toString()

    Column(modifier = Modifier.fillMaxSize()) {
        GcsTopBar(title = "Ajustes", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Conexión al backend", style = MaterialTheme.typography.titleSmall)

            OutlinedTextField(
                value = hostValue,
                onValueChange = { host = it; hostError = false },
                label = { Text("Host IP") },
                supportingText = {
                    if (hostError) Text("IP inválida", color = MaterialTheme.colorScheme.error)
                },
                isError = hostError,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = portValue,
                onValueChange = { port = it.filter { c -> c.isDigit() }.take(5); portError = false },
                label = { Text("Puerto") },
                supportingText = {
                    if (portError) Text("Puerto inválido (1-65535)", color = MaterialTheme.colorScheme.error)
                },
                isError = portError,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    val ipOk = isValidHost(hostValue)
                    val p = portValue.toIntOrNull()
                    val portOk = p != null && p in 1..65535
                    hostError = !ipOk
                    portError = !portOk
                    if (ipOk && p != null && portOk) {
                        viewModel.saveConnectionSettings(hostValue, p)
                        host = null
                        port = null
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Guardar y reconectar") }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mantener pantalla encendida", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Evita que el display se apague durante el vuelo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = keepScreenOn,
                    onCheckedChange = { viewModel.setKeepScreenOn(it) },
                )
            }
        }
    }
}

private fun isValidHost(host: String): Boolean {
    if (host.isBlank()) return false
    // Acepta hostname simple o IPv4
    val ipv4 = Regex("^((25[0-5]|2[0-4]\\d|1?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1?\\d?\\d)$")
    val hostname = Regex("^[a-zA-Z0-9]([a-zA-Z0-9\\-.]*[a-zA-Z0-9])?$")
    return ipv4.matches(host.trim()) || hostname.matches(host.trim())
}
