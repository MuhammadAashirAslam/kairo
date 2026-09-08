package com.example.kairo.presentation.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kairo.ui.theme.DarkBorder
import com.example.kairo.ui.theme.DarkBorderSubtle
import com.example.kairo.ui.theme.DarkSurface
import com.example.kairo.ui.theme.DarkSurfaceElevated
import com.example.kairo.ui.theme.DarkSurfaceHighlight
import com.example.kairo.ui.theme.OpenAIGreen
import com.example.kairo.ui.theme.TextMuted
import com.example.kairo.ui.theme.TextPrimary
import com.example.kairo.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * Rich Markdown renderer for ChatGPT-style assistant and user messages.
 * Parses headers, bold, italic, inline code, code blocks (with copy button),
 * bullet lists, numbered lists, blockquotes, and streaming cursor.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    isGenerating: Boolean = false,
    cursorModifier: Modifier = Modifier,
    textColor: Color = TextPrimary
) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEachIndexed { index, block ->
            val isLastBlock = index == blocks.lastIndex

            when (block) {
                is MarkdownBlock.Header -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = buildInlineMarkdown(block.text, textColor),
                            style = when (block.level) {
                                1 -> MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    color = textColor
                                )
                                2 -> MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = textColor
                                )
                                else -> MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = textColor
                                )
                            },
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        )
                        if (isLastBlock && isGenerating) {
                            StreamingCursor(cursorModifier)
                        }
                    }
                }

                is MarkdownBlock.CodeBlock -> {
                    CodeBlockCard(
                        language = block.language,
                        code = block.code
                    )
                }

                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (block.indent * 14).dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp, end = 8.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(OpenAIGreen)
                        )
                        Row(modifier = Modifier.weight(1f)) {
                            Text(
                                text = buildInlineMarkdown(block.text, textColor),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = textColor,
                                    fontSize = 14.5.sp,
                                    lineHeight = 22.sp
                                )
                            )
                            if (isLastBlock && isGenerating) {
                                StreamingCursor(cursorModifier)
                            }
                        }
                    }
                }

                is MarkdownBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (block.indent * 14).dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = block.number,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = OpenAIGreen,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            ),
                            modifier = Modifier.padding(end = 8.dp, top = 1.dp)
                        )
                        Row(modifier = Modifier.weight(1f)) {
                            Text(
                                text = buildInlineMarkdown(block.text, textColor),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = textColor,
                                    fontSize = 14.5.sp,
                                    lineHeight = 22.sp
                                )
                            )
                            if (isLastBlock && isGenerating) {
                                StreamingCursor(cursorModifier)
                            }
                        }
                    }
                }

                is MarkdownBlock.Blockquote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(DarkSurfaceElevated.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(22.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(OpenAIGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = buildInlineMarkdown(block.text, TextSecondary),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic,
                                color = TextSecondary,
                                fontSize = 14.sp,
                                lineHeight = 21.sp
                            )
                        )
                    }
                }

                is MarkdownBlock.HorizontalRule -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .height(1.dp)
                            .background(DarkBorderSubtle)
                    )
                }

                is MarkdownBlock.Paragraph -> {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = buildInlineMarkdown(block.text, textColor),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = textColor,
                                fontSize = 14.5.sp,
                                lineHeight = 23.sp
                            )
                        )
                        if (isLastBlock && isGenerating) {
                            StreamingCursor(cursorModifier)
                        }
                    }
                }
            }
        }

        // If markdown is completely empty and generating, show cursor
        if (blocks.isEmpty() && isGenerating) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Thinking...", style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted, fontSize = 14.sp))
                StreamingCursor(cursorModifier)
            }
        }
    }
}

@Composable
private fun StreamingCursor(modifier: Modifier = Modifier) {
    Text(
        text = " ▍",
        color = OpenAIGreen,
        modifier = modifier
    )
}

/**
 * ChatGPT-style Code Block with syntax styling, language badge, and a dedicated Copy Code button.
 */
@Composable
fun CodeBlockCard(
    language: String,
    code: String,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var isCopied by remember { mutableStateOf(false) }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(2000)
            isCopied = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
    ) {
        // Code Block Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceHighlight)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language.ifBlank { "code" }.lowercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        clipboardManager.setText(AnnotatedString(code))
                        isCopied = true
                        Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = "Copy code",
                    tint = if (isCopied) OpenAIGreen else TextSecondary,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = if (isCopied) "Copied!" else "Copy code",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        color = if (isCopied) OpenAIGreen else TextSecondary
                    )
                )
            }
        }

        // Code Content (horizontally scrollable)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Text(
                text = code,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFE2E8F0),
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            )
        }
    }
}

/**
 * Builds an AnnotatedString from markdown text supporting bold (**text**),
 * italic (*text*), inline code (`code`), and strikethrough (~~text~~).
 */
