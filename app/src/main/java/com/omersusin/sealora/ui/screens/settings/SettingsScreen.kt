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
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) { uiState.successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() } }
    LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar("Hata: $it"); viewModel.clearMessages() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayarlar", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("\uD83E\uDD16", fontSize = 24.sp); Spacer(Modifier.width(8.dp))
                    Column { Text("Yapay Zeka", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("Rapor ve sohbet icin AI yapilandirmasi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }

            if (uiState.activeAiProvider != null) {
                item {
                    val activeConfig = uiState.aiConfigs.find { it.isActive }
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SealoraPrimary.copy(alpha = 0.08f)), shape = RoundedCornerShape(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(uiState.activeAiProvider!!.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(8.dp))
                                    AssistChip(onClick = {}, label = { Text("Aktif", style = MaterialTheme.typography.labelSmall) }, colors = AssistChipDefaults.assistChipColors(containerColor = SuccessColor.copy(alpha = 0.15f), labelColor = SuccessColor))
                                }
                                if (activeConfig?.model?.isNotBlank() == true) Text(activeConfig.model, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { viewModel.showAiConfigDialog(true) }) { Icon(Icons.Outlined.Edit, "Duzenle") }
                        }
                    }
                }
            } else {
                item {
                    Card(onClick = { viewModel.showAiConfigDialog(true) }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SealoraPrimary.copy(alpha = 0.1f)), shape = RoundedCornerShape(16.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Add, null, tint = SealoraPrimary); Spacer(Modifier.width(12.dp)); Text("AI Saglayicisi Ekle", style = MaterialTheme.typography.titleSmall, color = SealoraPrimary, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("\u2600\uFE0F", fontSize = 24.sp); Spacer(Modifier.width(8.dp))
                    Column { Text("Hava Durumu Saglayicilari", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("${uiState.providers.count { it.isActive }} aktif", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }

            items(uiState.providers) { provider ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = provider.isActive, onCheckedChange = { viewModel.toggleProvider(provider.id, it) })
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(provider.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                if (provider.isBuiltIn) { Spacer(Modifier.width(4.dp)); Text("\u2B50", fontSize = 12.sp) }
                            }
                            Text(when (provider.type) { ProviderType.BUILTIN -> "Yerlesik"; ProviderType.API -> if (provider.apiKey.isNotBlank()) "API \u2713" else "API Key gerekli"; ProviderType.SCRAPED -> "Kullanici ekledi"; ProviderType.AI_DISCOVERED -> "AI kesfetti" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (!provider.isBuiltIn) IconButton(onClick = { viewModel.deleteProvider(provider.id) }) { Icon(Icons.Outlined.Delete, "Sil", tint = ErrorColor) }
                    }
                }
            }

            item {
                OutlinedButton(onClick = { viewModel.showAddProviderDialog(true) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Yeni Saglayici Ekle") }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) { Text("Sealora v1.0.3", fontWeight = FontWeight.Bold); Text("Coklu hava durumu saglayicisi ve AI destekli rapor uygulamasi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(8.dp)); Text("Gelistirici: omersusin", style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
    }

    if (uiState.showProviderUrlDialog) {
        var provUrl by remember { mutableStateOf("") }
        var provName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.showAddProviderDialog(false) },
            title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Add, null, tint = SealoraPrimary); Spacer(Modifier.width(8.dp)); Text("Saglayici Ekle") } },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = provName, onValueChange = { provName = it }, label = { Text("Isim (opsiyonel)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = provUrl, onValueChange = { provUrl = it }, label = { Text("URL") }, placeholder = { Text("https://havadurumu.com/{city}") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Text("{city} yer tutucusu kullanabilirsiniz", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (provUrl.isNotBlank()) {
                        viewModel.updateField(SettingsField.NEW_PROVIDER_URL, provUrl)
                        viewModel.updateField(SettingsField.NEW_PROVIDER_NAME, provName)
                        viewModel.addProviderFromUrl()
                        provUrl = ""; provName = ""
                    }
                }, enabled = provUrl.isNotBlank()) { Text("Ekle") }
            },
            dismissButton = { TextButton(onClick = { viewModel.showAddProviderDialog(false) }) { Text("Iptal") } }
        )
    }

    if (uiState.showAiConfigDialog) {
        var aiKey by remember { mutableStateOf("") }
        var aiModel by remember { mutableStateOf("") }
        var aiProvider by remember { mutableStateOf(AiProvider.OPENROUTER) }
        AlertDialog(
            onDismissRequest = { viewModel.showAiConfigDialog(false) },
            title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.AutoAwesome, null, tint = SealoraPrimary); Spacer(Modifier.width(8.dp)); Text("AI Yapilandirmasi") } },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Saglayici", style = MaterialTheme.typography.labelLarge)
                    AiProvider.entries.forEach { provider ->
                        Card(onClick = { aiProvider = provider; aiModel = ""; viewModel.selectAiProvider(provider) }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (aiProvider == provider) SealoraPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(8.dp)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = aiProvider == provider, onClick = { aiProvider = provider; aiModel = ""; viewModel.selectAiProvider(provider) })
                                Spacer(Modifier.width(8.dp))
                                Text(provider.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(value = aiKey, onValueChange = { aiKey = it }, label = { Text("API Key") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                    Text("Model", style = MaterialTheme.typography.labelLarge)
                    aiProvider.defaultModels().take(4).forEach { m ->
                        Card(onClick = { aiModel = m; viewModel.updateField(SettingsField.AI_MODEL, m) }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (aiModel == m) SealoraPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
                            Text(m, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    OutlinedTextField(value = aiModel, onValueChange = { aiModel = it }, label = { Text("Ozel model adi") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (aiKey.isNotBlank()) {
                        viewModel.updateField(SettingsField.AI_API_KEY, aiKey)
                        viewModel.updateField(SettingsField.AI_MODEL, aiModel.ifBlank { aiProvider.defaultModels().firstOrNull() ?: "" })
                        viewModel.saveAiConfig()
                        aiKey = ""; aiModel = ""
                    }
                }, enabled = aiKey.isNotBlank()) { Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = { viewModel.showAiConfigDialog(false) }) { Text("Iptal") } }
        )
    }
}
