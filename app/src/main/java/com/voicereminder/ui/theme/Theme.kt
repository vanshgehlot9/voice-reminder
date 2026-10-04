package com.voicereminder.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ============================================================
// Color palette — soft cream foundation + amber accent
// Legacy names are preserved to minimize churn across the app.
// ============================================================

// Background layers
val Navy900 = Color(0xFFF6F0E8)   // app background / parchment
val Navy800 = Color(0xFFEDE2D4)   // card background
val Navy700 = Color(0xFFE2D3BF)   // elevated surfaces
val Navy600 = Color(0xFFD0C0AB)   // dividers / borders

// Accent
val Violet500 = Color(0xFFC9891F)  // primary action / amber
val Violet400 = Color(0xFFD79A31)  // hover / focus
val Violet300 = Color(0xFFE7B863)  // secondary text / icons

// Status
val GreenSuccess = Color(0xFF3F7A58)
val RedError = Color(0xFFB85B4F)
val OrangeWarning = Color(0xFFD58A1B)

// Text
val White = Color(0xFF171411)
val White50 = Color(0x80171411)
val Grey300 = Color(0xFF4B453F)
val Grey500 = Color(0xFF7B736A)

// Accents added later
val Coral500 = Color(0xFFB9701A)

private val LightColorScheme = lightColorScheme(
    primary = Violet500,
    onPrimary = Navy900,
    primaryContainer = Color(0xFFF4D39E),
    onPrimaryContainer = White,

    secondary = Violet300,
    onSecondary = White,

    background = Navy900,
    onBackground = White,

    surface = Navy800,
    onSurface = White,
    surfaceVariant = Navy700,
    onSurfaceVariant = Grey300,

    error = RedError,
    onError = Navy900,

    outline = Navy600,
)

private val EditorialTypography = Typography(
    displayLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 56.sp,
        lineHeight = 60.sp,
    ),
    displayMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 44.sp,
    ),
    headlineLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
    ),
    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
    ),
)

@Composable
fun VoiceReminderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = EditorialTypography,
        content = content,
    )
}
