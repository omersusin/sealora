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

private val LightColorScheme = lightColorScheme(
    primary = SealoraPrimary,
    onPrimary = Color.White,
    primaryContainer = SealoraPrimaryLight,
    onPrimaryContainer = SealoraPrimaryDark,
    secondary = SealoraSecondary,
    onSecondary = Color.White,
    secondaryContainer = SealoraSecondaryLight,
    onSecondaryContainer = SealoraSecondaryDark,
    tertiary = SealoraAccent,
    onTertiary = SealoraTextPrimary,
    background = SealoraBackground,
    onBackground = SealoraTextPrimary,
    surface = SealoraSurface,
    onSurface = SealoraTextPrimary,
    surfaceVariant = SealoraSurfaceVariant,
    onSurfaceVariant = SealoraTextSecondary,
    outline = SealoraTextSecondary.copy(alpha = 0.5f),
    error = ErrorColor,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = SealoraPrimaryDarkTheme,
    onPrimary = Color.Black,
    primaryContainer = SealoraPrimaryDark,
    onPrimaryContainer = SealoraPrimaryLight,
    secondary = SealoraSecondary,
    onSecondary = Color.Black,
    secondaryContainer = SealoraSecondaryDark,
    onSecondaryContainer = SealoraSecondaryLight,
    tertiary = SealoraAccent,
    onTertiary = Color.Black,
    background = SealoraBackgroundDark,
    onBackground = SealoraTextDark,
    surface = SealoraSurfaceDark,
    onSurface = SealoraTextDark,
    surfaceVariant = SealoraSurfaceVariantDark,
    onSurfaceVariant = SealoraTextDark.copy(alpha = 0.8f),
    outline = SealoraTextDark.copy(alpha = 0.3f),
    error = ErrorColor,
    onError = Color.White
)

@Composable
fun SealoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SealoraTypography,
        content = content
    )
}
