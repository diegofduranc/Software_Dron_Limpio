package com.drinix.gcs.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.drinix.gcs.data.ConnectionValidator
import com.drinix.gcs.data.SettingsRepository
import com.drinix.gcs.theme.Color1
import com.drinix.gcs.theme.Color2
import com.drinix.gcs.theme.TextSecondary
import com.drinix.gcs.ui.DroneViewModel
import com.drinix.gcs.ui.common.AppIcons

/** Pantalla de carga mientras se comprueba la sesión guardada. */
@Composable
fun AuthLoadingScreen() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

/**
 * Pantalla de login como puerta de entrada. Se muestra cuando no hay sesión
 * válida (sin token, caducado o rechazado por el servidor).
 */
@Composable
fun LoginScreen(viewModel: DroneViewModel) {
    val host by viewModel.hostFlow.collectAsState(initial = SettingsRepository.DEFAULT_HOST)
    val port by viewModel.portFlow.collectAsState(initial = SettingsRepository.DEFAULT_PORT)
    val baseUrl by viewModel.baseUrlFlow.collectAsState(initial = SettingsRepository.DEFAULT_BASE_URL)
    val message by viewModel.authMessage.collectAsState()
    val authenticating by viewModel.isAuthenticating.collectAsState()
    var showScanDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color2.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Drone, contentDescription = null, tint = Color1, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("Drinix GCS", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Inicia sesión en el backend del dron",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(28.dp))

        LoginForm(
            initialBaseUrl = baseUrl,
            isAuthenticating = authenticating,
            message = message,
            onSubmit = { b, u, pw -> viewModel.login(b, u, pw) },
            onGuestLogin = { b -> viewModel.loginAsGuest(b) },
            onScan = {
                showScanDialog = true
            },
        )

        // Diálogo de escaneo de red
        if (showScanDialog) {
            ScanDevicesDialog(
                onDeviceSelected = { baseUrl ->
                    showScanDialog = false
                    viewModel.setBaseUrl(baseUrl)
                },
                onDismiss = { showScanDialog = false },
                viewModel = viewModel,
            )
        }
    }
}

/**
 * Formulario de conexión + credenciales. Reutilizado por la pantalla de login
 * y por Ajustes. La contraseña solo vive en memoria de este composable.
 */
@Composable
fun LoginForm(
    initialBaseUrl: String,
    isAuthenticating: Boolean,
    message: String?,
    buttonLabel: String = "Iniciar sesión",
    onSubmit: (baseUrl: String, username: String, password: String) -> Unit,
    onGuestLogin: ((baseUrl: String) -> Unit)? = null,
    onScan: (() -> Unit)? = null,
) {
    var baseUrl by remember(initialBaseUrl) { mutableStateOf(initialBaseUrl) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var baseUrlError by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it; baseUrlError = false },
                label = { Text("URL del servidor") },
                supportingText = { if (baseUrlError) Text("URL inválida (debe empezar por http:// o https://)", color = MaterialTheme.colorScheme.error) },
                isError = baseUrlError,
                singleLine = true,
                enabled = !isAuthenticating,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = { onScan?.invoke() },
                enabled = !isAuthenticating,
            ) {
                Text("Escanear red")
            }
        }

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Usuario") },
            singleLine = true,
            enabled = !isAuthenticating,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            singleLine = true,
            enabled = !isAuthenticating,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            trailingIcon = {
                TextButton(
                    onClick = { passwordVisible = !passwordVisible },
                ) {
                    Text(if (passwordVisible) "Ocultar" else "Mostrar")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        if (message != null) {
            Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = {
                val b = baseUrl.trim()
                baseUrlError = !ConnectionValidator.isValidUrl(b)
                if (!baseUrlError) {
                    onSubmit(b, username, password)
                }
            },
            enabled = !isAuthenticating,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (isAuthenticating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(10.dp))
                Text("Conectando...")
            } else {
                Text(buttonLabel)
            }
        }

        onGuestLogin?.let {
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val b = baseUrl.trim()
                    baseUrlError = !ConnectionValidator.isValidUrl(b)
                    if (!baseUrlError) {
                        it(b)
                    }
                },
                enabled = !isAuthenticating,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text("Ingresar como invitado")
            }
        }

        Text(
            "La contraseña no se guarda en el dispositivo.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
