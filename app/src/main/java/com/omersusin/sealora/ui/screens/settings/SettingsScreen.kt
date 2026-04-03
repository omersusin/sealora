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
fun SettingsScreen(onNavigateBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showProviderDialog by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.successMessage) { uiState.successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() } }
    LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar("Hata: $it"); viewModel.clearMessages() } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Ayarlar", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") } }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { SettingsSectionHeader("\uD83E\uDD16", "Yapay Zeka", "Rapor ve sohbet icin AI") }

            if (uiState.activeAiProvider != null) {
                item {
                    val cfg = uiState.aiConfigs.find { it.isActive }
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SealoraPrimary.copy(alpha = 0.08f)), shape = RoundedCornerShape(16.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(uiState.activeAiProvider!!.displayName, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(8.dp))
                                    AssistChip(onClick = {}, label = { Text("Aktif", style = MaterialTheme.typography.labelSmall) }, colors = AssistChipDefaults.assistChipColors(containerColor = SuccessColor.copy(alpha = 0.15f), labelColor = SuccessColor))
                                }
                                if (cfg?.model?.isNotBlank() == true) Text(cfg.model, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { showAiDialog = true }) { Icon(Icons.Outlined.Edit, "Duzenle") }
                        }
                    }
                }
            } else {
                item {
                    Card(onClick = { showAiDialog = true }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SealoraPrimary.copy(alpha = 0.1f)), shape = RoundedCornerShape(16.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Add, null, tint = SealoraPrimary); Spacer(Modifier.width(12.dp)); Text("AI Saglayicisi Ekle", color = SealoraPrimary, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }

            item { Spacer(Modifier.height(4.dp)); SettingsSectionHeader("\u2600\uFE0F", "Hava Durumu Saglayicilari", "${uiState.providers.count { it.isActive }} aktif") }

            items(uiState.providers) { p ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = p.isActive, onCheckedChange = { viewModel.toggleProvider(p.id, it) })
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) { Text(p.name, fontWeight = FontWeight.SemiBold); if (p.isBuiltIn) { Spacer(Modifier.width(4.dp)); Text("\u2B50", fontSize = 12.sp) } }
                            Text(when (p.type) { ProviderType.BUILTIN -> "Yerlesik"; ProviderType.API -> if (p.apiKey.isNotBlank()) "API \u2713" else "API Key gerekli"; else -> "Kullanici" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (!p.isBuiltIn) IconButton(onClick = { viewModel.deleteProvider(p.id) }) { Icon(Icons.Outlined.Delete, "Sil", tint = ErrorColor) }
                    }
                }
            }

            item { OutlinedButton(onClick = { showProviderDialog = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Yeni Saglayici") } }

            item {
                Spacer(Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) { Text("Sealora v1.0.5", fontWeight = FontWeight.Bold); Text("Coklu saglayici + AI hava durumu", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }

    if (showProviderDialog) {
        var url by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showProviderDialog = false }, title = { Text("Saglayici Ekle") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Isim") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") }, placeholder = { Text("https://site.com/{city}") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("{city} kullanabilirsiniz", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }, confirmButton = { Button(onClick = { if (url.isNotBlank()) { viewModel.updateField(SettingsField.NEW_PROVIDER_URL, url); viewModel.updateField(SettingsField.NEW_PROVIDER_NAME, name); viewModel.addProviderFromUrl(); url = ""; name = ""; showProviderDialog = false } }, enabled = url.isNotBlank()) { Text("Ekle") } }, dismissButton = { TextButton(onClick = { showProviderDialog = false }) { Text("Iptal") } })
    }

    if (showAiDialog) {
        var key by remember { mutableStateOf("") }
        var model by remember { mutableStateOf("") }
        var prov by remember { mutableStateOf(AiProvider.OPENROUTER) }
        AlertDialog(onDismissRequest = { showAiDialog = false }, title = { Text("AI Yapilandirmasi") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AiProvider.entries.forEach { p ->
                    Card(onClick = { prov = p; model = ""; viewModel.selectAiProvider(p) }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (prov == p) SealoraPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(8.dp)) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = prov == p, onClick = { prov = p; model = ""; viewModel.selectAiProvider(p) })
                            Spacer(Modifier.width(8.dp)); Text(p.displayName, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("API Key") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                Text("Model", style = MaterialTheme.typography.labelLarge)
                prov.defaultModels().take(4).forEach { m ->
                    Card(onClick = { model = m; viewModel.updateField(SettingsField.AI_MODEL, m) }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (model == m) SealoraPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) { Text(m, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall) }
                }
            }
        }, confirmButton = { Button(onClick = { if (key.isNotBlank()) { viewModel.updateField(SettingsField.AI_API_KEY, key); viewModel.updateField(SettingsField.AI_MODEL, model.ifBlank { prov.defaultModels().firstOrNull() ?: "" }); viewModel.saveAiConfig(); key = ""; model = ""; showAiDialog = false } }, enabled = key.isNotBlank()) { Text("Kaydet") } }, dismissButton = { TextButton(onClick = { showAiDialog = false }) { Text("Iptal") } })
    }
}

@Composable
private fun SettingsSectionHeader(icon: String, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 22.sp); Spacer(Modifier.width(8.dp))
        Column { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
