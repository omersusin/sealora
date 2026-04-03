package com.omersusin.sealora.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.omersusin.sealora.domain.model.*
import com.omersusin.sealora.ui.components.CurrentWeatherHeader
import com.omersusin.sealora.ui.theme.*
import com.omersusin.sealora.ui.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(weatherData: WeatherData, report: WeatherReport?, onNavigateBack: () -> Unit, viewModel: DetailViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(weatherData) { viewModel.loadData(weatherData, report) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Column { Text(weatherData.providerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(weatherData.city, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
            navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") } }
        )
    }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 32.dp)) {
            item { CurrentWeatherHeader(weatherData = weatherData, report = null, modifier = Modifier.padding(vertical = 8.dp)) }
            item { Spacer(Modifier.height(16.dp)); DetailStatsGrid(weatherData) }

            if (weatherData.hourlyForecast.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Schedule, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Saatlik Tahmin (${weatherData.hourlyForecast.size} saat)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(12.dp))
                }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(weatherData.hourlyForecast.size) { i ->
                            val h = weatherData.hourlyForecast[i]
                            Card(onClick = { viewModel.showHourlyDetail(h) }, modifier = Modifier.width(100.dp), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(formatTime(h.time), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(8.dp)); Text(h.conditionIcon, fontSize = 28.sp)
                                    Spacer(Modifier.height(8.dp)); Text(formatTemperature(h.temperature), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("${h.precipitationChance}%", style = MaterialTheme.typography.labelSmall, color = SealoraPrimary)
                                }
                            }
                        }
                    }
                }
            }

            if (weatherData.dailyForecast.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("${weatherData.dailyForecast.size} Gunluk Tahmin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(12.dp))
                }
                items(weatherData.dailyForecast.size) { i ->
                    val d = weatherData.dailyForecast[i]
                    val expanded = uiState.expandedDailyIndex == i
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(containerColor = if (expanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface)) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.expandDaily(i) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(d.conditionIcon, fontSize = 28.sp, modifier = Modifier.width(40.dp))
                                Column(modifier = Modifier.weight(1f)) { Text(formatDayName(d.date), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold); Text(d.condition, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(formatTemperature(d.tempMin), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                                    val norm = ((d.tempMax + 10).coerceIn(-10.0, 40.0) + 10) / 50.0
                                    Box(modifier = Modifier.width(60.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(norm.toFloat()).clip(RoundedCornerShape(3.dp)).background(Brush.horizontalGradient(listOf(SealoraPrimaryLight, ErrorColor.copy(alpha = 0.6f)))))
                                    }
                                    Text(formatTemperature(d.tempMax), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                            }
                            if (expanded) {
                                Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                    Spacer(Modifier.height(12.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Row { Text("Sunrise: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(d.sunrise, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold) }
                                        Row { Text("Sunset: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(d.sunset, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold) }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Row { Text("Wind: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${d.windSpeed.toInt()} km/h", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold) }
                                        Row { Text("Rain: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${d.precipitationChance}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold) }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Row { Text("UV: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${d.uvIndexMax.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold) }
                                        Row { Text("Avg: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(formatTemperature(d.avgTemp), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold) }
                                    }
                                    if (d.hourlyDetail.isNotEmpty()) {
                                        Spacer(Modifier.height(12.dp))
                                        Text("Hourly Detail", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.height(8.dp))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(d.hourlyDetail.size) { hi ->
                                                val hd = d.hourlyDetail[hi]
                                                Column(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(formatTime(hd.time), style = MaterialTheme.typography.labelSmall); Text(hd.conditionIcon, fontSize = 18.sp); Text(formatTemperature(hd.temperature), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showHourlyDetail && uiState.selectedHourlyData != null) {
        val d = uiState.selectedHourlyData!!
        ModalBottomSheet(onDismissRequest = { viewModel.dismissHourlyDetail() }, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(d.conditionIcon, fontSize = 48.sp); Spacer(Modifier.height(8.dp))
                Text(formatTime(d.time), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(d.condition, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Text(formatTemperature(d.temperature), style = MaterialTheme.typography.headlineLarge.copy(fontSize = 48.sp), fontWeight = FontWeight.Light)
                Text("Hissedilen ${formatTemperature(d.feelsLike)}")
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${d.humidity}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("Nem", style = MaterialTheme.typography.labelSmall) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${d.windSpeed.toInt()} km/h", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("Ruzgar", style = MaterialTheme.typography.labelSmall) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${d.precipitationChance}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("Yagis", style = MaterialTheme.typography.labelSmall) }
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${d.uvIndex.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("UV", style = MaterialTheme.typography.labelSmall) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${d.cloudCover}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("Bulut", style = MaterialTheme.typography.labelSmall) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${d.dewPoint.toInt()}\u00B0", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("Ciy", style = MaterialTheme.typography.labelSmall) }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun DetailStatsGrid(weatherData: WeatherData) {
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Detayli Bilgiler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatItem("\uD83D\uDCA7", "Nem", "${weatherData.humidity}%", Modifier.weight(1f))
            StatItem("\uD83D\uDCA8", "Ruzgar", "${weatherData.windSpeed.toInt()} km/h", Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatItem("\u2600\uFE0F", "UV", "${weatherData.uvIndex.toInt()}", Modifier.weight(1f))
            StatItem("\uD83C\uDF21\uFE0F", "Basinc", "${weatherData.pressure} hPa", Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatItem("\u2601\uFE0F", "Bulut", "${weatherData.cloudCover}%", Modifier.weight(1f))
            StatItem("\uD83C\uDF27\uFE0F", "Yagis", "${weatherData.precipitation} mm", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatItem(icon: String, label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 28.sp); Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
