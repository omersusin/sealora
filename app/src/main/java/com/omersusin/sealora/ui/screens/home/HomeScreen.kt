package com.omersusin.sealora.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (WeatherData, WeatherReport?) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val providers by viewModel.activeProviders.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Sealora",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = SealoraPrimary
                        )
                        if (uiState.city.isNotBlank()) {
                            Text(
                                text = uiState.city,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Chat Button
                    IconButton(onClick = { viewModel.onEvent(HomeEvent.ToggleChatSheet) }) {
                        Badge(
                            containerColor = if (uiState.report != null) SealoraPrimary else Color.Transparent
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "AI Sohbet",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    // Settings
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Ayarlar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Search Bar
            item {
                CitySearchBar(
                    city = uiState.city,
                    onCityChange = { viewModel.onEvent(HomeEvent.UpdateCity(it)) },
                    onSearch = {
                        if (uiState.city.isNotBlank()) {
                            viewModel.onEvent(HomeEvent.LoadWeather(uiState.city))
                        }
                    },
                    isLoading = uiState.isLoading,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Error State
            if (uiState.error != null) {
                item {
                    ErrorCard(
                        message = uiState.error!!,
                        onRetry = { viewModel.onEvent(HomeEvent.RefreshWeather) },
                        onDismiss = { viewModel.onEvent(HomeEvent.ClearError) },
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Loading State
            if (uiState.isLoading) {
                item {
                    LoadingWeatherCard()
                }
            }

            // Weather Data
            if (uiState.weatherDataList.isNotEmpty() && !uiState.isLoading) {
                // Current Weather Header (using average/first provider)
                item {
                    val primaryData = uiState.weatherDataList.first()
                    CurrentWeatherHeader(
                        weatherData = primaryData,
                        report = uiState.report,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Report Generation Status
                if (uiState.isGeneratingReport) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = SealoraPrimaryLight.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = SealoraPrimary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "🤖 AI rapor oluşturuyor...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SealoraPrimary
                                )
                            }
                        }
                    }
                }

                // Alerts
                uiState.report?.alerts?.let { alerts ->
                    if (alerts.isNotEmpty()) {
                        item {
                            WeatherAlertsCard(
                                alerts = alerts,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                // Hourly Forecast
                val primaryData = uiState.weatherDataList.first()
                if (primaryData.hourlyForecast.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        HourlyForecastRow(
                            hourlyData = primaryData.hourlyForecast,
                            selectedIndex = uiState.selectedHourlyIndex,
                            onHourClick = { viewModel.onEvent(HomeEvent.SelectHourlyIndex(it)) }
                        )
                    }
                }

                // Daily Forecast
                if (primaryData.dailyForecast.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        DailyForecastList(
                            dailyData = primaryData.dailyForecast,
                            selectedIndex = uiState.selectedDailyIndex,
                            onDayClick = { viewModel.onEvent(HomeEvent.SelectDailyIndex(it)) }
                        )
                    }
                }

                // Provider Cards
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Source,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${uiState.weatherDataList.size} Sağlayıcıdan Veri",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(uiState.weatherDataList.size) { index ->
                    val data = uiState.weatherDataList[index]
                    ProviderWeatherCard(
                        weatherData = data,
                        onClick = { onNavigateToDetail(data, uiState.report) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // Empty State
            if (uiState.weatherDataList.isEmpty() && !uiState.isLoading && uiState.error == null) {
                item {
                    EmptyStateCard(
                        icon = {
                            Text("🌊", fontSize = 64.sp)
                        },
                        title = "Hava Durumu Ara",
                        subtitle = "Bir şehir adı girerek hava durumu bilgisini alın"
                    )
                }
            }
        }
    }

    // Chat Bottom Sheet
    if (uiState.showChatSheet) {
        ChatBottomSheet(
            messages = uiState.chatMessages,
            inputText = uiState.chatInput,
            isLoading = uiState.isChatLoading,
            onInputChange = { /* handled by viewmodel */ },
            onSend = { message ->
                viewModel.onEvent(HomeEvent.SendChatMessage(message))
            },
            onDismiss = { viewModel.onEvent(HomeEvent.ToggleChatSheet) }
        )
    }
}

@Composable
private fun CitySearchBar(
    city: String,
    onCityChange: (String) -> Unit,
    onSearch: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = city,
        onValueChange = onCityChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Şehir ara... (ör: İstanbul, Ankara)") },
        leadingIcon = {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Outlined.Search, contentDescription = "Ara")
            }
        },
        trailingIcon = {
            if (city.isNotBlank()) {
                IconButton(onClick = { onCityChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Temizle")
                }
            } else {
                IconButton(onClick = { /* GPS location */ }) {
                    Icon(Icons.Outlined.MyLocation, contentDescription = "Konum")
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SealoraPrimary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    )
}

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ErrorColor.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = ErrorColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Hata",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ErrorColor
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tekrar Dene")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatBottomSheet(
    messages: List<AiChatMessage>,
    inputText: String,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onSend: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var localInput by remember { mutableStateOf(inputText) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🤖", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sealora AI",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat")
                }
            }

            Divider()

            // Messages
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("💬", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "AI ile sohbet et",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Hava durumu hakkında sorular sorabilirsiniz.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                items(messages.size) { index ->
                    val msg = messages[index]
                    ChatBubble(message = msg)
                }

                if (isLoading) {
                    item {
                        LoadingChatBubble()
                    }
                }
            }

            // Input
            Divider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = localInput,
                    onValueChange = { localInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Hava durumu hakkında sor...") },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = false,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (localInput.isNotBlank()) {
                                onSend(localInput)
                                localInput = ""
                            }
                        }
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = {
                        if (localInput.isNotBlank()) {
                            onSend(localInput)
                            localInput = ""
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = SealoraPrimary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Gönder", modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ChatBubble(message: AiChatMessage) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SealoraPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🤖", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    SealoraPrimary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
