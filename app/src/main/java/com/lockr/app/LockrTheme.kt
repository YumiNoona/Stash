package com.lockr.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
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

private val StashSans = FontFamily.SansSerif

private val LockrTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = StashSans, fontSize = 34.sp, lineHeight = 42.sp),
        displayMedium = base.displayMedium.copy(fontFamily = StashSans, fontSize = 30.sp, lineHeight = 38.sp),
        displaySmall = base.displaySmall.copy(fontFamily = StashSans, fontSize = 26.sp, lineHeight = 34.sp),
        headlineLarge = base.headlineLarge.copy(fontFamily = StashSans, fontSize = 30.sp, lineHeight = 37.sp, fontWeight = FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.copy(fontFamily = StashSans, fontSize = 27.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.copy(fontFamily = StashSans, fontSize = 23.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontFamily = StashSans, fontSize = 20.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontFamily = StashSans, fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
        titleSmall = base.titleSmall.copy(fontFamily = StashSans, fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium),
        bodyLarge = base.bodyLarge.copy(fontFamily = StashSans, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = base.bodyMedium.copy(fontFamily = StashSans, fontSize = 15.sp, lineHeight = 22.sp),
        bodySmall = base.bodySmall.copy(fontFamily = StashSans, fontSize = 13.sp, lineHeight = 19.sp),
        labelLarge = base.labelLarge.copy(fontFamily = StashSans, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
        labelMedium = base.labelMedium.copy(fontFamily = StashSans, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
        labelSmall = base.labelSmall.copy(fontFamily = StashSans, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
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
