package com.example.kairo.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kairo.KairoApp
import com.example.kairo.data.SystemPersona
import com.example.kairo.ui.theme.*
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.api.models
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onDocumentCleared: () -> Unit,
    onChatCleared: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val preferences = KairoApp.instance.preferences
    val chunkStore = KairoApp.instance.chunkStore
    val metadata = chunkStore.getDocumentMetadata()

    // Persistent Settings State
    var userName by remember { mutableStateOf(preferences.userName) }
    var userRole by remember { mutableStateOf(preferences.userRole) }
    var selectedPersona by remember { mutableStateOf(preferences.systemPersona) }
    var customPrompt by remember { mutableStateOf(preferences.customSystemPrompt) }
    var temperature by remember { mutableFloatStateOf(preferences.temperature) }
    var selectedTopK by remember { mutableIntStateOf(preferences.topKRetrieval) }
    var selectedMaxTokens by remember { mutableIntStateOf(preferences.maxOutputTokens) }
    var similarityThreshold by remember { mutableFloatStateOf(preferences.similarityThreshold) }

    var storageUsedBytes by remember { mutableLongStateOf(0L) }
    var storageFreeBytes by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val state = RunAnywhere.models.state()
                storageUsedBytes = state.storageUsedBytes
                storageFreeBytes = state.storageFreeBytes
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & Configuration",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Profile & Identity Section
            Text(
                text = "Profile & Identity",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = OpenAIGreen
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(OpenAIGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = preferences.getUserInitials(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        Column {
                            Text(
                                text = userName.ifBlank { "User" },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            Text(
                                text = userRole.ifBlank { "Local AI Explorer" },
                                style = MaterialTheme.typography.bodySmall,
                                color = OpenAIGreen
                            )
                        }
                    }

                    OutlinedTextField(
                        value = userName,
                        onValueChange = {
                            userName = it
                            preferences.userName = it
                        },
                        label = { Text("Display Name") },
                        placeholder = { Text("e.g. Dr. Jane Doe or Alex") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = OpenAIGreen)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OpenAIGreen,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkSurfaceElevated,
                            unfocusedContainerColor = DarkSurfaceElevated
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = userRole,
                        onValueChange = {
                            userRole = it
                            preferences.userRole = it
                        },
                        label = { Text("Role or Specialty") },
                        placeholder = { Text("e.g. Physician • Clinical Consultation") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = OpenAIGreen)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OpenAIGreen,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkSurfaceElevated,
                            unfocusedContainerColor = DarkSurfaceElevated
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 2. System Persona & Consultation Engine
            Text(
                text = "System Persona & Workflow Harness",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = OpenAIGreen
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Select a persona to optimize reasoning, prompt grounding, and response styling for your use case.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SystemPersona.entries.forEach { persona ->
                            val isSelected = selectedPersona == persona
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedPersona = persona
                                    preferences.systemPersona = persona
                                },
                                leadingIcon = {
                                    val icon = when (persona) {
                                        SystemPersona.GENERAL -> Icons.Default.AutoAwesome
                                        SystemPersona.CLINICAL -> Icons.Default.LocalHospital
                                        SystemPersona.CODING -> Icons.Default.Code
                                        SystemPersona.RESEARCH -> Icons.Default.Science
                                        SystemPersona.CUSTOM -> Icons.Default.Psychology
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text(persona.displayName, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OpenAIGreen,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White,
                                    containerColor = DarkSurfaceElevated,
                                    labelColor = TextPrimary
                                )
                            )
                        }
                    }

                    // Persona description pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceElevated)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = selectedPersona.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }

                    // Custom prompt editor
                    if (selectedPersona == SystemPersona.CUSTOM) {
                        OutlinedTextField(
                            value = customPrompt,
                            onValueChange = {
                                customPrompt = it
                                preferences.customSystemPrompt = it
                            },
                            label = { Text("Custom System Instructions") },
                            placeholder = { Text("Define how Kairo should behave, tone, citation format, and reasoning rules...") },
                            minLines = 3,
                            maxLines = 6,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OpenAIGreen,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkSurfaceElevated,
                                unfocusedContainerColor = DarkSurfaceElevated
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 3. Inference Hyperparameters
            Text(
                text = "Inference & Model Parameters",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = OpenAIGreen
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = OpenAIGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Temperature", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                        Text(
                            text = String.format("%.2f", temperature),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = OpenAIGreen
                        )
                    }

                    val tempLabel = when {
                        temperature <= 0.25f -> "Deterministic & Strict (Ideal for Clinical & Code)"
                        temperature <= 0.65f -> "Balanced & Grounded (Recommended)"
                        else -> "Creative & Exploratory"
                    }

                    Text(text = tempLabel, style = MaterialTheme.typography.bodySmall, color = TextSecondary)

                    Slider(
                        value = temperature,
                        onValueChange = {
                            temperature = (it * 20).roundToInt() / 20f
                            preferences.temperature = temperature
                        },
                        valueRange = 0.0f..1.0f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = OpenAIGreen,
                            activeTrackColor = OpenAIGreen,
                            inactiveTrackColor = DarkSurfaceElevated
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Max Output Tokens", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    Text(
                        text = "Token limit per response. Higher values prevent code and detailed analyses from cutting off.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(256, 512, 1024, 2048, 4096).forEach { tokens ->
                            FilterChip(
                                selected = selectedMaxTokens == tokens,
                                onClick = {
                                    selectedMaxTokens = tokens
                                    preferences.maxOutputTokens = tokens
                                },
                                label = { Text("$tokens") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OpenAIGreen,
                                    selectedLabelColor = Color.White,
                                    containerColor = DarkSurfaceElevated,
                                    labelColor = TextPrimary
                                )
                            )
                        }
                    }
                }
            }

            // 4. Document & RAG Retrieval
            Text(
                text = "RAG & Grounding Engine",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = OpenAIGreen
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = OpenAIGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = metadata?.name ?: "No document currently loaded",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                    }

                    if (metadata != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Indexed chunks: ${metadata.chunkCount} • Characters: ${metadata.characterCount} (~${metadata.sizeBytes / 1024} KB)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                chunkStore.clear()
                                onDocumentCleared()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose500)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear Document Index")
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Top-K Retrieved Context Chunks", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    Text(
                        text = "Number of relevant chunks passed to the LLM prompt for factual citations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 3, 5, 8).forEach { k ->
                            FilterChip(
                                selected = selectedTopK == k,
                                onClick = {
                                    selectedTopK = k
                                    preferences.topKRetrieval = k
                                },
                                label = { Text("k = $k") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OpenAIGreen,
                                    selectedLabelColor = Color.White,
                                    containerColor = DarkSurfaceElevated,
                                    labelColor = TextPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Hybrid Retrieval Similarity Threshold", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.50f, 0.65f, 0.75f, 0.85f).forEach { thresh ->
                            FilterChip(
                                selected = similarityThreshold == thresh,
                                onClick = {
                                    similarityThreshold = thresh
                                    preferences.similarityThreshold = thresh
                                },
                                label = { Text("${(thresh * 100).toInt()}%") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OpenAIGreen,
                                    selectedLabelColor = Color.White,
                                    containerColor = DarkSurfaceElevated,
                                    labelColor = TextPrimary
                                )
                            )
                        }
                    }
                }
            }

            // 5. Device Storage & Chat Management
            Text(
                text = "Device Storage & Data Management",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = OpenAIGreen
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = OpenAIGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Model Cache & Free Space", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Downloaded Models: ${storageUsedBytes / (1024 * 1024)} MB  •  Free Device Storage: ${storageFreeBytes / (1024 * 1024 * 1024)} GB",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            KairoApp.instance.conversationStore.clear()
                            onChatCleared()
                        }
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear Chat History")
                    }
                }
            }

            // 6. Open Source & License
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = OpenAIGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kairo Open Source Project", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Kairo is an open-source, on-device Retrieval-Augmented Generation (RAG) assistant running locally on Android. All embeddings, hybrid search, and LLM inference operate 100% offline with zero external server dependencies.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Version 1.0.0 • Licensed under the Apache License, Version 2.0\nPowered by RunAnywhere SDK, LlamaCPP, and Android Jetpack Compose.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
