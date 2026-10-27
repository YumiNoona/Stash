package com.lockr.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class BackdropPreset(val id: String, val label: String, val tint: Color, val wash: Color)

val BACKDROP_PRESETS = listOf(
    BackdropPreset("aurora", "Aurora", Color.White, Color.Transparent),
    BackdropPreset("sapphire", "Sapphire", Color(0xFF96BDFF), Color(0x342153B8)),
    BackdropPreset("amethyst", "Amethyst", Color(0xFFD2A6FF), Color(0x3647288D)),
    BackdropPreset("rose", "Rose", Color(0xFFFFB1CB), Color(0x36A72D73)),
    BackdropPreset("lagoon", "Lagoon", Color(0xFF8DEAD9), Color(0x30248291)),
    BackdropPreset("sunset", "Sunset", Color(0xFFFFC393), Color(0x40B7503C)),
    BackdropPreset("citrine", "Citrine", Color(0xFFF4DC91), Color(0x36AE8235)),
    BackdropPreset("mint", "Mint", Color(0xFFACF4C7), Color(0x303C9B72)),
    BackdropPreset("pearl", "Pearl", Color(0xFFDDDFFF), Color(0x24777BA4)),
    BackdropPreset("midnight", "Midnight", Color(0xFF8091C9), Color(0x604A328B)),
    BackdropPreset("coral", "Coral", Color(0xFFFFAD9E), Color(0x3ABD5552))
)

private fun customBackdropFile(context: android.content.Context, dark: Boolean) =
    File(context.filesDir, if (dark) "custom-backdrop-dark.jpg" else "custom-backdrop-light.jpg")

@Composable
fun StashBackdrop(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dark = LocalDarkTheme.current
    val prefs = remember(context) { context.getSharedPreferences("lockr_preferences", 0) }
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision++ }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val id = remember(revision, dark) { prefs.getString(if (dark) "backdrop_dark" else "backdrop_light", "aurora") ?: "aurora" }
    val preset = BACKDROP_PRESETS.firstOrNull { it.id == id } ?: BACKDROP_PRESETS.first()
    var custom by remember(id, revision, dark) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(id, revision, dark) {
        custom = if (id == "custom") withContext(Dispatchers.IO) {
            BitmapFactory.decodeFile(customBackdropFile(context, dark).absolutePath)
        } else null
    }
    Box(modifier.fillMaxSize()) {
        if (custom != null) Image(custom!!.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Image(painterResource(if (dark) R.drawable.vault_dark_aurora_bg else R.drawable.vault_aurora_bg),
            null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
            colorFilter = if (id == "aurora") null else ColorFilter.tint(preset.tint, BlendMode.Modulate))
        if (id != "aurora" && id != "custom") Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(preset.wash, Color.Transparent, preset.wash.copy(alpha = preset.wash.alpha * .5f)))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            if (dark) listOf(Color(0x24101929), Color(0x30101929), Color(0x30121B2C))
            else listOf(Color(0x18E6DAFF), Color(0x0FFFD9C9), Color(0x16C6F3EA)))))
    }
}

@Composable
fun BackdropSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("lockr_preferences", 0) }
    val scope = rememberCoroutineScope()
    var target by remember { mutableStateOf("Both") }
    var status by remember { mutableStateOf("") }
    var revision by remember { mutableIntStateOf(0) }
    val lightId = remember(revision, target) { prefs.getString("backdrop_light", "aurora") ?: "aurora" }
    val darkId = remember(revision, target) { prefs.getString("backdrop_dark", "aurora") ?: "aurora" }
    var customPreview by remember(target, revision) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(target, revision) {
        customPreview = withContext(Dispatchers.IO) {
            BitmapFactory.decodeFile(customBackdropFile(context, target == "Dark").absolutePath)
        }
    }
    val apply: (String) -> Unit = { id ->
        prefs.edit().apply {
            if (target != "Dark") putString("backdrop_light", id)
            if (target != "Light") putString("backdrop_dark", id)
        }.apply()
        revision++
        status = "Applied to ${target.lowercase()} mode${if (target == "Both") "s" else ""}."
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                        ?: error("Image could not be opened")
                    val scale = minOf(1f, 1600f / maxOf(bitmap.width, bitmap.height))
                    val sized = if (scale < 1f) Bitmap.createScaledBitmap(bitmap,
                        (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true) else bitmap
                    val files = when (target) {
                        "Light" -> listOf(customBackdropFile(context, false))
                        "Dark" -> listOf(customBackdropFile(context, true))
                        else -> listOf(customBackdropFile(context, false), customBackdropFile(context, true))
                    }
                    files.forEach { file -> file.outputStream().use { sized.compress(Bitmap.CompressFormat.JPEG, 88, it) } }
                    if (sized !== bitmap) sized.recycle()
                    bitmap.recycle()
                }
                apply("custom")
            } catch (_: Exception) { status = "Could not use this image. Try another photo." }
        }
    }
    Box(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Column(Modifier.fillMaxSize().padding(top = 86.dp)) {
            Text("Apply to", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Both", "Light", "Dark").forEach { option ->
                    FilterChip(target == option, onClick = { target = option; revision++ },
                        label = { Text(option) })
                }
            }
            Text("Choose a background", style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 15.dp, bottom = 8.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 25.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(BACKDROP_PRESETS) { preset ->
                    val selected = when (target) {
                        "Light" -> lightId == preset.id
                        "Dark" -> darkId == preset.id
                        else -> lightId == preset.id && darkId == preset.id
                    }
                    Box(Modifier.fillMaxWidth().height(133.dp).clip(RoundedCornerShape(20.dp))
                        .clickable { apply(preset.id) }
                        .border(if (selected) 2.dp else 1.dp, if (selected) LockrColors.Accent else LockrColors.Line,
                            RoundedCornerShape(20.dp))) {
                        Image(painterResource(if (LocalDarkTheme.current) R.drawable.vault_dark_aurora_bg else R.drawable.vault_aurora_bg),
                            null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                            colorFilter = if (preset.id == "aurora") null else ColorFilter.tint(preset.tint, BlendMode.Modulate))
                        Box(Modifier.fillMaxSize().background(preset.wash))
                        Box(Modifier.fillMaxWidth().height(38.dp).align(Alignment.BottomCenter).background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color(0xB9001025)))))
                        Text(preset.label, color = Color.White, style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp))
                    }
                }
                item {
                    Box(Modifier.fillMaxWidth().height(133.dp).clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF626A9C), Color(0xFF85D7D1))))
                        .clickable { picker.launch("image/*") }, contentAlignment = Alignment.Center) {
                        customPreview?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.AddPhotoAlternate, "Choose custom background", tint = Color.White)
                            Text("Your photo", color = Color.White, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
            if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall,
                color = LockrColors.TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
        }
        StashTopBar("Backgrounds", onBack = onBack)
    }
}
