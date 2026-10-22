package com.enigmacast.companion.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigmacast.companion.R

/**
 * De kleuren van de standaardskin van EnigmaCast (Glass Earth), overgenomen uit
 * `ui/theme/Skins.kt` van de TV-app. Alleen de tokens; geen TV-componenten.
 */
object Ink {
    val Canvas = Color(0xFF03060F)
    val CanvasLift = Color(0xFF071021)
    val Card = Color(0xCC0F1E3A)
    val CardSolid = Color(0xFF0F1E3A)
    val Border = Color(0x33FFFFFF)
    val Raised = Color(0xCC152A4C)
    val Accent = Color(0xFF5AA9FF)
    val AccentDim = Color(0xFF2A6FDB)
    val Highlight = Color(0xFF9ADCFF)
    val Text = Color(0xFFF7FAFF)
    val Muted = Color(0xFFCCD8EC)
    val Subtle = Color(0xFF93A5C2)
    val Success = Color(0xFF6FE3A5)
    val Danger = Color(0xFFFF8A80)
    val Warning = Color(0xFFFFC46B)
    val OnAccent = Color(0xFF03122E)
}

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

private val CompanionTypography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = Inter),
        displayMedium = base.displayMedium.copy(fontFamily = Inter),
        displaySmall = base.displaySmall.copy(fontFamily = Inter),
        headlineLarge = base.headlineLarge.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Medium),
        titleSmall = base.titleSmall.copy(fontFamily = Inter, fontWeight = FontWeight.Medium),
        bodyLarge = base.bodyLarge.copy(fontFamily = Inter, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = base.bodyMedium.copy(fontFamily = Inter, fontSize = 14.sp, lineHeight = 21.sp),
        bodySmall = base.bodySmall.copy(fontFamily = Inter, fontSize = 12.sp, lineHeight = 18.sp),
        labelLarge = base.labelLarge.copy(fontFamily = Inter, fontWeight = FontWeight.Medium),
        labelMedium = base.labelMedium.copy(fontFamily = Inter),
        labelSmall = base.labelSmall.copy(fontFamily = Inter),
    )
}

private val CompanionShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

@Composable
fun CompanionTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = Ink.Accent,
        onPrimary = Ink.OnAccent,
        primaryContainer = Ink.AccentDim,
        onPrimaryContainer = Ink.Text,
        secondary = Ink.Highlight,
        onSecondary = Ink.OnAccent,
        background = Ink.Canvas,
        onBackground = Ink.Text,
        surface = Ink.CardSolid,
        onSurface = Ink.Text,
        surfaceVariant = Ink.Raised,
        onSurfaceVariant = Ink.Muted,
        outline = Ink.Border,
        error = Ink.Danger,
        onError = Ink.OnAccent,
    )
    MaterialTheme(
        colorScheme = scheme,
        typography = CompanionTypography,
        shapes = CompanionShapes,
        content = content,
    )
}
