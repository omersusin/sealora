package com.omersusin.sealora.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightScheme = lightColorScheme(
    primary = SealoraPrimary, onPrimary = Color.White,
    primaryContainer = SealoraPrimaryLight, onPrimaryContainer = SealoraPrimaryDark,
    secondary = SealoraSecondary, onSecondary = Color.White,
    secondaryContainer = SealoraSecondaryLight, onSecondaryContainer = SealoraSecondaryDark,
    background = SealoraBackground, onBackground = SealoraTextPrimary,
    surface = SealoraSurface, onSurface = SealoraTextPrimary,
    surfaceVariant = SealoraSurfaceVariant, onSurfaceVariant = SealoraTextSecondary,
    error = ErrorColor, onError = Color.White
)

private val DarkScheme = darkColorScheme(
    primary = SealoraPrimaryDarkTheme, onPrimary = Color.Black,
    primaryContainer = SealoraPrimaryDark, onPrimaryContainer = SealoraPrimaryLight,
    secondary = SealoraSecondary, onSecondary = Color.Black,
    background = SealoraBackgroundDark, onBackground = SealoraTextDark,
    surface = SealoraSurfaceDark, onSurface = SealoraTextDark,
    surfaceVariant = SealoraSurfaceVariantDark, onSurfaceVariant = SealoraTextDark.copy(alpha = 0.8f),
    error = ErrorColor, onError = Color.White
)

@Composable
fun SealoraTheme(darkTheme: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = true, content: @Composable () -> Unit) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkScheme
        else -> LightScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colorScheme, typography = SealoraTypography, content = content)
}
