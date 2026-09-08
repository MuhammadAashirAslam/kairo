package com.example.kairo.presentation.ingestion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kairo.ui.theme.Cyan400
import com.example.kairo.ui.theme.Emerald400
import com.example.kairo.ui.theme.Indigo500

enum class IngestionStage {
    IDLE,
    READING,
    PARSING_STRUCTURE,
    INDEXING_BM25,
    EMBEDDING,
    COMPLETED,
    ERROR
}

data class IngestionState(
    val stage: IngestionStage = IngestionStage.IDLE,
    val fileName: String = "",
    val fileSize: Long = 0L,
    val currentStep: Int = 0,
    val totalSteps: Int = 4,
    val statusMessage: String = "",
    val errorMessage: String? = null,
    val chunkCount: Int = 0,
    val headingCount: Int = 0
)

@Composable
fun IngestionSheet(
    state: IngestionState,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Production Ingestion Pipeline",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = state.fileName.ifEmpty { "Loading..." },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (state.stage == IngestionStage.COMPLETED || state.stage == IngestionStage.ERROR) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Step Progress Checklist (Production 4-Stage Pipeline)
            StepItem(
                title = "Extract & decode document text",
                isActive = state.stage == IngestionStage.READING,
                isCompleted = state.stage.ordinal > IngestionStage.READING.ordinal
            )
            Spacer(modifier = Modifier.height(12.dp))
            StepItem(
                title = "Hierarchical parsing & contextual breadcrumbs",
                isActive = state.stage == IngestionStage.PARSING_STRUCTURE,
                isCompleted = state.stage.ordinal > IngestionStage.PARSING_STRUCTURE.ordinal
            )
            Spacer(modifier = Modifier.height(12.dp))
            StepItem(
                title = "Construct in-memory Okapi BM25 lexical index",
                isActive = state.stage == IngestionStage.INDEXING_BM25,
                isCompleted = state.stage.ordinal > IngestionStage.INDEXING_BM25.ordinal
            )
            Spacer(modifier = Modifier.height(12.dp))
            StepItem(
                title = "Generate dense vector embeddings & L2 norms",
                isActive = state.stage == IngestionStage.EMBEDDING,
                isCompleted = state.stage.ordinal > IngestionStage.EMBEDDING.ordinal
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Progress Bar & Percentage
            val progressFraction = if (state.totalSteps > 0) {
                state.currentStep.toFloat() / state.totalSteps.toFloat()
            } else if (state.stage == IngestionStage.COMPLETED) {
                1.0f
            } else {
                0.0f
            }

            if (state.stage != IngestionStage.COMPLETED && state.stage != IngestionStage.ERROR) {
                if (state.totalSteps > 0) {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Indigo500,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Indigo500,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status message
            Text(
                text = state.statusMessage,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = if (state.stage == IngestionStage.ERROR) {
                        MaterialTheme.colorScheme.error
                    } else if (state.stage == IngestionStage.COMPLETED) {
                        Emerald400
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Actions
            if (state.stage != IngestionStage.COMPLETED && state.stage != IngestionStage.ERROR) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel Ingestion")
                }
            }
        }
    }
}

@Composable
private fun StepItem(
    title: String,
    isActive: Boolean,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCompleted -> Emerald400
                        isActive -> Indigo500
                        else -> MaterialTheme.colorScheme.surface
                    }
                )
                .border(
                    width = 1.dp,
                    color = when {
                        isCompleted -> Emerald400
                        isActive -> Indigo500
                        else -> MaterialTheme.colorScheme.outlineVariant
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                isCompleted -> Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.Black
                )
                isActive -> CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isActive || isCompleted) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isActive || isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}
