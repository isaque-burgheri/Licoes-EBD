package br.com.licoesebd.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Palette
val Paper = Color(0xFFFAF6EC)
val PaperDark = Color(0xFFEFE7D4)
val Bg = Color(0xFFF3EDE1)
val Ink = Color(0xFF1A1410)
val InkSoft = Color(0xFF4A3F33)
val Gold = Color(0xFFB08838)
val GoldDeep = Color(0xFF8A6620)
val Burgundy = Color(0xFF6B1F24)
val Night = Color(0xFF16212E)
val LineColor = Color(0x1F1A1410)

// Use system serif for an editorial feel without bundling fonts.
// Replace with Fraunces/Cormorant in res/font for the final version.
val DisplaySerif = FontFamily.Serif
val BodySans = FontFamily.SansSerif
val BodySerif = FontFamily.Serif

object EbdTypography {
    val displayLarge = TextStyle(
        fontFamily = DisplaySerif,
        fontWeight = FontWeight.Medium,
        fontSize = 38.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.5).sp,
        color = Ink
    )
    val displayMedium = TextStyle(
        fontFamily = DisplaySerif,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp,
        color = Ink
    )
    val titleSerif = TextStyle(
        fontFamily = DisplaySerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        color = Ink
    )
    val italicSerif = TextStyle(
        fontFamily = BodySerif,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = Ink
    )
    val body = TextStyle(
        fontFamily = BodySans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = InkSoft
    )
    val label = TextStyle(
        fontFamily = BodySans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        letterSpacing = 2.sp,
        color = GoldDeep
    )
    val labelSmall = TextStyle(
        fontFamily = BodySans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 9.sp,
        letterSpacing = 1.5.sp,
        color = InkSoft
    )
    val readingBody = TextStyle(
        fontFamily = BodySerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        color = Ink
    )
}

private val LightScheme = lightColorScheme(
    primary = Burgundy,
    onPrimary = Paper,
    secondary = Gold,
    onSecondary = Ink,
    background = Bg,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink
)

private val DarkScheme = darkColorScheme(
    primary = Gold,
    onPrimary = Night,
    secondary = Burgundy,
    onSecondary = Paper,
    background = Color(0xFF0F1620),
    onBackground = Paper,
    surface = Night,
    onSurface = Paper
)

@Composable
fun LicoesEbdTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        content = content
    )
}
