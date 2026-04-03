package com.omersusin.sealora.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.omersusin.sealora.domain.model.*
import com.omersusin.sealora.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Snackbar for messages
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar("Hata: $it")
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Ayarlar",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // AI Section
            item {
                SectionHeader(
                    icon = "🤖",
                    title = "Yapay Zeka",
                    subtitle = "Rapor ve sohbet için AI yapılandırması"
                )
            }

            // Active AI Config
            if (uiState.activeAiProvider != null) {
                val activeConfig = uiState.aiConfigs.find { it.isActive }
                item {
                    ActiveAiCard(
                        provider = uiState.activeAiProvider!!,
                        model = activeConfig?.model ?: "",
                        onEdit = { viewModel.showAiConfigDialog(true) }
                    )
                }
            } else {
                item {
                    Card(
                        onClick = { viewModel.showAiConfigDialog(true) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = SealoraPrimary.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Add,
                                contentDescription = null,
                                tint = SealoraPrimary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "AI Sağlayıcısı Ekle",
                                style = MaterialTheme.typography.titleSmall,
                                color = SealoraPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // All AI configs
            if (uiState.aiConfigs.size > 1) {
                item {
                    Text(
                        "Diğer Yapılandırmalar",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(uiState.aiConfigs.filter { !it.isActive }) { config ->
                    AiConfigItem(
                        config = config,
                        onActivate = { viewModel.activateAiProvider(config.provider) },
                        onDelete = { viewModel.deleteAiConfig(config.provider) }
                    )
                }
            }

            // Weather Providers Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    icon = "🌤️",
                    title = "Hava Durumu Sağlayıcıları",
                    subtitle = "${uiState.providers.count { it.isActive }} aktif"
                )
            }

            items(uiState.providers) { provider ->
                ProviderSettingsCard(
                    provider = provider,
                    onToggle = { viewModel.toggleProvider(provider.id, it) },
                    onDelete = {
                        if (!provider.isBuiltIn) {
                            viewModel.deleteProvider(provider.id)
                        }
                    }
                )
            }

            // Add Provider Button
            item {
                OutlinedButton(
                    onClick = { viewModel.showAddProviderDialog(true) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Yeni Sağlayıcı Ekle")
                }
            }

            // About Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(
                    icon = "ℹ️",
                    title = "Hakkında",
                    subtitle = ""
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sealora v1.0.0", fontWeight = FontWeight.Bold)
                        Text(
                            "Çoklu hava durumu sağlayıcısı ve AI destekli rapor uygulaması.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Geliştirici: omersusin",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }

    // Add Provider Dialog
    if (uiState.showProviderUrlDialog) {
        AddProviderDialog(
            url = uiState.newProviderUrl,
            name = uiState.newProviderName,
            isDiscovering = uiState.isDiscoveringUrl,
            discoveredTemplate = uiState.discoveredTemplate,
            onUrlChange = { viewModel.updateField(SettingsField.NEW_PROVIDER_URL, it) },
            onNameChange = { viewModel.updateField(SettingsField.NEW_PROVIDER_NAME, it) },
            onDiscover = { viewModel.discoverUrlWithAi() },
            onAdd = { viewModel.addProviderFromUrl() },
            onAddFromTemplate = { viewModel.addProviderFromTemplate() },
            onDismiss = { viewModel.showAddProviderDialog(false) }
        )
    }

    // AI Config Dialog
    if (uiState.showAiConfigDialog) {
        AiConfigDialog(
            selectedProvider = uiState.selectedAiProvider,
            apiKey = uiState.newAiApiKey,
            model = uiState.newAiModel,
            baseUrl = uiState.newAiBaseUrl,
            availableModels = uiState.selectedAiProvider.defaultModels(),
            onProviderSelect = { viewModel.selectAiProvider(it) },
            onApiKeyChange = { viewModel.updateField(SettingsField.AI_API_KEY, it) },
            onModelChange = { viewModel.updateField(SettingsField.AI_MODEL, it) },
            onBaseUrlChange = { viewModel.updateField(SettingsField.AI_BASE_URL, it) },
            onSave = { viewModel.saveAiConfig() },
            onDismiss = { viewModel.showAiConfigDialog(false) }
        )
    }
}

@Composable
private fun SectionHeader(icon: String, title: String, subtitle: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        Text(text = icon, fontSize = 24.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ActiveAiCard(
    provider: AiProvider,
    model: String,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SealoraPrimary.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = provider.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AssistChip(
                        onClick = {},
                        label = { Text("Aktif", style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = SuccessColor.copy(alpha = 0.15f),
                            labelColor = SuccessColor
                        )
                    )
                }
                if (model.isNotBlank()) {
                    Text(
                        text = model,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = "Düzenle")
            }
        }
    }
}

@Composable
private fun AiConfigItem(
    config: AiConfig,
    onActivate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = config.provider.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = config.model.ifBlank { "Varsayılan model" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onActivate) {
                Text("Aktifleştir")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Sil", tint = ErrorColor)
            }
        }
    }
}

@Composable
private fun ProviderSettingsCard(
    provider: WeatherProvider,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = provider.isActive,
                onCheckedChange = onToggle
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = provider.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (provider.isBuiltIn) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("⭐", fontSize = 12.sp)
                    }
                }
                Text(
                    text = when (provider.type) {
                        ProviderType.BUILTIN -> "Yerleşik"
                        ProviderType.API -> if (provider.apiKey.isNotBlank()) "API ✓" else "API Key gerekli"
                        ProviderType.SCRAPED -> "Kullanıcı ekledi"
                        ProviderType.AI_DISCOVERED -> "AI keşfetti"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!provider.isBuiltIn) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Sil", tint = ErrorColor)
                }
            }
        }
    }
}

