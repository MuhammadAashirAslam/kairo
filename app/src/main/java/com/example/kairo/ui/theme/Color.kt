package com.example.kairo.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ChatGPT / OpenAI Signature Accents
val OpenAIGreen = Color(0xFF10A37F)
val OpenAIGreenDark = Color(0xFF0D8A6C)
val OpenAIGreenSubtle = Color(0x1F10A37F)
val WhiteAccent = Color(0xFFFFFFFF)

// Compatibility & Secondary Accents (Tasteful neutrals & warm tones, zero neon purple/blue)
val Emerald500 = OpenAIGreenDark
val Emerald400 = OpenAIGreen
val Indigo600 = Color(0xFF2F2F2F)
val Indigo500 = OpenAIGreen
val Indigo400 = Color(0xFFD4D4D8)
val Indigo300 = Color(0xFFE4E4E7)
val Cyan400 = OpenAIGreen
val Amber500 = Color(0xFFF59E0B)
val Rose500 = Color(0xFFEF4444)

// ChatGPT-Style Charcoal & Neutral Surfaces
val DarkBg = Color(0xFF212121)               // Canonical ChatGPT dark background
val DarkSurface = Color(0xFF171717)          // Sidebar, bottom sheets, base canvas
val DarkSurfaceElevated = Color(0xFF2F2F2F)  // Input bar, user message pill, cards
val DarkSurfaceHighlight = Color(0xFF383838) // Active chips, hover states
val DarkBorder = Color(0xFF383838)           // Structural 1px borders
val DarkBorderSubtle = Color(0xFF2D2D2D)     // Hairline dividers

// Semantic Aliases
val SurfaceElevated = DarkSurfaceElevated
val SurfaceHighlight = DarkSurfaceHighlight
val BorderSubtle = DarkBorderSubtle
val Border = DarkBorder
val Bg = DarkBg
val Surface = DarkSurface


// High-Contrast Comfortable Typography
val TextPrimary = Color(0xFFECECEC)          // 92% luminance off-white (easy on eyes)
val TextSecondary = Color(0xFFB4B4B4)        // Metadata, subtitles, descriptions
val TextMuted = Color(0xFF737373)            // Placeholders, disabled icons

// Sleek Subtle Gradients (No neon blue-to-purple clichés)
val BrandGradient = Brush.horizontalGradient(
    listOf(Color(0xFF2F2F2F), Color(0xFF242424))
)
val UserBubbleGradient = Brush.verticalGradient(
    listOf(Color(0xFF2F2F2F), Color(0xFF2F2F2F))
)
val CardHighlightGradient = Brush.verticalGradient(
    listOf(Color(0xFF2A2A2A), Color(0xFF212121))
)
val EmeraldGradient = Brush.horizontalGradient(
    listOf(Color(0xFF10A37F), Color(0xFF0D9488))
)