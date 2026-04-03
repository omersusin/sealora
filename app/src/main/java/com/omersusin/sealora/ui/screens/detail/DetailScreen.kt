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
fun DetailScreen(
    weatherData: WeatherData,
    report: WeatherReport?,
    onNavigateBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(weatherData) {
        viewModel.loadData(weatherData, report)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = weatherData.providerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = weatherData.city,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                CurrentWeatherHeader(
                    weatherData = weatherData,
                    report = null,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                DetailedStatsGrid(weatherData = weatherData)
            }

            if (weatherData.hourlyForecast.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Schedule, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saatlik Tahmin (${weatherData.hourlyForecast.size} saat)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(weatherData.hourlyForecast.size) { index ->
                            val hourly = weatherData.hourlyForecast[index]
                            DetailedHourlyCard(data = hourly, onClick = { viewModel.showHourlyDetail(hourly) })
                        }
                    }
                }
            }

            if (weatherData.dailyForecast.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${weatherData.dailyForecast.size} Günlük Tahmin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                items(weatherData.dailyForecast.size) { index ->
                    val daily = weatherData.dailyForecast[index]
                    DetailDailyItem(
                        data = daily,
                        isExpanded = uiState.expandedDailyIndex == index,
                        onClick = { viewModel.expandDaily(index) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    if (uiState.showHourlyDetail && uiState.selectedHourlyData != null) {
        HourlyDetailDialog(data = uiState.selectedHourlyData!!, onDismiss = { viewModel.dismissHourlyDetail() })
    }
}

@Composable
private fun DetailedStatsGrid(weatherData: WeatherData, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Detaylı Bilgiler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("💧", "Nem", "${weatherData.humidity}%", getHumidityDescription(weatherData.humidity), Modifier.weight(1f))
            StatCard("💨", "Rüzgar", "${weatherData.windSpeed.toInt()} km/h", weatherData.windDirection, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("☀️", "UV", "${weatherData.uvIndex.toInt()}", getUvDescription(weatherData.uvIndex), Modifier.weight(1f))
            StatCard("🌡️", "Basınç", "${weatherData.pressure} hPa", getPressureDescription(weatherData.pressure), Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("☁️", "Bulut", "${weatherData.cloudCover}%", "", Modifier.weight(1f))
            StatCard("🌧️", "Yağış", "${weatherData.precipitation} mm", "", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(icon: String, label: String, value: String, description: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (description.isNotBlank()) {
                Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun DetailedHourlyCard(data: HourlyData, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.width(100.dp), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatTime(data.time), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(data.conditionIcon, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(formatTemperature(data.temperature), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${data.precipitationChance}%", style = MaterialTheme.typography.labelSmall, color = SealoraPrimary)
        }
    }
}

@Composable
private fun DetailDailyItem(data: DailyData, isExpanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = if (isExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(data.conditionIcon, fontSize = 28.sp, modifier = Modifier.width(40.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(formatDayName(data.date), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(data.condition, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(formatTemperature(data.tempMin), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    DetailTemperatureBar(data.tempMin, data.tempMax, Modifier.width(60.dp).height(6.dp))
                    Text(formatTemperature(data.tempMax), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            }
            if (isExpanded) {
                Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        DetailStatChip("🌅 Doğum", data.sunrise)
                        DetailStatChip("🌇 Batım", data.sunset)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        DetailStatChip("💨 Rüzgar", "${data.windSpeed.toInt()} km/h")
                        DetailStatChip("🌧️ Yağış", "${data.precipitationChance}%")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        DetailStatChip("☀️ UV", "${data.uvIndexMax.toInt()}")
                        DetailStatChip("🌡️ Ort.", formatTemperature(data.avgTemp))
                    }
                    if (data.hourlyDetail.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Saatlik Detay", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(data.hourlyDetail.size) { i ->
                                val h = data.hourlyDetail[i]
                                Column(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(formatTime(h.time), style = MaterialTheme.typography.labelSmall)
                                    Text(h.conditionIcon, fontSize = 18.sp)
                                    Text(formatTemperature(h.temperature), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailStatChip(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DetailTemperatureBar(minTemp: Double, maxTemp: Double, modifier: Modifier = Modifier) {
    val norm = ((maxTemp + 10).coerceIn(-10.0, 40.0) + 10) / 50.0
    Box(modifier = modifier.clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(norm.toFloat()).clip(RoundedCornerShape(3.dp)).background(Brush.horizontalGradient(listOf(SealoraPrimaryLight, ErrorColor.copy(alpha = 0.6f)))))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HourlyDetailDialog(data: HourlyData, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(data.conditionIcon, fontSize = 48.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(formatTime(data.time), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(data.condition, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Text(formatTemperature(data.temperature), style = MaterialTheme.typography.headlineLarge.copy(fontSize = 48.sp), fontWeight = FontWeight.Light)
            Text("Hissedilen ${formatTemperature(data.feelsLike)}")
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                DetailStatColumn("💧 Nem", "${data.humidity}%")
                DetailStatColumn("💨 Rüzgar", "${data.windSpeed.toInt()} km/h")
                DetailStatColumn("🌧️ Yağış", "${data.precipitationChance}%")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                DetailStatColumn("☀️ UV", "${data.uvIndex.toInt()}")
                DetailStatColumn("☁️ Bulut", "${data.cloudCover}%")
                DetailStatColumn("🌡️ Çiy", "${data.dewPoint.toInt()}°")
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DetailStatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
