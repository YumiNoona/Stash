package com.lockr.app

import androidx.compose.material.icons.outlined.CreditCard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class LockrSettingsState(
    val appLockEnabled: Boolean = false,
    val autoLock: String = "After 1 minute",
    val revealTimeout: String = "30 seconds",
    val hideCardNumber: Boolean = false,
    val autoClearClipboard: Boolean = true,
    val hideInRecents: Boolean = false
)

@Composable
fun SettingsScreen(
    settings: LockrSettingsState,
    onSettingsChange: (LockrSettingsState) -> Unit,
    onBack: () -> Unit,
    onExportBackup: () -> Unit,
    onExportData: (DataExportFormat, Boolean) -> Unit,
    onImportBackup: () -> Unit,
    onOpenOrganization: () -> Unit,
    backupStatus: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("lockr_preferences", android.content.Context.MODE_PRIVATE) }
    val dark = LocalDarkTheme.current
    var themeMode by remember { mutableStateOf(preferences.getString("theme_mode", "system") ?: "system") }
    var reducedMotion by remember { mutableStateOf(preferences.getBoolean("reduce_motion", false)) }
    var artworkAutoplay by remember { mutableStateOf(preferences.getBoolean("artwork_autoplay", true)) }
    var ocrEnabled by remember { mutableStateOf(preferences.getBoolean("ocr_on_import", true)) }
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
    var dialog by remember { mutableStateOf("") }

    Box(modifier.fillMaxSize().padding(horizontal = 22.dp)) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 88.dp)) {
                SettingsSection("Appearance") {
                    SettingsRow(Icons.Outlined.Animation, "Reduce motion", "Keep artwork and controls still", toggle = reducedMotion,
                        onToggle = { reducedMotion = it; preferences.edit().putBoolean("reduce_motion", it).apply() })
                    SettingsRow(Icons.Outlined.PlayCircle, "Animated card artwork", "Play saved GIF and video art", toggle = artworkAutoplay,
                        onToggle = { artworkAutoplay = it; preferences.edit().putBoolean("artwork_autoplay", it).apply() })
                    SettingsRow(Icons.Outlined.CreditCard, "Home-screen widgets", "Add a wallet or document folder", last = true, onClick = { dialog = "widgets" })
                }
                SettingsSection("Organization", modifier = Modifier.padding(top = 14.dp)) {
                    SettingsRow(Icons.Outlined.Label, "Custom tags and folders", "Manage your labels and folders", value = "›", onClick = onOpenOrganization)
                    SettingsRow(Icons.Outlined.DocumentScanner, "Read text on import", "Suggest fields locally from scans", toggle = ocrEnabled,
                        last = true, onToggle = { ocrEnabled = it; preferences.edit().putBoolean("ocr_on_import", it).apply() })
                }
                SettingsSection("Security", modifier = Modifier.padding(top = 14.dp)) {
                    SettingsRow(
                        Icons.Outlined.Lock, "App Lock", "Require device unlock after idle",
                        toggle = settings.appLockEnabled,
                        iconTint = Color(0xFF18A996), iconBg = Color(0xFFDDF8F3),
                        onToggle = { onSettingsChange(settings.copy(appLockEnabled = it)) }
                    )
                    SettingsRow(
                        Icons.Outlined.Timer, "Auto Lock", settings.autoLock, value = "›",
                        iconTint = Color(0xFF3B6589), iconBg = Color(0xFFFFECDD),
                        onClick = { dialog = "auto" }
                    )
                    SettingsRow(
                        Icons.Outlined.Visibility, "Reveal Timeout", "Hide revealed card details", value = settings.revealTimeout,
                        iconTint = Color(0xFF4F68D4), iconBg = Color(0xFFE9E6FF),
                        onClick = { dialog = "reveal" }
                    )
                    SettingsRow(
                        Icons.Outlined.VisibilityOff, "Card number display",
                        if (settings.hideCardNumber) "Number hidden · CVV hidden" else "Number shown · CVV hidden",
                        value = "›", iconTint = Color(0xFF5065B5), iconBg = Color(0xFFE9EEFF),
                        onClick = { dialog = "cardVisibility" }
                    )
                    SettingsRow(
                        Icons.Outlined.Key, "Vault Encryption", "Android Keystore · this device", value = "›",
                        iconTint = Color(0xFF8C4A8D), iconBg = Color(0xFFF9E5F0), last = true,
                        onClick = { dialog = "encryption" }
                    )
                }

                SettingsSection("Export & Backup", modifier = Modifier.padding(top = 14.dp)) {
                    SettingsRow(
                        Icons.Outlined.IosShare, "Export Vault", "JSON, PDF, Excel or text", value = "›",
                        iconTint = Color(0xFF286DE0), iconBg = Color(0xFFE3EEFF), onClick = { dialog = "export" }
                    )
                    SettingsRow(
                        Icons.Outlined.CloudUpload, "Export Encrypted Backup", "Save a protected file on this device", value = "›",
                        iconTint = Color(0xFF286DE0), iconBg = Color(0xFFE3EEFF), onClick = onExportBackup
                    )
                    SettingsRow(
                        Icons.Outlined.Restore, "Import Backup", "Restore a backup made on this device", value = "›",
                        iconTint = Color(0xFF5A64C8), iconBg = LockrColors.Surface, last = true, onClick = onImportBackup
                    )
                }
                backupStatus?.let {
                    Text(it, color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 7.dp, top = 7.dp))
                }

                SettingsSection("Privacy", modifier = Modifier.padding(top = 14.dp)) {
                    SettingsRow(
                        Icons.Outlined.ContentPaste, "Auto Clear Clipboard", "Clear copied details after 30s",
                        toggle = settings.autoClearClipboard,
                        iconTint = Color(0xFF395F94), iconBg = Color(0xFFE4F1FF),
                        onToggle = { onSettingsChange(settings.copy(autoClearClipboard = it)) }
                    )
                    SettingsRow(
                        Icons.Outlined.PrivacyTip, "Hide App in Recents", "Block screenshots and app previews",
                        toggle = settings.hideInRecents,
                        iconTint = Color(0xFF4D6382), iconBg = Color(0xFFE6F5F7), last = true,
                        onToggle = { onSettingsChange(settings.copy(hideInRecents = it)) }
                    )
                }
                SettingsSection("About", modifier = Modifier.padding(top = 14.dp)) {
                    SettingsRow(
                        Icons.Outlined.Info, "About Stash", "Version $version · offline vault", value = "›",
                        iconTint = Color(0xFF4C526B), iconBg = LockrColors.Surface, last = true,
                        onClick = { dialog = "about" }
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

        }
        StashTopBar("Settings", onBack = onBack,
            trailing = { IconButton(onClick = {
                themeMode = if (dark) "light" else "dark"
                preferences.edit().putString("theme_mode", themeMode).apply()
            }) { Icon(if (dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                if (dark) "Switch to light mode" else "Switch to dark mode", tint = LockrColors.Accent) } })
    }

    when (dialog) {
        "widgets" -> AlertDialog(onDismissRequest = { dialog = "" }, title = { Text("Add a home-screen widget") },
            text = { Column {
                Text("Private values stay in Stash. Resize widgets on your home screen.", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { dialog = ""; pinStashWidget(context, false) }) { Text("Wallet widget") }
                TextButton(onClick = { dialog = ""; pinStashWidget(context, true) }) { Text("Documents widget") }
            } }, confirmButton = { TextButton(onClick = { dialog = "" }) { Text("Done") } })
        "export" -> ExportFormatDialog(onDismiss = { dialog = "" }) { format, includePrivate ->
            dialog = ""
            onExportData(format, includePrivate)
        }
        "auto" -> ChoiceDialog(
            "Auto Lock", listOf("Immediately", "After 30 seconds", "After 1 minute", "After 5 minutes"),
            selected = settings.autoLock,
            onDismiss = { dialog = "" }
        ) { onSettingsChange(settings.copy(autoLock = it)); dialog = "" }
        "reveal" -> ChoiceDialog(
            "Reveal Timeout", listOf("10 seconds", "30 seconds", "1 minute", "5 minutes"),
            selected = settings.revealTimeout,
            onDismiss = { dialog = "" }
        ) { onSettingsChange(settings.copy(revealTimeout = it)); dialog = "" }
        "cardVisibility" -> ChoiceDialog(
            "Card number display", listOf("Show card number", "Hide card number"),
            selected = if (settings.hideCardNumber) "Hide card number" else "Show card number",
            onDismiss = { dialog = "" }
        ) { onSettingsChange(settings.copy(hideCardNumber = it == "Hide card number")); dialog = "" }
        "encryption" -> InfoDialog(
            "On-device encryption",
            "Vault records are encrypted with AES-GCM. The encryption key is held by Android Keystore on this device; a master password is not used.",
            onDismiss = { dialog = "" }
        )
        "about" -> InfoDialog(
            "Stash · $version",
            "Your vault is stored locally on this device. Stash has no account, cloud sync, or network connection.",
            onDismiss = { dialog = "" }
        )
    }
}

