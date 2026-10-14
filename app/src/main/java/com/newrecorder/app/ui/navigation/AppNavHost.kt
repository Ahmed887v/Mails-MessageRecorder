package com.newrecorder.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.newrecorder.app.NewRecorderApp
import com.newrecorder.app.ui.accounts.AccountsScreen
import com.newrecorder.app.ui.dashboard.DashboardScreen
import com.newrecorder.app.ui.messages.MessagesScreen
import com.newrecorder.app.ui.settings.SettingsScreen
import com.newrecorder.app.util.SourcePackages

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val repository = (context.applicationContext as NewRecorderApp).container.repository

    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onOpenAccounts = { pkg -> navController.navigate(Routes.accounts(pkg)) },
                onOpenMessages = { id -> navController.navigate(Routes.messages(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            route = Routes.ACCOUNTS,
            arguments = listOf(navArgument("packageName") { type = NavType.StringType })
        ) { backStackEntry ->
            val pkg = backStackEntry.arguments?.getString("packageName").orEmpty()
            AccountsScreen(
                appPackage = pkg,
                appTitle = SourcePackages.displayNameOf(pkg),
                onBack = { navController.popBackStack() },
                onOpenMessages = { id -> navController.navigate(Routes.messages(id)) }
            )
        }

        composable(
            route = Routes.MESSAGES,
            arguments = listOf(navArgument("accountId") { type = NavType.LongType })
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getLong("accountId") ?: 0L
            var title by remember { mutableStateOf("الرسائل") }
            LaunchedEffect(accountId) {
                repository.accountWithMessages(accountId)?.let {
                    title = it.account.displayName
                }
            }
            MessagesScreen(
                accountId = accountId,
                accountTitle = title,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
