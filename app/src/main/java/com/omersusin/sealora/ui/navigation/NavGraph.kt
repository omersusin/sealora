package com.omersusin.sealora.ui.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.omersusin.sealora.domain.model.WeatherData
import com.omersusin.sealora.domain.model.WeatherReport
import com.omersusin.sealora.ui.screens.detail.DetailScreen
import com.omersusin.sealora.ui.screens.home.HomeScreen
import com.omersusin.sealora.ui.screens.onboarding.OnboardingScreen
import com.omersusin.sealora.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Detail : Screen("detail")
    object Settings : Screen("settings")
}

object NavData {
    var weatherData: WeatherData? = null
    var weatherReport: WeatherReport? = null
}

@Composable
fun SealoraNavGraph(navController: NavHostController, isFirstLaunch: Boolean, onRequestLocation: () -> Unit = {}, hasLocationPermission: Boolean = false) {
    var showOnboarding by remember { mutableStateOf(isFirstLaunch) }
    NavHost(navController = navController, startDestination = if (showOnboarding) Screen.Onboarding.route else Screen.Home.route) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(onComplete = { showOnboarding = false; navController.navigate(Screen.Home.route) { popUpTo(Screen.Onboarding.route) { inclusive = true } } })
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToDetail = { data, report ->
                    NavData.weatherData = data
                    NavData.weatherReport = report
                    navController.navigate(Screen.Detail.route)
                },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onRequestLocation = onRequestLocation,
                hasLocationPermission = hasLocationPermission
            )
        }
        composable(Screen.Detail.route) {
            val data = NavData.weatherData
            if (data != null) {
                DetailScreen(weatherData = data, report = NavData.weatherReport, onNavigateBack = { navController.popBackStack() })
            }
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
