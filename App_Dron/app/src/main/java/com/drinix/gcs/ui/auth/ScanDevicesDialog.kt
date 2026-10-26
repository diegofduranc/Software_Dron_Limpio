package com.drinix.gcs.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.drinix.gcs.data.NetworkScanner
import com.drinix.gcs.ui.DroneViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Composable
fun ScanDevicesDialog(
    onDeviceSelected: (baseUrl: String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: DroneViewModel,
) {
    val context = LocalContext.current
    var scanning by remember { mutableStateOf<Boolean>(false) }
    var foundDevices by remember { mutableStateOf<List<String>>(emptyList()) }
    var scanError by remember { mutableStateOf<String?>(null) }
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.IO) }

    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Buscar servidor en la red",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Button(
                        onClick = {
                            scanning = true
                            scanError = null
                            foundDevices = emptyList()
                            scope.launch {
                                val prefix = NetworkScanner.getLocalIpPrefix(context)
                                if (prefix == null) {
                                    scanning = false
                                    scanError = "No se pudo obtener la IP local del móvil. Asegúrate de estar conectado al WiFi."
                                    return@launch
                                }
                                val found = NetworkScanner.scanForBackends(prefix)
                                scanning = false
                                if (found.isEmpty()) {
                                    scanError = "No se encontró ningún servidor en la red. Puedes escribir la IP manualmente."
                                } else {
                                    foundDevices = found
                                }
                            }
                        },
                        enabled = !scanning,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (scanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text("Escanear red")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (scanning) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                        Text(
                            "Escaneando red...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                } else if (scanError != null) {
                    val errorMsg: String = scanError ?: ""
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "No se encontraron servidores",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Text(
                            errorMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, start = 16.dp, end = 16.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                } else if (foundDevices.isNotEmpty()) {
                    Text(
                        "Servidores encontrados:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                    ) {
                        items(foundDevices) { ip ->
                            val baseUrl = "http://$ip:8000"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onDeviceSelected(baseUrl) },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    ip,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    ":8000",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            androidx.compose.material3.Divider(
                                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp),
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Presiona \"Escanear red\" para buscar servidores disponibles.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Cancelar")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