@Composable
private fun ExportFormatDialog(onDismiss: () -> Unit, onExport: (DataExportFormat, Boolean) -> Unit) {
    var includePrivate by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LockrColors.Surface,
        shape = RoundedCornerShape(27.dp),
        title = { Text("Export vault") },
        text = {
            Column {
                Text("Choose a readable file format. Attachments are included in JSON only when private data is enabled.", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                DataExportFormat.entries.forEach { format ->
                    Text(format.label, modifier = Modifier.fillMaxWidth().clickable { onExport(format, includePrivate) }.padding(vertical = 13.dp), color = LockrColors.TextPrimary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Include private fields", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = includePrivate, onCheckedChange = { includePrivate = it })
                }
                Text("Readable exports are not encrypted. Keep them in a safe location.", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SettingsSection(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 3.dp, bottom = 7.dp))
        GlassPanel(Modifier.fillMaxWidth(), radius = 21.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) { content() }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String? = null,
    toggle: Boolean? = null,
    iconTint: Color = Color(0xFF344359),
    iconBg: Color = Color(0xFFE8EFF7),
    last: Boolean = false,
    onToggle: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(63.dp).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(39.dp).clip(CircleShape).background(iconBg), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(21.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = LockrColors.TextPrimary, maxLines = 1)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LockrColors.TextSecondary, maxLines = 1)
        }
        if (toggle != null) {
            Switch(checked = toggle, onCheckedChange = { onToggle?.invoke(it) })
        } else if (value != null) {
            if (value == "›") {
                Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = "Open", tint = LockrColors.TextSecondary, modifier = Modifier.padding(start = 8.dp, end = 4.dp).size(16.dp))
            } else {
                Text(value, color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp, end = 4.dp))
            }
        }
    }
    if (!last) androidx.compose.material3.HorizontalDivider(color = LockrColors.Line)
}

@Composable
private fun ChoiceDialog(title: String, choices: List<String>, selected: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                choices.forEach { choice ->
                    Text(
                        text = (if (choice == selected) "●  " else "○  ") + choice,
                        color = if (choice == selected) LockrColors.Accent else LockrColors.TextPrimary,
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(choice) }.padding(vertical = 10.dp)
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun InfoDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message, color = LockrColors.TextSecondary) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}