fun buildInlineMarkdown(
    rawText: String,
    baseColor: Color = TextPrimary,
    inlineCodeBg: Color = Color(0xFF2A2A2A),
    inlineCodeColor: Color = Color(0xFFE2E8F0)
): AnnotatedString {
    return buildAnnotatedString {
        // Regex matches bold/italic (***), bold (**), italic (*), code (`), strikethrough (~~)
        val regex = Regex("""(\*\*\*.*?\*\*\*|\*\*.*?\*\*|\*.*?\*|`.*?`|~~.*?~~)""")
        var currentIndex = 0
        val matches = regex.findAll(rawText)

        for (match in matches) {
            if (match.range.first > currentIndex) {
                append(rawText.substring(currentIndex, match.range.first))
            }
            val token = match.value
            when {
                token.startsWith("***") && token.endsWith("***") && token.length >= 6 -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, color = baseColor)) {
                        append(token.substring(3, token.length - 3))
                    }
                }
                token.startsWith("**") && token.endsWith("**") && token.length >= 4 -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = baseColor)) {
                        append(token.substring(2, token.length - 2))
                    }
                }
                token.startsWith("*") && token.endsWith("*") && token.length >= 2 -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = baseColor)) {
                        append(token.substring(1, token.length - 1))
                    }
                }
                token.startsWith("`") && token.endsWith("`") && token.length >= 2 -> {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = inlineCodeBg,
                            color = inlineCodeColor,
                            fontSize = 13.5.sp
                        )
                    ) {
                        append(" ${token.substring(1, token.length - 1)} ")
                    }
                }
                token.startsWith("~~") && token.endsWith("~~") && token.length >= 4 -> {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = baseColor.copy(alpha = 0.7f))) {
                        append(token.substring(2, token.length - 2))
                    }
                }
                else -> append(token)
            }
            currentIndex = match.range.last + 1
        }

        if (currentIndex < rawText.length) {
            val remaining = rawText.substring(currentIndex)
            // Handle unclosed bold/code during streaming
            if (remaining.startsWith("**") && remaining.length > 2) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = baseColor)) {
                    append(remaining.removePrefix("**"))
                }
            } else if (remaining.startsWith("`") && remaining.length > 1) {
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = inlineCodeBg, color = inlineCodeColor)) {
                    append(" ${remaining.removePrefix("`")} ")
                }
            } else {
                append(remaining)
            }
        }
    }
}

sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class BulletItem(val bullet: String, val text: String, val indent: Int = 0) : MarkdownBlock()
    data class NumberedItem(val number: String, val text: String, val indent: Int = 0) : MarkdownBlock()
    data class Blockquote(val text: String) : MarkdownBlock()
    data object HorizontalRule : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
}

/**
 * Parses markdown text into structural blocks.
 */
fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    if (markdown.isBlank()) return emptyList()

    val blocks = mutableListOf<MarkdownBlock>()
    val lines = markdown.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // 1. Code Block Fence (```)
        if (line.trimStart().startsWith("```")) {
            val trimmed = line.trimStart()
            val language = trimmed.removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size && lines[i].trimStart().startsWith("```")) {
                i++ // consume closing ```
            }
            blocks.add(MarkdownBlock.CodeBlock(language, codeLines.joinToString("\n")))
            continue
        }

        // 2. Horizontal Rule (---, ***, ___)
        val trimmedLine = line.trim()
        if (trimmedLine == "---" || trimmedLine == "***" || trimmedLine == "___") {
            blocks.add(MarkdownBlock.HorizontalRule)
            i++
            continue
        }

        // 3. Headers (# H1, ## H2, ### H3)
        if (trimmedLine.startsWith("#")) {
            val level = trimmedLine.takeWhile { it == '#' }.length
            if (level in 1..6 && trimmedLine.length > level && trimmedLine[level] == ' ') {
                val headerText = trimmedLine.substring(level + 1).trim()
                blocks.add(MarkdownBlock.Header(level, headerText))
                i++
                continue
            }
        }

        // 4. Blockquotes (> text)
        if (trimmedLine.startsWith(">")) {
            val quoteText = trimmedLine.removePrefix(">").trim()
            blocks.add(MarkdownBlock.Blockquote(quoteText))
            i++
            continue
        }

        // 5. Bullet List (- item, * item, + item, • item)
        val bulletMatch = Regex("""^(\s*)([-*+•])\s+(.*)$""").find(line)
        if (bulletMatch != null) {
            val indentSpaces = bulletMatch.groupValues[1].length
            val bullet = bulletMatch.groupValues[2]
            val text = bulletMatch.groupValues[3]
            blocks.add(MarkdownBlock.BulletItem(bullet, text, indent = indentSpaces / 2))
            i++
            continue
        }

        // 6. Numbered List (1. item, 2. item)
        val numMatch = Regex("""^(\s*)(\d+\.)\s+(.*)$""").find(line)
        if (numMatch != null) {
            val indentSpaces = numMatch.groupValues[1].length
            val num = numMatch.groupValues[2]
            val text = numMatch.groupValues[3]
            blocks.add(MarkdownBlock.NumberedItem(num, text, indent = indentSpaces / 2))
            i++
            continue
        }

        // 7. Regular Paragraph or Empty line
        if (trimmedLine.isEmpty()) {
            i++
            continue
        }

        // Gather consecutive non-empty lines into a single paragraph
        val paragraphLines = mutableListOf<String>()
        while (i < lines.size) {
            val nextLine = lines[i]
            val nextTrimmed = nextLine.trim()
            if (nextTrimmed.isEmpty() ||
                nextTrimmed.startsWith("```") ||
                nextTrimmed.startsWith("#") ||
                nextTrimmed.startsWith(">") ||
                nextTrimmed == "---" ||
                Regex("""^(\s*)([-*+•]|\d+\.)\s+""").containsMatchIn(nextLine)
            ) {
                break
            }
            paragraphLines.add(nextLine)
            i++
        }

        if (paragraphLines.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(paragraphLines.joinToString("\n")))
        }
    }

    return blocks
}
