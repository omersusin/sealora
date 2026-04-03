package com.omersusin.sealora.ui.util

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.omersusin.sealora.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun getWeatherGradientColors(condition: String): List<Color> {
    val lower = condition.lowercase(Locale.ROOT)
    return when {
        lower.contains("acik") || lower.contains("gunes") || lower.contains("clear") || lower.contains("sunny") -> listOf(SunnyGradient1, SunnyGradient2)
        lower.contains("yagmur") || lower.contains("rain") -> listOf(RainyGradient1, RainyGradient2)
        lower.contains("firtina") || lower.contains("storm") -> listOf(StormyGradient1, StormyGradient2)
        lower.contains("kar") || lower.contains("snow") -> listOf(SnowyGradient1, SnowyGradient2)
        lower.contains("sis") || lower.contains("fog") -> listOf(FoggyColor, FoggyColor.copy(alpha = 0.7f))
        lower.contains("bulut") || lower.contains("cloud") || lower.contains("kapali") -> listOf(CloudyGradient1, CloudyGradient2)
        else -> listOf(SealoraPrimary, SealoraPrimaryDark)
    }
}

fun formatTemperature(temp: Double): String = "${temp.toInt()}\u00B0"

fun formatTime(timeString: String): String {
    return try { if (timeString.contains("T")) timeString.split("T").getOrNull(1)?.substring(0, 5) ?: timeString else timeString } catch (e: Exception) { timeString }
}

fun formatDayName(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString)
        val today = LocalDate.now()
        when (date) { today -> "Bugun"; today.plusDays(1) -> "Yarin"; else -> date.format(DateTimeFormatter.ofPattern("EEEE", Locale("tr"))).replaceFirstChar { it.uppercase() } }
    } catch (e: Exception) { dateString }
}

fun getUvDescription(uv: Double): String = when { uv <= 2 -> "Dusuk"; uv <= 5 -> "Orta"; uv <= 7 -> "Yuksek"; uv <= 10 -> "Cok Yuksek"; else -> "Asiri" }

fun getHumidityDescription(humidity: Int): String = when { humidity < 30 -> "Cok Kuru"; humidity < 50 -> "Kuru"; humidity < 70 -> "Normal"; humidity < 85 -> "Nemli"; else -> "Cok Nemli" }

fun getPressureDescription(pressure: Int): String = when { pressure < 1000 -> "Dusuk Basin"; pressure < 1013 -> "Normal Alti"; pressure < 1020 -> "Normal"; pressure < 1030 -> "Normal Ustu"; else -> "Yuksek Basin" }
