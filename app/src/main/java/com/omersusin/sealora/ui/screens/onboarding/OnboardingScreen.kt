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

@Composable
fun OnboardingScreen(onComplete: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var step by remember { mutableIntStateOf(0) }
    var localKey by remember { mutableStateOf("") }
    var localModel by remember { mutableStateOf("") }
    var localProvider by remember { mutableStateOf(AiProvider.OPENROUTER) }
    var saved by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(SealoraPrimary, SealoraPrimaryDark)))) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp).statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            // Progress
            LinearProgressIndicator(
                progress = (step + 1).toFloat() / 4,
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = Color.White, trackColor = Color.White.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(32.dp))

            val titles = listOf("Hos Geldin", "Yapay Zeka", "Saglayicilar", "Hazir!")
            Text(titles[step], style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(32.dp))

            // Content
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (step) {
                    0 -> WelcomeContent()
                    1 -> AiStepContent(
                        provider = localProvider, apiKey = localKey, model = localModel,
                        onProvider = { localProvider = it; localModel = "" },
                        onKey = { localKey = it }, onModel = { localModel = it }
                    )
                    2 -> ProviderStepContent(uiState = uiState, viewModel = viewModel)
                    3 -> ReadyContent()
                }
            }

            // Buttons
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (step > 0 && step != 1) {
                    OutlinedButton(onClick = { step-- }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null); Spacer(Modifier.width(8.dp)); Text("Geri")
                    }
                } else { Spacer(Modifier.width(1.dp)) }

                when (step) {
                    1 -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { step = 2 }) { Text("Atla", color = Color.White.copy(alpha = 0.7f)) }
                            Button(
                                onClick = {
                                    if (localKey.isNotBlank()) {
                                        viewModel.selectAiProvider(localProvider)
                                        viewModel.updateField(SettingsField.AI_API_KEY, localKey)
                                        viewModel.updateField(SettingsField.AI_MODEL, localModel.ifBlank { localProvider.defaultModels().firstOrNull() ?: "" })
                                        viewModel.saveAiConfig()
                                        saved = true
                                    }
                                    step = 2
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = SealoraPrimary)
                            ) {
                                Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("Kaydet")
                            }
                        }
                    }
                    else -> {
                        Button(onClick = { if (step < 3) step++ else onComplete() }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = SealoraPrimary)) {
                            Text(if (step == 3) "Baslayalim" else "Devam")
                            Spacer(Modifier.width(8.dp))
                            Icon(if (step == 3) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward, null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("\uD83C\uDF0A", fontSize = 80.sp)
        Spacer(Modifier.height(16.dp))
        Text("Coklu saglayicili, yapay zeka destekli\nakilli hava durumu uygulamasi", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            FeatureItem("\u2600\uFE0F", "Coklu\nSaglayici")
            FeatureItem("\uD83E\uDD16", "AI\nRapor")
            FeatureItem("\uD83D\uDCAC", "AI\nSohbet")
        }
    }
}

@Composable
private fun FeatureItem(emoji: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp)).padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(emoji, fontSize = 36.sp); Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AiStepContent(provider: AiProvider, apiKey: String, model: String, onProvider: (AiProvider) -> Unit, onKey: (String) -> Unit, onModel: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("AI saglayicisini secin", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(AiProvider.entries) { p ->
                val active = provider == p
                Card(onClick = { onProvider(p) }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (active) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)), shape = RoundedCornerShape(14.dp)) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = active, onClick = { onProvider(p) }, colors = RadioButtonDefaults.colors(selectedColor = Color.White, unselectedColor = Color.White.copy(alpha = 0.5f)))
                        Spacer(Modifier.width(10.dp))
                        Column { Text(p.displayName, color = Color.White, fontWeight = FontWeight.SemiBold); Text(p.defaultBaseUrl, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = apiKey, onValueChange = onKey, label = { Text("API Key", color = Color.White.copy(alpha = 0.7f)) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.White, unfocusedBorderColor = Color.White.copy(alpha = 0.4f), focusedTextColor = Color.White, unfocusedTextColor = Color.White), singleLine = true)
                Spacer(Modifier.height(8.dp))
            }

            item {
                Text("Model", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
                Spacer(Modifier.height(4.dp))
            }

            items(provider.defaultModels().take(4)) { m ->
                Card(onClick = { onModel(m) }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (model == m) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f)), shape = RoundedCornerShape(10.dp)) {
                    Text(m, modifier = Modifier.padding(12.dp), color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ProviderStepContent(uiState: com.omersusin.sealora.ui.screens.settings.SettingsUiState, viewModel: SettingsViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Open-Meteo varsayilan olarak aktif", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.providers) { p ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (p.isActive) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f)), shape = RoundedCornerShape(14.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = p.isActive, onCheckedChange = { viewModel.toggleProvider(p.id, it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SealoraSecondary))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(p.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                            if (p.requiresApiKey) Text("API Key gerekli", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                        }
                        if (p.isBuiltIn) Text("\u2B50", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadyContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("\uD83C\uDF89", fontSize = 80.sp)
        Spacer(Modifier.height(16.dp))
        Text("Hazirsin!", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Sehrini ara ve hava durumunu ogren", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center)
    }
}
