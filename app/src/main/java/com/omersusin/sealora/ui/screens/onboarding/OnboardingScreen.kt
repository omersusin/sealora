package com.omersusin.sealora.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.omersusin.sealora.domain.model.*
import com.omersusin.sealora.ui.screens.settings.SettingsField
import com.omersusin.sealora.ui.screens.settings.SettingsViewModel
import com.omersusin.sealora.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentStep by remember { mutableIntStateOf(0) }
    val steps = listOf("Hos Geldin", "Yapay Zeka", "Saglayicilar", "Hazir!")
    var aiConfigSaved by remember { mutableStateOf(false) }
    var localApiKey by remember { mutableStateOf("") }
    var localModel by remember { mutableStateOf("") }
    var localProvider by remember { mutableStateOf(AiProvider.OPENROUTER) }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(SealoraPrimary, SealoraPrimaryDark))).padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = (currentStep + 1).toFloat() / steps.size,
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(24.dp))
            Text(steps[currentStep], style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(32.dp))

            when (currentStep) {
                0 -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("\uD83C\uDF0A", fontSize = 80.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("Sealora'ya Hos Geldin", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text("Birden fazla hava durumu saglayicisindan veri toplayan, yapay zeka destekli akilli hava durumu uygulamasi.", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center)
                        Spacer(Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            listOf("Coklu\nSaglayici", "AI\nRapor", "AI\nSohbet").forEach { label ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)).padding(16.dp)) {
                                    Text(listOf("\uD83C\uDF24\uFE0F", "\uD83E\uDD16", "\uD83D\uDCAC")[listOf("Coklu\nSaglayici", "AI\nRapor", "AI\nSohbet").indexOf(label)], fontSize = 32.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
                1 -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Yapay zeka rapor icin bir AI saglayici secin.", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center)
                        Spacer(Modifier.height(24.dp))
                        AiProvider.entries.forEach { provider ->
                            Card(
                                onClick = { localProvider = provider; localModel = "" },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = if (localProvider == provider) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = localProvider == provider, onClick = { localProvider = provider; localModel = "" }, colors = RadioButtonDefaults.colors(selectedColor = Color.White, unselectedColor = Color.White.copy(alpha = 0.6f)))
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(provider.displayName, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(provider.defaultBaseUrl, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(value = localApiKey, onValueChange = { localApiKey = it }, label = { Text("API Key", color = Color.White.copy(alpha = 0.7f)) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.White, unfocusedBorderColor = Color.White.copy(alpha = 0.5f), focusedTextColor = Color.White, unfocusedTextColor = Color.White), singleLine = true, leadingIcon = { Icon(Icons.Outlined.Key, null, tint = Color.White.copy(alpha = 0.7f)) })
                        Spacer(Modifier.height(8.dp))
                        Text("Model Secin", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
                        Spacer(Modifier.height(4.dp))
                        localProvider.defaultModels().take(4).forEach { model ->
                            Card(
                                onClick = { localModel = model },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                colors = CardDefaults.cardColors(containerColor = if (localModel == model) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text(model, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = Color.White) }
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (localApiKey.isNotBlank()) {
                                    viewModel.selectAiProvider(localProvider)
                                    viewModel.updateField(SettingsField.AI_API_KEY, localApiKey)
                                    viewModel.updateField(SettingsField.AI_MODEL, localModel.ifBlank { localProvider.defaultModels().firstOrNull() ?: "" })
                                    viewModel.saveAiConfig()
                                    aiConfigSaved = true
                                    currentStep++
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = SealoraPrimary),
                            enabled = localApiKey.isNotBlank()
                        ) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("Kaydet ve Devam Et") }
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { currentStep++ }, modifier = Modifier.fillMaxWidth()) { Text("Simdilik gec", color = Color.White.copy(alpha = 0.7f)) }
                    }
                }
                2 -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Hava durumu saglayicilarinizi yonetin. Open-Meteo varsayilan olarak aktiftir.", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(uiState.providers) { provider ->
                                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (provider.isActive) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.1f)), shape = RoundedCornerShape(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Switch(checked = provider.isActive, onCheckedChange = { viewModel.toggleProvider(provider.id, it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SealoraSecondary))
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(provider.name, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                                            if (provider.requiresApiKey) Text("API Key gerekli", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                        }
                                        if (provider.isBuiltIn) Text("\u2B50", fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.showAddProviderDialog(true) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Yeni Saglayici Ekle") }
                    }
                }
                3 -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("\uD83C\uDF89", fontSize = 80.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("Hazirsin!", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text("Sealora kullanima hazir. Sehrini ara ve hava durumunu ogren!", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (currentStep > 0 && currentStep != 1) {
                    OutlinedButton(onClick = { currentStep-- }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null); Spacer(Modifier.width(8.dp)); Text("Geri") }
                } else { Spacer(Modifier.width(1.dp)) }

                if (currentStep != 1) {
                    Button(onClick = { if (currentStep < steps.size - 1) currentStep++ else onComplete() }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = SealoraPrimary)) {
                        Text(if (currentStep == steps.size - 1) "Baslayalim" else "Devam")
                        Spacer(Modifier.width(8.dp))
                        Icon(if (currentStep == steps.size - 1) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward, null)
                    }
                }
            }
        }
    }
}
