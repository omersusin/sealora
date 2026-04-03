package com.omersusin.sealora.ui.screens.home

import android.content.Context
import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.omersusin.sealora.domain.model.*
import com.omersusin.sealora.ui.components.*
import com.omersusin.sealora.ui.theme.*
import com.omersusin.sealora.ui.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (WeatherData, WeatherReport?) -> Unit,
    onNavigateToSettings: () -> Unit,
    onRequestLocation: () -> Unit = {},
    hasLocationPermission: Boolean = false,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoadingLocation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Sealora", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = SealoraPrimary)
                        if (uiState.city.isNotBlank()) {
                            Text(uiState.city, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(HomeEvent.ToggleChatSheet) }) {
                        Icon(Icons.Outlined.ChatBubbleOutline, "AI Sohbet")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Outlined.Settings, "Ayarlar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.city,
                    onValueChange = { viewModel.onEvent(HomeEvent.UpdateCity(it)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Sehir ara...") },
                    leadingIcon = {
                        if (uiState.isLoading || isLoadingLocation) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.Search, "Ara")
                        }
                    },
                    trailingIcon = {
                        if (uiState.city.isNotBlank()) {
                            IconButton(onClick = { viewModel.onEvent(HomeEvent.UpdateCity("")) }) {
                                Icon(Icons.Default.Close, "Temizle")
                            }
                        } else {
                            IconButton(onClick = {
                                if (hasLocationPermission) {
                                    isLoadingLocation = true
                                    scope.launch {
                                        val city = getLastKnownCity(context)
                                        if (city != null) {
                                            viewModel.onEvent(HomeEvent.UpdateCity(city))
                                            viewModel.onEvent(HomeEvent.LoadWeather(city))
                                        }
                                        isLoadingLocation = false
                                    }
                                } else {
                                    onRequestLocation()
                                }
                            }) {
                                Icon(Icons.Outlined.MyLocation, "Konum")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { if (uiState.city.isNotBlank()) viewModel.onEvent(HomeEvent.LoadWeather(uiState.city)) }),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SealoraPrimary)
                )
            }

            if (uiState.error != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ErrorColor.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.ErrorOutline, null, tint = ErrorColor, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Hata", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ErrorColor)
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { viewModel.onEvent(HomeEvent.ClearError) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, "Kapat")
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(uiState.error!!, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { viewModel.onEvent(HomeEvent.RefreshWeather) }) {
                                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Tekrar Dene")
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(32.dp))
                        Text("Yukleniyor...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = SealoraPrimary)
                        Spacer(Modifier.height(8.dp))
                        Text("Hava durumu verileri cekiliyor", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (uiState.weatherDataList.isNotEmpty() && !uiState.isLoading) {
                item {
                    CurrentWeatherHeader(
                        weatherData = uiState.weatherDataList.first(),
                        report = uiState.report,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                if (uiState.isGeneratingReport) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SealoraPrimaryLight.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("AI rapor olusturuyor...", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = SealoraPrimary)
                        }
                    }
                }

                val primaryData = uiState.weatherDataList.first()

                if (primaryData.hourlyForecast.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        HourlyForecastRow(
                            hourlyData = primaryData.hourlyForecast,
                            selectedIndex = uiState.selectedHourlyIndex,
                            onHourClick = { viewModel.onEvent(HomeEvent.SelectHourlyIndex(it)) }
                        )
                    }
                }

                if (primaryData.dailyForecast.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        DailyForecastList(
                            dailyData = primaryData.dailyForecast,
                            selectedIndex = uiState.selectedDailyIndex,
                            onDayClick = { viewModel.onEvent(HomeEvent.SelectDailyIndex(it)) }
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Source, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("${uiState.weatherDataList.size} Saglayicidan Veri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                }

                items(uiState.weatherDataList.size) { index ->
                    ProviderWeatherCard(
                        weatherData = uiState.weatherDataList[index],
                        onClick = { onNavigateToDetail(uiState.weatherDataList[index], uiState.report) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            if (uiState.weatherDataList.isEmpty() && !uiState.isLoading && uiState.error == null) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("\uD83C\uDF0A", fontSize = 64.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("Hava Durumu Ara", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Bir sehir adi girerek veya konum butonuna basarak hava durumu bilgisini alin", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (uiState.showChatSheet) {
        var chatInput by remember { mutableStateOf("") }
        ModalBottomSheet(
            onDismissRequest = { viewModel.onEvent(HomeEvent.ToggleChatSheet) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("\uD83E\uDD16", fontSize = 24.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("Sealora AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { viewModel.onEvent(HomeEvent.ToggleChatSheet) }) {
                        Icon(Icons.Default.Close, "Kapat")
                    }
                }
                Divider()
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.chatMessages.isEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("\uD83D\uDCAC", fontSize = 48.sp)
                                Spacer(Modifier.height(16.dp))
                                Text("AI ile sohbet et", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Text("Hava durumu hakkinda sorular sorabilirsiniz.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            }
                        }
                    }
                    items(uiState.chatMessages.size) { index ->
                        val msg = uiState.chatMessages[index]
                        val isUser = msg.role == "user"
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
                            if (!isUser) {
                                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(SealoraPrimary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                    Text("\uD83E\uDD16", fontSize = 16.sp)
                                }
                                Spacer(Modifier.width(8.dp))
                            }
                            Card(
                                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (isUser) 16.dp else 4.dp, bottomEnd = if (isUser) 4.dp else 16.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isUser) SealoraPrimary else MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Text(msg.content, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium, color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (uiState.isChatLoading) {
                        item {
                            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                                Text("Yaziyor...", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Divider()
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Hava durumu hakkinda sor...") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { if (chatInput.isNotBlank()) { viewModel.onEvent(HomeEvent.SendChatMessage(chatInput)); chatInput = "" } })
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { if (chatInput.isNotBlank()) { viewModel.onEvent(HomeEvent.SendChatMessage(chatInput)); chatInput = "" } },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SealoraPrimary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Gonder", modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

private suspend fun getLastKnownCity(context: Context): String? {
    return withContext(Dispatchers.IO) {
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            val providers = lm.getProviders(true)
            var best: android.location.Location? = null
            for (p in providers) {
                val loc = lm.getLastKnownLocation(p) ?: continue
                if (best == null || loc.accuracy < best.accuracy) best = loc
            }
            if (best != null) {
                val geocoder = Geocoder(context, Locale("tr"))
                @Suppress("DEPRECATION")
                val addr = geocoder.getFromLocation(best.latitude, best.longitude, 1)
                if (!addr.isNullOrEmpty()) addr[0].locality ?: addr[0].subAdminArea else null
            } else null
        } catch (e: Exception) { null }
    }
}
