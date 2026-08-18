package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*

sealed class NavDestination(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : NavDestination("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Calculators : NavDestination("calculators", "Calculators", Icons.Default.Calculate)
    object CAD : NavDestination("cad", "2D CAD", Icons.Default.Architecture)
    object BOQ : NavDestination("boq", "BOQ & Cost", Icons.Default.RequestQuote)
    object AIAssistant : NavDestination("ai_assistant", "CiviAI", Icons.Default.SmartToy)
    object Report : NavDestination("report", "Dossier", Icons.Default.Description)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(
    mainViewModel: MainViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showProjectsDialog by remember { mutableStateOf(false) }

    val destinations = listOf(
        NavDestination.Dashboard,
        NavDestination.Calculators,
        NavDestination.CAD,
        NavDestination.BOQ,
        NavDestination.AIAssistant,
        NavDestination.Report
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        if (isExpandedScreen) {
            // Tablet / Desktop layout with NavigationRail
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    destinations.forEachIndexed { index, dest ->
                        NavigationRailItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(dest.icon, contentDescription = dest.title) },
                            label = { Text(dest.title) }
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    RenderScreen(selectedTab, mainViewModel, { selectedTab = it }, { showProjectsDialog = true })
                }
            }
        } else {
            // Handheld Mobile layout with Bottom NavigationBar
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        destinations.forEachIndexed { index, dest ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = { Icon(dest.icon, contentDescription = dest.title) },
                                label = { Text(dest.title) }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    RenderScreen(selectedTab, mainViewModel, { selectedTab = it }, { showProjectsDialog = true })
                }
            }
        }
    }

    if (showProjectsDialog) {
        ProjectsDialog(
            viewModel = mainViewModel,
            onDismiss = { showProjectsDialog = false }
        )
    }
}

@Composable
private fun RenderScreen(
    tabIndex: Int,
    viewModel: MainViewModel,
    onNavigateToTab: (Int) -> Unit,
    onOpenProjectsDialog: () -> Unit
) {
    when (tabIndex) {
        0 -> DashboardScreen(viewModel, onNavigateToTab, onOpenProjectsDialog)
        1 -> CalculatorsScreen(viewModel)
        2 -> CADScreen(viewModel)
        3 -> BOQScreen(viewModel)
        4 -> AIAssistantScreen(viewModel, onNavigateToTab)
        5 -> ReportScreen(viewModel)
        else -> DashboardScreen(viewModel, onNavigateToTab, onOpenProjectsDialog)
    }
}
