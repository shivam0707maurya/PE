package com.poojaenterprises.pe.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.poojaenterprises.pe.ui.screens.CustomersManageScreen
import com.poojaenterprises.pe.ui.screens.HistoryScreen
import com.poojaenterprises.pe.ui.screens.ItemsManageScreen
import com.poojaenterprises.pe.ui.screens.NewBillScreen
import com.poojaenterprises.pe.ui.screens.SettingsScreen
import com.poojaenterprises.pe.ui.screens.SetupScreen
import com.poojaenterprises.pe.ui.screens.WhatsAppNumbersManageScreen
import com.poojaenterprises.pe.ui.theme.RedDark
import com.poojaenterprises.pe.ui.theme.RedPrimary
import com.poojaenterprises.pe.ui.viewmodel.MainViewModel

enum class MainTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    NEW_BILL("New Bill", Icons.Default.PostAdd),
    HISTORY("History", Icons.Default.History),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val isSetupComplete by viewModel.isSetupComplete.collectAsState()

    if (!isSetupComplete) {
        SetupScreen(viewModel = viewModel)
    } else {
        val navController = rememberNavController()
        var selectedTab by remember { mutableStateOf(MainTab.NEW_BILL) }

        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    MainTab.values().forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = {
                                selectedTab = tab
                                navController.navigate(tab.name) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = {
                                Text(
                                    text = tab.label,
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = RedPrimary,
                                selectedTextColor = RedPrimary,
                                indicatorColor = Color(0xFFFFEBEE)
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = MainTab.NEW_BILL.name,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(MainTab.NEW_BILL.name) {
                    NewBillScreen(viewModel = viewModel)
                }
                composable(MainTab.HISTORY.name) {
                    HistoryScreen(viewModel = viewModel)
                }
                composable(MainTab.SETTINGS.name) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateToCustomers = { navController.navigate("manage_customers") },
                        onNavigateToItems = { navController.navigate("manage_items") },
                        onNavigateToNumbers = { navController.navigate("manage_numbers") }
                    )
                }
                composable("manage_customers") {
                    CustomersManageScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("manage_items") {
                    ItemsManageScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("manage_numbers") {
                    WhatsAppNumbersManageScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
