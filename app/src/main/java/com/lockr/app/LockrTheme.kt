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
    primary = Color(0xFF7251B5),
    onPrimary = Color.White,
    secondary = Color(0xFF247F89),
    tertiary = Color(0xFFCB724F),
    primaryContainer = Color(0xFFE7DAFF), onPrimaryContainer = Color(0xFF34214F),
    background = Color(0xFFF7F5FC),
    onBackground = Color(0xFF17202D),
    surface = Color(0xF8FFFFFF),
    onSurface = Color(0xFF17202D),
    surfaceVariant = Color(0xFFEFF3F8),
    onSurfaceVariant = Color(0xFF4F5D70),
    outline = Color(0x337B8796)
)

private val StashGrotesk = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_bold, FontWeight.Bold)
)

private val LockrTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = StashGrotesk, fontSize = 30.sp, lineHeight = 39.sp),
        displayMedium = base.displayMedium.copy(fontFamily = StashGrotesk, fontSize = 27.sp, lineHeight = 36.sp),
        displaySmall = base.displaySmall.copy(fontFamily = StashGrotesk, fontSize = 24.sp, lineHeight = 32.sp),
        headlineLarge = base.headlineLarge.copy(fontFamily = StashGrotesk, fontSize = 27.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold),
        headlineMedium = base.headlineMedium.copy(fontFamily = StashGrotesk, fontSize = 25.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontFamily = StashGrotesk, fontSize = 21.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontFamily = StashGrotesk, fontSize = 18.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontFamily = StashGrotesk, fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold),
        titleSmall = base.titleSmall.copy(fontFamily = StashGrotesk, fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold),
        bodyLarge = base.bodyLarge.copy(fontFamily = StashGrotesk, fontSize = 14.sp, lineHeight = 22.sp),
        bodyMedium = base.bodyMedium.copy(fontFamily = StashGrotesk, fontSize = 13.sp, lineHeight = 21.sp),
        bodySmall = base.bodySmall.copy(fontFamily = StashGrotesk, fontSize = 11.sp, lineHeight = 18.sp),
        labelLarge = base.labelLarge.copy(fontFamily = StashGrotesk, fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
        labelMedium = base.labelMedium.copy(fontFamily = StashGrotesk, fontSize = 11.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium),
        labelSmall = base.labelSmall.copy(fontFamily = StashGrotesk, fontSize = 10.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
    )
}

val LocalDarkTheme = compositionLocalOf { false }
val LocalMotionEnabled = compositionLocalOf { true }
val LocalArtworkAutoplay = compositionLocalOf { true }
val LocalOcrOnImport = compositionLocalOf { true }

private val LockrDarkColors = darkColorScheme(
    primary = Color(0xFFD5BCFF), onPrimary = Color(0xFF36244D),
    primaryContainer = Color(0xFF5B4D72), onPrimaryContainer = Color(0xFFF5EBFF),
    background = Color(0xFF2B2D3A), onBackground = Color(0xFFFFFAFF),
    surface = Color(0xFF3A3D4E), onSurface = Color(0xFFFFFAFF),
    surfaceVariant = Color(0xFF4A4C60), onSurfaceVariant = Color(0xFFD2D0E2),
    outline = Color(0xFF9E9AB6), outlineVariant = Color(0xFF64637A),
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
