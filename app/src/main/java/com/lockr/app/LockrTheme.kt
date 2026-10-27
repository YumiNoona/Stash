package com.lockr.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.LocalActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object LockrColors {
    val Pearl: Color @Composable get() = MaterialTheme.colorScheme.background
    val SeaGlass = Color(0xFFD9F3ED)
    val Peach = Color(0xFFFFDCD0)
    val LilacMist = Color(0xFFE8DDF8)
    val IceBlue = Color(0xFFDCEBFF)
    val Champagne = Color(0xFFF7E9B8)
    val TextPrimary: Color @Composable get() = MaterialTheme.colorScheme.onSurface
    val TextSecondary: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val Accent: Color @Composable get() = MaterialTheme.colorScheme.primary
    val Surface: Color @Composable get() = MaterialTheme.colorScheme.surface
    val Raised: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
    val Line: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
}

private val LockrLightColors = lightColorScheme(
    primary = Color(0xFF398F89),
    onPrimary = Color.White,
    secondary = Color(0xFF8294C5),
    background = Color(0xFFF7F5F0),
    onBackground = Color(0xFF182333),
    surface = Color(0xFFF8FFFFFF),
    onSurface = Color(0xFF182333),
    surfaceVariant = Color(0xFFEFF3F8),
    onSurfaceVariant = Color(0xFF66758C),
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

val LocalDarkTheme = compositionLocalOf { false }
val LocalMotionEnabled = compositionLocalOf { true }
val LocalArtworkAutoplay = compositionLocalOf { true }
val LocalOcrOnImport = compositionLocalOf { true }

private val LockrDarkColors = darkColorScheme(
    primary = Color(0xFF83D8CB), onPrimary = Color(0xFF003831),
    background = Color(0xFF09121F), onBackground = Color(0xFFE9F1FF),
    surface = Color(0xFF152234), onSurface = Color(0xFFE9F1FF),
    surfaceVariant = Color(0xFF203147), onSurfaceVariant = Color(0xFFACBED3),
    outline = Color(0xFF576C84), outlineVariant = Color(0xFF304258),
    secondary = Color(0xFFB5B9FF), error = Color(0xFFFFB4AB))

@Composable
fun LockrTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("lockr_preferences", android.content.Context.MODE_PRIVATE) }
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(preferences) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision++ }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val mode = remember(revision) { preferences.getString("theme_mode", "system") }
    val reduced = remember(revision) { preferences.getBoolean("reduce_motion", false) }
    val autoplay = remember(revision) { preferences.getBoolean("artwork_autoplay", true) }
    val ocr = remember(revision) { preferences.getBoolean("ocr_on_import", true) }
    val dark = mode == "dark" || (mode == "system" && isSystemInDarkTheme())
    val activity = LocalActivity.current
    SideEffect {
        activity?.window?.let { window ->
            WindowInsetsControllerCompat(window, window.decorView).apply {
                isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark
            }
        }
    }
    CompositionLocalProvider(LocalDarkTheme provides dark, LocalMotionEnabled provides !reduced,
        LocalArtworkAutoplay provides (autoplay && !reduced), LocalOcrOnImport provides ocr) {
        MaterialTheme(colorScheme = if (dark) LockrDarkColors else LockrLightColors,
            typography = LockrTypography) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) { content() }
        }
    }
}
