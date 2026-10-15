package com.goraya.constructionkhata.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.goraya.constructionkhata.MainViewModel

@Composable
fun KhataTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = lightColorScheme(primary = Color(0xFF1B5E20), secondary = Color(0xFFEF6C00), primaryContainer = Color(0xFFC8E6C9)),
    content = content
)

private class BarTab(val route: String, val label: String, val icon: ImageVector)

@Composable
fun KhataApp(vm: MainViewModel) {
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val tabs = listOf(
        BarTab("home", "ہوم", Icons.Default.Home),
        BarTab("expense/0/0", "نیا خرچہ", Icons.Default.AddCircle),
        BarTab("cash", "کیش بُک", Icons.Default.AccountBalanceWallet),
        BarTab("items", "سامان", Icons.Default.Inventory2),
        BarTab("more", "مزید", Icons.Default.MoreHoriz)
    )
    val showBar = route in setOf("home", "cash", "items", "more")
    val go: (String) -> Unit = { r ->
        if (r in setOf("home", "cash", "items", "more")) nav.navigate(r) { popUpTo("home"); launchSingleTop = true } else nav.navigate(r)
    }
    val back: () -> Unit = { nav.popBackStack() }
    Scaffold(bottomBar = {
        if (showBar) NavigationBar {
            tabs.forEach { t ->
                NavigationBarItem(selected = route == t.route, onClick = { go(t.route) }, icon = { Icon(t.icon, t.label) }, label = { Text(t.label) })
            }
        }
    }) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
            composable("home") { HomeScreen(vm, go) }
            composable("cash") { CashBookScreen(vm) }
            composable("items") { ItemsScreen(vm, go) }
            composable("more") { MoreScreen(go) }
            composable(
                "expense/{eid}/{iid}",
                arguments = listOf(navArgument("eid") { type = NavType.LongType }, navArgument("iid") { type = NavType.LongType })
            ) { e -> ExpenseFormScreen(vm, e.arguments?.getLong("eid") ?: 0L, e.arguments?.getLong("iid") ?: 0L, back) }
            composable("labour") { LabourFormScreen(vm, back) }
            composable("parties") { PartiesScreen(vm, go, back) }
            composable("ledger/{pid}", arguments = listOf(navArgument("pid") { type = NavType.LongType })) { e ->
                LedgerScreen(vm, e.arguments?.getLong("pid") ?: 0L, go, back)
            }
            composable("transactions") { TransactionsScreen(vm, go, back) }
            composable("reports") { ReportsScreen(vm, back) }
            composable("budget") { BudgetScreen(vm, back) }
            composable("diary") { DiaryScreen(vm, back) }
            composable("locations") { LocationsScreen(vm, back) }
            composable("settings") { SettingsScreen(vm, back) }
            composable("backup") { BackupScreen(back) }
        }
    }
}
