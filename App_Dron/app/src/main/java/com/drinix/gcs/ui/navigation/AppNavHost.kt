package com.drinix.gcs.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.drinix.gcs.data.model.CommandResult
import com.drinix.gcs.ui.DroneViewModel
import com.drinix.gcs.ui.control.DroneControlScreen
import com.drinix.gcs.ui.maintenance.MaintenanceScreen
import com.drinix.gcs.ui.map.MapScreen
import com.drinix.gcs.ui.settings.SettingsScreen
import com.drinix.gcs.ui.telemetry.TelemetryScreen

object Routes {
    const val CONTROL = "control"
    const val TELEMETRY = "telemetry"
    const val MAP = "map"
    const val MAINTENANCE = "maintenance"
    const val SETTINGS = "settings"
}

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Routes.CONTROL, "Control", Icons.Filled.AirplanemodeActive),
    TabItem(Routes.TELEMETRY, "Telemetría", Icons.Filled.BatteryStd),
    TabItem(Routes.MAP, "Mapa", Icons.Filled.Map),
    TabItem(Routes.MAINTENANCE, "Mantenimiento", Icons.Filled.Build),
    TabItem(Routes.SETTINGS, "Ajustes", Icons.Filled.Settings),
)

@Composable
fun AppNavHost(viewModel: DroneViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.commandEvents.collect { result ->
            val msg = when (result) {
                is CommandResult.Success -> "✓ ${result.message}"
                is CommandResult.Error -> "✗ ${result.message}"
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.height(64.dp),
                tonalElevation = 0.dp,
            ) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        label = {
                            Text(
                                tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                            )
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.CONTROL,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.CONTROL) {
                DroneControlScreen(viewModel = viewModel, onBack = {})
            }
            composable(Routes.TELEMETRY) {
                TelemetryScreen(viewModel)
            }
            composable(Routes.MAP) {
                MapScreen(
                    viewModel = viewModel,
                    onExpandMap = {},
                )
            }
            composable(Routes.MAINTENANCE) {
                MaintenanceScreen(viewModel = viewModel, onBack = {})
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(viewModel = viewModel, onBack = {})
            }
        }
    }
}
