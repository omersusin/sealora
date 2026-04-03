package com.omersusin.sealora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.omersusin.sealora.ui.navigation.SealoraNavGraph
import com.omersusin.sealora.ui.theme.SealoraTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SealoraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    val isFirstLaunch = remember {
                        val prefs = getSharedPreferences("sealora_prefs", MODE_PRIVATE)
                        val launched = prefs.getBoolean("has_launched", false)
                        if (!launched) {
                            prefs.edit().putBoolean("has_launched", true).apply()
                        }
                        !launched
                    }

                    SealoraNavGraph(
                        navController = navController,
                        isFirstLaunch = isFirstLaunch
                    )
                }
            }
        }
    }
}
