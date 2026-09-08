package com.example.kairo.presentation.setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.kairo.ui.theme.Cyan400
import com.example.kairo.ui.theme.Emerald400
import com.example.kairo.ui.theme.Indigo500
import com.example.kairo.ui.theme.Rose500
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.api.DownloadEvent
import com.runanywhere.sdk.public.api.models
import kotlinx.coroutines.launch

data class ModelCardInfo(
    val id: String,
    val name: String,
    val description: String,
    val sizeText: String,
    val isPrimary: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSetupScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    val modelsList = listOf(
        ModelCardInfo(
            id = KairoApp.MODEL_SMOLLM_360M_Q4,
            name = "SmolLM2 360M Instruct (Q4_K_M)",
            description = "Fastest token generation (~12-18 tok/s). Half the memory traffic, lowest battery use.",
            sizeText = "270 MB",
            isPrimary = true
        ),
        ModelCardInfo(
            id = KairoApp.MODEL_SMOLLM_360M,
            name = "SmolLM2 360M Instruct (Q8_0)",
            description = "High precision 8-bit weights. Higher memory bandwidth requirement (~6-8 tok/s).",
            sizeText = "388 MB",
            isPrimary = false
        ),
        ModelCardInfo(
            id = KairoApp.MODEL_LLAMA_1B,
            name = "Llama 3.2 1B Instruct (Q4_K_M)",
            description = "High-quality responses and general reasoning. Balanced mobile footprint (~5-7 tok/s).",
            sizeText = "747 MB",
            isPrimary = false
        ),
        ModelCardInfo(
            id = KairoApp.MODEL_QWEN_CODER_1_5B,
            name = "Qwen 2.5 Coder 1.5B Instruct (Q4_K_M)",
            description = "State-of-the-art coding, technical reasoning, and structured document analysis.",
            sizeText = "1.11 GB",
            isPrimary = false
        )
    )

    val downloadedMap = remember { mutableStateMapOf<String, Boolean>() }
    val downloadingMap = remember { mutableStateMapOf<String, Boolean>() }
    val progressMap = remember { mutableStateMapOf<String, Float>() }
    val statusMap = remember { mutableStateMapOf<String, String>() }

    var selectedModelId by remember { mutableStateOf(KairoApp.selectedModelId) }

    fun refreshModelStatus() {
        scope.launch {
            for (model in modelsList) {
                try {
                    val info = RunAnywhere.models.get(model.id)
                    val isDownloaded = info != null && info.local_path.isNotEmpty()
                    downloadedMap[model.id] = isDownloaded
                } catch (_: Exception) {
                    downloadedMap[model.id] = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshModelStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "On-Device Models",
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Intro Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Indigo500.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = Cyan400,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "100% Offline Inference",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Models are stored in app storage. Once downloaded, no internet connection is ever needed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = "Available Language Models",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Model Cards
            modelsList.forEach { model ->
                val isDownloaded = downloadedMap[model.id] == true
                val isDownloading = downloadingMap[model.id] == true
                val progress = progressMap[model.id] ?: 0.0f
                val status = statusMap[model.id] ?: if (isDownloaded) "Ready on device" else "Not downloaded"
                val isCurrentSelected = selectedModelId == model.id

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrentSelected) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = isDownloaded) {
                            selectedModelId = model.id
                            KairoApp.selectedModelId = model.id
                        }
                        .border(
                            width = if (isCurrentSelected) 2.dp else 1.dp,
                            color = if (isCurrentSelected) Indigo500 else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = model.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (model.isPrimary) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Cyan400.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Default",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Cyan400,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "Size: ${model.sizeText}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }

                            if (isDownloaded) {
                                IconButton(
                                    onClick = {
                                        selectedModelId = model.id
                                        KairoApp.selectedModelId = model.id
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isCurrentSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = "Select",
                                        tint = if (isCurrentSelected) Indigo500 else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = model.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Status Row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isDownloaded) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Emerald400,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = status,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isDownloaded) Emerald400 else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isDownloaded) FontWeight.SemiBold else FontWeight.Normal
                                )
                            )
                        }

                        // Download Progress
                        if (isDownloading) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Indigo500
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isDownloaded) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                RunAnywhere.models.delete(model.id)
                                                downloadedMap[model.id] = false
                                                statusMap[model.id] = "Deleted from storage"
                                                refreshModelStatus()
                                            } catch (e: Exception) {
                                                statusMap[model.id] = "Delete error: ${e.message}"
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Rose500
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        downloadingMap[model.id] = true
                                        statusMap[model.id] = "Starting download..."
                                        progressMap[model.id] = 0f

                                        scope.launch {
                                            try {
                                                RunAnywhere.models.download(model.id).collect { event ->
                                                    when (event) {
                                                        is DownloadEvent.Progress -> {
                                                            val total = event.bytesTotal.coerceAtLeast(1L)
                                                            val fraction = (event.bytesDone.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                                                            progressMap[model.id] = fraction
                                                            val doneMb = event.bytesDone / (1024 * 1024)
                                                            val totalMb = event.bytesTotal / (1024 * 1024)
                                                            statusMap[model.id] = "Downloading: $doneMb MB / $totalMb MB (${(fraction * 100).toInt()}%)"
                                                        }
                                                        is DownloadEvent.Verifying -> {
                                                            statusMap[model.id] = "Verifying model checksum..."
                                                        }
                                                        is DownloadEvent.Completed -> {
                                                            downloadingMap[model.id] = false
                                                            downloadedMap[model.id] = true
                                                            statusMap[model.id] = "Ready on device!"
                                                            selectedModelId = model.id
                                                            KairoApp.selectedModelId = model.id
                                                        }
                                                        is DownloadEvent.Failed -> {
                                                            downloadingMap[model.id] = false
                                                            statusMap[model.id] = "Download failed: ${event.error.message}"
                                                        }
                                                        else -> {}
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                downloadingMap[model.id] = false
                                                statusMap[model.id] = "Error: ${e.message}"
                                            }
                                        }
                                    },
                                    enabled = !isDownloading,
                                    colors = ButtonDefaults.buttonColors(containerColor = Indigo500)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isDownloading) "Downloading..." else "Download Model")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
