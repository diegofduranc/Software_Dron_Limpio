package com.drinix.gcs.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Speed
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import com.drinix.gcs.data.model.CommandResult
import com.drinix.gcs.ui.DroneViewModel
import com.drinix.gcs.ui.alerts.AlertsScreen
import com.drinix.gcs.ui.control.DroneControlScreen
import com.drinix.gcs.ui.history.FlightHistoryScreen
import com.drinix.gcs.ui.home.HomeScreen
import com.drinix.gcs.ui.map.FullMapScreen
import com.drinix.gcs.ui.map.MapScreen
import com.drinix.gcs.ui.mission.WaypointScreen
import com.drinix.gcs.ui.more.MoreScreen
import com.drinix.gcs.ui.settings.SettingsScreen
import com.drinix.gcs.ui.status.DroneStatusScreen
import com.drinix.gcs.ui.telemetry.TelemetryScreen

object Routes {
    const val HOME = "home"
    const val MAP = "map"
    const val TELEMETRY = "telemetry"
    const val MORE = "more"
    // Subpantallas
    const val MAP_FULL = "map_full"
    const val CONTROL = "control"
    const val MISSION = "mission"
    const val STATUS = "status"
    const val HISTORY = "history"
    const val ALERTS = "alerts"
    const val SETTINGS = "settings"
}

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Routes.HOME, "Inicio", Icons.Filled.Home),
    TabItem(Routes.MAP, "Mapa", Icons.Filled.Map),
    TabItem(Routes.TELEMETRY, "Telemetría", Icons.Filled.Speed),
    TabItem(Routes.MORE, "Más", Icons.Filled.MoreHoriz),
)

/** Subpantallas que pertenecen a la pestaña "Más" (para resaltarla en la barra). */
private val moreChildren = setOf(Routes.CONTROL, Routes.MISSION, Routes.STATUS, Routes.HISTORY, Routes.ALERTS, Routes.SETTINGS)

@Composable
fun AppNavHost(viewModel: DroneViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    // Resultados de comandos como snackbars globales
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
    val selectedTab = when {
        currentRoute in moreChildren -> Routes.MORE
        currentRoute == Routes.MAP_FULL -> Routes.MAP
        else -> currentRoute
    }

    Scaffold(
        // Las pantallas (TopAppBar) ya manejan sus propios insets; aquí solo el bottomBar
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.height(64.dp),
                tonalElevation = 0.dp,
            ) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
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
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    viewModel = viewModel,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                    onOpenAlerts = { navController.navigate(Routes.ALERTS) { launchSingleTop = true } },
                )
            }
            composable(Routes.MAP) {
                MapScreen(
                    viewModel = viewModel,
                    onExpandMap = { navController.navigate(Routes.MAP_FULL) { launchSingleTop = true } },
                )
            }
            composable(Routes.MAP_FULL) {
                FullMapScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.TELEMETRY) { TelemetryScreen(viewModel) }
            composable(Routes.MORE) {
                MoreScreen(
                    onNavigate = { route -> navController.navigate(route) { launchSingleTop = true } },
                )
            }
            composable(Routes.CONTROL) {
                DroneControlScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable(Routes.MISSION) {
                WaypointScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable(Routes.STATUS) {
                DroneStatusScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable(Routes.HISTORY) {
                FlightHistoryScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ALERTS) {
                AlertsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
        }
    }
}