@Composable
private fun AddProviderDialog(
    url: String,
    name: String,
    isDiscovering: Boolean,
    discoveredTemplate: ProviderTemplate?,
    onUrlChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onDiscover: () -> Unit,
    onAdd: () -> Unit,
    onAddFromTemplate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = SealoraPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sağlayıcı Ekle")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("İsim (opsiyonel)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    label = { Text("URL") },
                    placeholder = { Text("https://havadurumu.com/{city}") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(
                    text = "{city} yer tutucusu kullanabilirsiniz",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // AI Discover Button
                OutlinedButton(
                    onClick = onDiscover,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = url.isNotBlank() && !isDiscovering
                ) {
                    if (isDiscovering) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI ile Keşfet")
                }

                // Discovered Template
                if (discoveredTemplate != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = SuccessColor.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "✅ Şablon keşfedildi",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = SuccessColor
                            )
                            Text(
                                "Güven: ${(discoveredTemplate.confidence * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onAddFromTemplate,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Bu Şablonu Kullan")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAdd,
                enabled = url.isNotBlank()
            ) {
                Text("Ekle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@Composable
private fun AiConfigDialog(
    selectedProvider: AiProvider,
    apiKey: String,
    model: String,
    baseUrl: String,
    availableModels: List<String>,
    onProviderSelect: (AiProvider) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = SealoraPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Yapılandırması")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Provider Selection
                Text("Sağlayıcı", style = MaterialTheme.typography.labelLarge)
                AiProvider.values().forEach { provider ->
                    Card(
                        onClick = { onProviderSelect(provider) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedProvider == provider)
                                SealoraPrimary.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedProvider == provider,
                                onClick = { onProviderSelect(provider) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = provider.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = onApiKeyChange,
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                // Model Selection
                Text("Model", style = MaterialTheme.typography.labelLarge)
                availableModels.take(4).forEach { m ->
                    Card(
                        onClick = { onModelChange(m) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (model == m)
                                SealoraPrimary.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = m,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                OutlinedTextField(
                    value = model,
                    onValueChange = onModelChange,
                    label = { Text("Özel model adı") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = onBaseUrlChange,
                    label = { Text("Base URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = apiKey.isNotBlank()
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}
