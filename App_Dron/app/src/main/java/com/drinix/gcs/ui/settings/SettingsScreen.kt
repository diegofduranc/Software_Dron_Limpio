package com.drinix.gcs.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.drinix.gcs.data.SettingsRepository
import com.drinix.gcs.theme.TextSecondary
import com.drinix.gcs.ui.DroneViewModel
import com.drinix.gcs.ui.auth.LoginForm
import com.drinix.gcs.ui.common.GcsTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: DroneViewModel,
    onBack: () -> Unit,
) {
    val savedHost by viewModel.hostFlow.collectAsState(initial = SettingsRepository.DEFAULT_HOST)
    val savedPort by viewModel.portFlow.collectAsState(initial = SettingsRepository.DEFAULT_PORT)
    val username by viewModel.usernameFlow.collectAsState(initial = null)
    val expiresAt by viewModel.authExpiresAtFlow.collectAsState(initial = null)
    val authMessage by viewModel.authMessage.collectAsState()
    val authenticating by viewModel.isAuthenticating.collectAsState()
    val keepScreenOn by viewModel.keepScreenOnFlow.collectAsState(initial = false)

    Column(modifier = Modifier.fillMaxSize()) {
        GcsTopBar(title = "Ajustes", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Sesión", style = MaterialTheme.typography.titleSmall)
            Text(
                "Usuario: ${username ?: "—"}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Sesión válida hasta: ${formatExpiry(expiresAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            OutlinedButton(
                onClick = { viewModel.logout() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("Cerrar sesión") }

            Text("Conexión al backend", style = MaterialTheme.typography.titleSmall)
            LoginForm(
                initialHost = savedHost,
                initialPort = savedPort.toString(),
                isAuthenticating = authenticating,
                message = authMessage,
                buttonLabel = "Iniciar sesión",
                onSubmit = { h, p, u, pw -> viewModel.login(h, p, u, pw) },
            )

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

private fun formatExpiry(epochSeconds: Long?): String {
    if (epochSeconds == null || epochSeconds <= 0L) return "—"
    val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return fmt.format(Date(epochSeconds * 1000L))
}
