package com.omersusin.sealora.ui.util

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.omersusin.sealora.ui.theme.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.LightGray.copy(alpha = 0.6f),
                Color.LightGray.copy(alpha = 0.2f),
                Color.LightGray.copy(alpha = 0.6f)
            ),
            start = Offset.Zero,
            end = Offset(x = translateAnim.value, y = translateAnim.value)
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

fun Modifier.weatherCardGradient(condition: String): Modifier = composed {
    val colors = getWeatherGradientColors(condition)
    background(
        brush = Brush.verticalGradient(colors = colors),
        shape = RoundedCornerShape(20.dp)
    )
}

fun getWeatherGradientColors(condition: String): List<Color> {
    val lower = condition.lowercase(Locale.ROOT)
    return when {
        lower.contains("açık") || lower.contains("güneş") || lower.contains("clear") || lower.contains("sunny") ->
            listOf(SunnyGradient1, SunnyGradient2)
        lower.contains("yağmur") || lower.contains("rain") || lower.contains("çisenti") ->
            listOf(RainyGradient1, RainyGradient2)
        lower.contains("fırtına") || lower.contains("storm") || lower.contains("thunder") ->
            listOf(StormyGradient1, StormyGradient2)
        lower.contains("kar") || lower.contains("snow") ->
            listOf(SnowyGradient1, SnowyGradient2)
        lower.contains("sis") || lower.contains("fog") ->
            listOf(FoggyColor, FoggyColor.copy(alpha = 0.7f))
        lower.contains("bulut") || lower.contains("cloud") || lower.contains("kapalı") ->
            listOf(CloudyGradient1, CloudyGradient2)
        else -> listOf(SealoraPrimary, SealoraPrimaryDark)
    }
}

fun formatTemperature(temp: Double): String {
    return "${temp.toInt()}°"
}

fun formatTime(timeString: String): String {
    return try {
        if (timeString.contains("T")) {
            val parts = timeString.split("T")
            parts.getOrNull(1)?.substring(0, 5) ?: timeString
        } else {
            timeString
        }
    } catch (e: Exception) {
        timeString
    }
}

fun formatDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString)
        val formatter = DateTimeFormatter.ofPattern("dd MMM", Locale("tr"))
        date.format(formatter)
    } catch (e: Exception) {
        dateString
    }
}

fun formatDayName(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString)
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        when (date) {
            today -> "Bugün"
            tomorrow -> "Yarın"
            else -> {
                val formatter = DateTimeFormatter.ofPattern("EEEE", Locale("tr"))
                date.format(formatter).replaceFirstChar { it.uppercase() }
            }
        }
    } catch (e: Exception) {
        dateString
    }
}

fun getWeatherEmoji(condition: String): String {
    val lower = condition.lowercase()
    return when {
        lower.contains("açık") || lower.contains("clear") || lower.contains("sunny") -> "☀️"
        lower.contains("parçalı") || lower.contains("partly") -> "⛅"
        lower.contains("bulut") || lower.contains("cloud") || lower.contains("kapalı") -> "☁️"
        lower.contains("yağmur") || lower.contains("rain") -> "🌧️"
        lower.contains("çisenti") || lower.contains("drizzle") -> "🌦️"
        lower.contains("fırtına") || lower.contains("storm") -> "⛈️"
        lower.contains("kar") || lower.contains("snow") -> "❄️"
        lower.contains("sis") || lower.contains("fog") -> "🌫️"
        lower.contains("rüzgar") || lower.contains("wind") -> "💨"
        else -> "🌡️"
    }
}

fun getWindDirection(degrees: Double): String {
    return when {
        degrees < 22.5 -> "K"
        degrees < 67.5 -> "KD"
        degrees < 112.5 -> "D"
        degrees < 157.5 -> "GD"
        degrees < 202.5 -> "G"
        degrees < 247.5 -> "GB"
        degrees < 292.5 -> "B"
        degrees < 337.5 -> "KB"
        else -> "K"
    }
}

fun getUvDescription(uv: Double): String {
    return when {
        uv <= 2 -> "Düşük"
        uv <= 5 -> "Orta"
        uv <= 7 -> "Yüksek"
        uv <= 10 -> "Çok Yüksek"
        else -> "Aşırı"
    }
}

fun getUvColor(uv: Double): Color {
    return when {
        uv <= 2 -> SuccessColor
        uv <= 5 -> InfoColor
        uv <= 7 -> WarningColor
        uv <= 10 -> ErrorColor
        else -> Color(0xFF7C3AED)
    }
}

fun getHumidityDescription(humidity: Int): String {
    return when {
        humidity < 30 -> "Çok Kuru"
        humidity < 50 -> "Kuru"
        humidity < 70 -> "Normal"
        humidity < 85 -> "Nemli"
        else -> "Çok Nemli"
    }
}

fun getPressureDescription(pressure: Int): String {
    return when {
        pressure < 1000 -> "Düşük Basınç"
        pressure < 1013 -> "Normal Altı"
        pressure < 1020 -> "Normal"
        pressure < 1030 -> "Normal Üstü"
        else -> "Yüksek Basınç"
    }
}
