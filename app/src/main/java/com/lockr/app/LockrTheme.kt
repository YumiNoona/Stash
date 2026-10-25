package com.lockr.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object LockrColors {
    val Pearl = Color(0xFFF7F5F0)
    val SeaGlass = Color(0xFFD9F3ED)
    val Peach = Color(0xFFFFDCD0)
    val LilacMist = Color(0xFFE8DDF8)
    val IceBlue = Color(0xFFDCEBFF)
    val Champagne = Color(0xFFF7E9B8)
    val TextPrimary = Color(0xFF182333)
    val TextSecondary = Color(0xFF66758C)
    val Accent = Color(0xFF398F89)
    val Line = Color(0x22708096)
}

private val LockrLightColors = lightColorScheme(
    primary = LockrColors.Accent,
    onPrimary = Color.White,
    secondary = Color(0xFF8294C5),
    background = LockrColors.Pearl,
    onBackground = LockrColors.TextPrimary,
    surface = Color(0xFFF8FFFFFF),
    onSurface = LockrColors.TextPrimary,
    surfaceVariant = Color(0xFFEFF3F8),
    onSurfaceVariant = LockrColors.TextSecondary,
    outline = Color(0x337B8796)
)

private val StashMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold)
)

private val LockrTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = StashMono, fontSize = 30.sp, lineHeight = 39.sp),
        displayMedium = base.displayMedium.copy(fontFamily = StashMono, fontSize = 27.sp, lineHeight = 36.sp),
        displaySmall = base.displaySmall.copy(fontFamily = StashMono, fontSize = 24.sp, lineHeight = 32.sp),
        headlineLarge = base.headlineLarge.copy(fontFamily = StashMono, fontSize = 27.sp, lineHeight = 35.sp, fontWeight = FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.copy(fontFamily = StashMono, fontSize = 25.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.copy(fontFamily = StashMono, fontSize = 21.sp, lineHeight = 29.sp, fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontFamily = StashMono, fontSize = 18.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontFamily = StashMono, fontSize = 15.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium),
        titleSmall = base.titleSmall.copy(fontFamily = StashMono, fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium),
        bodyLarge = base.bodyLarge.copy(fontFamily = StashMono, fontSize = 14.sp, lineHeight = 22.sp),
        bodyMedium = base.bodyMedium.copy(fontFamily = StashMono, fontSize = 13.sp, lineHeight = 21.sp),
        bodySmall = base.bodySmall.copy(fontFamily = StashMono, fontSize = 11.sp, lineHeight = 18.sp),
        labelLarge = base.labelLarge.copy(fontFamily = StashMono, fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
        labelMedium = base.labelMedium.copy(fontFamily = StashMono, fontSize = 11.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium),
        labelSmall = base.labelSmall.copy(fontFamily = StashMono, fontSize = 10.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
    )
}

@Composable
fun LockrTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LockrLightColors,
        typography = LockrTypography,
        content = content
    )
}
