package com.example.habitup.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.habitup.ui.detail.HabitDetailScreen
import com.example.habitup.ui.form.HabitFormScreen
import com.example.habitup.ui.login.LoginScreen
import com.example.habitup.ui.settings.SettingsScreen
import com.example.habitup.ui.splash.SplashScreen
import com.example.habitup.ui.today.TodayScreen

@Composable
fun AppNavHost(gateViewModel: AuthGateViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val authState by gateViewModel.authState.collectAsStateWithLifecycle()

    // Cổng duy nhất: SignedOut thì luôn về Login (dù đang ở màn nào). SignedIn chỉ đưa tới
    // Today khi đang ở Splash/Login — một lần FirebaseAuth tự làm mới token (cùng uid) cũng
    // phát lại currentUser, nếu không có điều kiện này người dùng sẽ bị bật khỏi màn đang xem.
    LaunchedEffect(authState) {
        when (authState) {
            AuthState.Loading -> Unit
            AuthState.SignedOut -> navController.resetTo(Login)
            is AuthState.SignedIn -> {
                val onAuthScreen = navController.currentDestination?.hasRoute<Splash>() == true ||
                    navController.currentDestination?.hasRoute<Login>() == true
                if (onAuthScreen) navController.resetTo(Today)
            }
        }
    }

    NavHost(navController = navController, startDestination = Splash) {
        composable<Splash> { SplashScreen() }
        composable<Login> { LoginScreen() }
        composable<Today> {
            TodayScreen(
                onAddHabit = { navController.navigate(HabitForm()) },
                onHabitClick = { id -> navController.navigate(HabitDetail(id)) },
                onOpenSettings = { navController.navigate(Settings) },
            )
        }
        composable<HabitForm> {
            HabitFormScreen(onBack = { navController.navigateUp() })
        }
        composable<HabitDetail> {
            HabitDetailScreen(
                onBack = { navController.navigateUp() },
                onEdit = { id -> navController.navigate(HabitForm(id)) },
                onDeleted = { navController.navigateUp() },
            )
        }
        composable<Settings> {
            SettingsScreen(onBack = { navController.navigateUp() })
        }
    }
}

/**
 * Điều hướng tới [route] và xoá toàn bộ back stack, để Back không quay lại màn trước.
 * Bỏ qua nếu đang ở đúng màn hình đó (ví dụ sau khi xoay màn hình).
 */
private inline fun <reified T : Any> NavController.resetTo(route: T) {
    if (currentDestination?.hasRoute<T>() == true) return
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
