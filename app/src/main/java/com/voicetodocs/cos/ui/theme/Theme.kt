package com.voicetodocs.cos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Cream = Color(0xFFF7F1E8)
val Ink = Color(0xFF1A1A1A)
val Teal = Color(0xFF0B5F56)
val TealDark = Color(0xFF073E39)
val OnTeal = Color(0xFFF7F1E8)
val Danger = Color(0xFF9B1C1C)
val OkGreen = Color(0xFF1B5E20)
val CardBg = Color(0xFFFFFFFF)

private val Colors = lightColorScheme(
    primary = Teal,
    onPrimary = OnTeal,
    secondary = TealDark,
    onSecondary = OnTeal,
    background = Cream,
    onBackground = Ink,
    surface = Cream,
    onSurface = Ink,
    error = Danger,
    onError = Color.White
)

private val Type = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        color = Ink
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = Ink
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        color = Ink
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = Ink
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    )
)

@Composable
fun CosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colors,
        typography = Type,
        content = content
    )
}
