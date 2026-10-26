package com.franzk.mijo.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object MijoColors {
    val Cream = Color(0xFFFAF8EB)
    val CreamDeep = Color(0xFFF1EDD6)
    val Paper = Color(0xFFFFFEF7)
    val Butter = Color(0xFFF4D35E)
    val ButterSoft = Color(0xFFF8E6A0)
    val Ink = Color(0xFF2B2A26)
    val InkSoft = Color(0xFF6E6A5E)
    val Muted = Color(0xFF9C978A)
    val Glass = Color(0xE62B2A26)
    val GlassChip = Color(0x33FFFFFF)
    val Danger = Color(0xFFC0463B)
}

private val typography = Typography(
    displaySmall = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun MijoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = MijoColors.Ink,
            onPrimary = MijoColors.Paper,
            secondary = MijoColors.Butter,
            onSecondary = MijoColors.Ink,
            background = MijoColors.Cream,
            onBackground = MijoColors.Ink,
            surface = MijoColors.Paper,
            onSurface = MijoColors.Ink,
            surfaceVariant = MijoColors.CreamDeep,
            onSurfaceVariant = MijoColors.InkSoft,
        ),
        typography = typography,
        content = content,
    )
}
