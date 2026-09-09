package com.example.kairo.presentation.components

import android.graphics.Bitmap
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.kairo.R
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kairo.data.ChatMessage
import com.example.kairo.data.MessageRole
import com.example.kairo.ui.theme.DarkBorder
import com.example.kairo.ui.theme.DarkBorderSubtle
import com.example.kairo.ui.theme.DarkSurface
import com.example.kairo.ui.theme.DarkSurfaceElevated
import com.example.kairo.ui.theme.OpenAIGreen
import com.example.kairo.ui.theme.TextMuted
import com.example.kairo.ui.theme.TextPrimary
import com.example.kairo.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun ChatBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == MessageRole.USER
    val context = LocalContext.current
    // Decoded off the main thread and downsampled for thumbnail display
    val bitmap: Bitmap? = rememberDownsampledBitmap(context, message.imageUri, maxDimensionPx = 1024)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // Kairo Branded Assistant Avatar
            Image(
                painter = painterResource(id = R.drawable.app_logo_round),
                contentDescription = "Kairo AI",
                modifier = Modifier
                    .padding(end = 10.dp, top = 2.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .border(1.dp, DarkBorder, CircleShape)
            )
        }

        Column(
            modifier = Modifier.widthIn(max = if (isUser) 310.dp else 340.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            if (isUser) {
                // User Message: ChatGPT Solid Neutral Pill (No loud purple gradients)
                Column(horizontalAlignment = Alignment.End) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Attached image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .widthIn(max = 240.dp)
                                .heightIn(max = 160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (message.content.isNotBlank()) {
                        val userClipboard = LocalClipboardManager.current
                        Box(
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 20.dp,
                                        topEnd = 20.dp,
                                        bottomStart = 20.dp,
                                        bottomEnd = 6.dp
                                    )
                                )
                                .clickable {
                                    userClipboard.setText(AnnotatedString(message.content))
                                    Toast.makeText(context, "Copied prompt", Toast.LENGTH_SHORT).show()
                                }
                                .background(DarkSurfaceElevated)
                                .border(1.dp, DarkBorderSubtle, RoundedCornerShape(
                                    topStart = 20.dp,
                                    topEnd = 20.dp,
                                    bottomStart = 20.dp,
                                    bottomEnd = 6.dp
                                ))
                                .padding(horizontal = 16.dp, vertical = 11.dp)
                        ) {
                            Text(
                                text = buildInlineMarkdown(message.content, TextPrimary),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp
                                )
                            )
                        }
                    }
                }
            } else {
                // Assistant Message: Natural, clean, unboxed typography (ChatGPT style)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "Kairo",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                fontSize = 13.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkSurfaceElevated)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "On-Device",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = OpenAIGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    // Content with streaming cursor if generating
                    val infiniteTransition = rememberInfiniteTransition(label = "cursorBlink")
                    val cursorAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.15f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(500),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "cursorAlpha"
                    )

                    val displayText = message.content.ifEmpty {
                        if (message.isGenerating) "Thinking..." else ""
                    }

                    MarkdownText(
                        markdown = displayText,
                        isGenerating = message.isGenerating,
                        cursorModifier = Modifier.alpha(cursorAlpha),
                        textColor = TextPrimary
                    )

                    // Citations section
                    if (message.sources.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        SourceCitationCard(sources = message.sources)
                    }

                    // Action Bar: Copy Button + Metrics Banner
                    var isCopied by remember { mutableStateOf(false) }
                    val clipboardManager = LocalClipboardManager.current
                    LaunchedEffect(isCopied) {
                        if (isCopied) {
                            delay(2000)
                            isCopied = false
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        // Copy Action
                        if (message.content.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString(message.content))
                                        isCopied = true
                                        Toast.makeText(context, "Copied response to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                    .background(DarkSurfaceElevated)
                                    .border(1.dp, DarkBorderSubtle, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = "Copy message",
                                    modifier = Modifier.size(12.dp),
                                    tint = if (isCopied) OpenAIGreen else TextSecondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isCopied) "Copied" else "Copy",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        color = if (isCopied) OpenAIGreen else TextSecondary
                                    )
                                )
                            }
                        }

                        // Metrics Banner (Understated ChatGPT-style metadata tag)
                        message.metrics?.let { metrics ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DarkSurfaceElevated)
                                    .border(1.dp, DarkBorderSubtle, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(11.dp),
                                    tint = OpenAIGreen
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "%.1f tok/s · %d in / %d out".format(
                                        metrics.tokensPerSecond,
                                        metrics.inputTokens,
                                        metrics.outputTokens
                                    ),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
