package com.omersusin.sealora.ui.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.omersusin.sealora.domain.model.WeatherData
import com.omersusin.sealora.domain.model.WeatherReport
import com.omersusin.sealora.ui.screens.detail.DetailScreen
import com.omersusin.sealora.ui.screens.home.HomeScreen
import com.omersusin.sealora.ui.screens.onboarding.OnboardingScreen
import com.omersusin.sealora.ui.screens.settings.SettingsScreen
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Detail : Screen("detail/{weatherData}/{report}") {
        fun createRoute(weatherDataJson: String, reportJson: String): String {
            val encodedData = URLEncoder.encode(weatherDataJson, "UTF-8")
            val encodedReport = URLEncoder.encode(reportJson, "UTF-8")
            return "detail/$encodedData/$encodedReport"
        }
    }
    object Settings : Screen("settings")
}

@Composable
fun SealoraNavGraph(
    navController: NavHostController,
    isFirstLaunch: Boolean
) {
    var showOnboarding by remember { mutableStateOf(isFirstLaunch) }

    NavHost(
        navController = navController,
        startDestination = if (showOnboarding) Screen.Onboarding.route else Screen.Home.route
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    showOnboarding = false
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToDetail = { weatherData, report ->
                    val dataJson = Json.encodeToString(weatherData)
                    val reportJson = Json.encodeToString(report)
                    navController.navigate(Screen.Detail.createRoute(dataJson, reportJson))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Detail.route) { backStackEntry ->
            val encodedData = backStackEntry.arguments?.getString("weatherData") ?: ""
            val encodedReport = backStackEntry.arguments?.getString("report") ?: ""

            val dataJson = URLDecoder.decode(encodedData, "UTF-8")
            val reportJson = URLDecoder.decode(encodedReport, "UTF-8")

            val weatherData = try {
                Json.decodeFromString<WeatherData>(dataJson)
            } catch (e: Exception) {
                null
            }

            val report = try {
                Json.decodeFromString<WeatherReport?>(reportJson)
            } catch (e: Exception) {
                null
            }

            if (weatherData != null) {
                DetailScreen(
                    weatherData = weatherData,
                    report = report,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
