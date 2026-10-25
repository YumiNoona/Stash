package com.lockr.app

import android.app.Activity
import android.app.KeyguardManager
import android.os.Bundle
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (android.os.Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent { LockrTheme { LockrApp() } }
    }
}

enum class LockrScreen { HOME, ALL_ITEMS, CARDS, DOCUMENTS, PASSWORDS, NOTES, SETTINGS, PROFILE }

enum class VaultCategory(val label: String) {
    CARD("Card"), DOCUMENT("Document"), PASSWORD("Password"), NOTE("Note")
}

data class VaultItem(
    val title: String,
    val category: VaultCategory,
    val detail: String,
    val time: String,
    val lastFour: String = "",
    val metadata: Map<String, String> = emptyMap(),
    val attachments: List<VaultAttachment> = emptyList()
)

data class VaultAttachment(
    val name: String,
    val mimeType: String,
    val byteCount: Long,
    val base64Data: String
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LockrApp() {
    var screen by rememberSaveable { mutableStateOf(LockrScreen.HOME) }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var savingItem by rememberSaveable { mutableStateOf(false) }
    var addSaveError by rememberSaveable { mutableStateOf<String?>(null) }
    val activity = LocalContext.current as? ComponentActivity
    val context = activity?.applicationContext ?: LocalContext.current.applicationContext
    val preferences = remember(context) { context.getSharedPreferences("lockr_preferences", Context.MODE_PRIVATE) }
    var onboardingComplete by rememberSaveable { mutableStateOf(preferences.getBoolean("onboarding_complete", false)) }
    val store = remember(context) { LocalVaultStore(context) }
    val items = remember { mutableStateListOf<VaultItem>() }
    var settings by remember(context) {
        mutableStateOf(
            LockrSettingsState(
                appLockEnabled = preferences.getBoolean("app_lock_enabled", false),
                autoLock = preferences.getString("auto_lock", "After 1 minute") ?: "After 1 minute",
                revealTimeout = preferences.getString("reveal_timeout", "30 seconds") ?: "30 seconds",
                autoClearClipboard = preferences.getBoolean("auto_clear_clipboard", true),
                hideInRecents = preferences.getBoolean("hide_in_recents", false),
                launcherStyle = preferences.getString("launcher_style", "Stash") ?: "Stash"
            )
        )
    }
    var storageReady by remember { mutableStateOf(false) }
    var storageError by remember { mutableStateOf(false) }
    var backupStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var restoreBytes by remember { mutableStateOf<ByteArray?>(null) }
    var restoreItemCount by rememberSaveable { mutableStateOf(0) }
    var vaultLocked by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val keyguardManager = remember(context) { context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager }
    val lifecycleOwner = LocalLifecycleOwner.current
    var backgroundedAt by remember { mutableStateOf(0L) }

    BackHandler(enabled = vaultLocked) { }
    BackHandler(enabled = !vaultLocked && !showAdd && restoreBytes == null && screen != LockrScreen.HOME) {
        screen = LockrScreen.HOME
    }

    val unlockLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) vaultLocked = false
    }

    DisposableEffect(lifecycleOwner, settings.appLockEnabled, settings.autoLock) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> if (settings.appLockEnabled) backgroundedAt = System.currentTimeMillis()
                Lifecycle.Event.ON_START -> {
                    if (backgroundedAt > 0L && settings.appLockEnabled) {
                        val elapsed = System.currentTimeMillis() - backgroundedAt
                        val lockAfterMillis = when (settings.autoLock) {
                            "Immediately" -> 0L
                            "After 30 seconds" -> 30_000L
                            "After 5 minutes" -> 300_000L
                            else -> 60_000L
                        }
                        if (elapsed >= lockAfterMillis) vaultLocked = true
                    }
                    backgroundedAt = 0L
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val updateSettings: (LockrSettingsState) -> Unit = { updated ->
        if (updated.appLockEnabled && !keyguardManager.isDeviceSecure) {
            Toast.makeText(activity, "Set a device screen lock before enabling App Lock.", Toast.LENGTH_LONG).show()
            activity?.startActivity(Intent(AndroidSettings.ACTION_SECURITY_SETTINGS))
        } else {
            settings = updated
            preferences.edit()
                .putBoolean("app_lock_enabled", updated.appLockEnabled)
                .putString("auto_lock", updated.autoLock)
                .putString("reveal_timeout", updated.revealTimeout)
                .putBoolean("auto_clear_clipboard", updated.autoClearClipboard)
                .putBoolean("hide_in_recents", updated.hideInRecents)
                .putString("launcher_style", updated.launcherStyle)
                .apply()
            activity?.window?.let { window ->
                if (updated.hideInRecents) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    LaunchedEffect(settings.launcherStyle) {
        val packageManager = activity?.packageManager ?: return@LaunchedEffect
        val stashAlias = ComponentName(context, "${context.packageName}.StashLauncher")
        val calculatorAlias = ComponentName(context, "${context.packageName}.CalculatorLauncher")
        if (settings.launcherStyle == "Calculator") {
            packageManager.setComponentEnabledSetting(calculatorAlias, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED, android.content.pm.PackageManager.DONT_KILL_APP)
            packageManager.setComponentEnabledSetting(stashAlias, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED, android.content.pm.PackageManager.DONT_KILL_APP)
        } else {
            packageManager.setComponentEnabledSetting(stashAlias, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED, android.content.pm.PackageManager.DONT_KILL_APP)
            packageManager.setComponentEnabledSetting(calculatorAlias, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED, android.content.pm.PackageManager.DONT_KILL_APP)
        }
    }

    LaunchedEffect(settings.hideInRecents) {
        activity?.window?.let { window ->
            if (settings.hideInRecents) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            backupStatus = "Creating encrypted backup…"
            coroutineScope.launch {
                try {
                    val encryptedBytes = withContext(Dispatchers.IO) {
                        store.save(items.toList())
                        store.exportEncryptedBackup()
                    }
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(encryptedBytes) }
                            ?: throw IllegalStateException("Could not open the selected file")
                    }
                    encryptedBytes.fill(0)
                    backupStatus = "Encrypted backup saved. It can be restored on this device."
                } catch (_: Exception) {
                    backupStatus = "Backup could not be saved. Your vault is unchanged."
                }
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            backupStatus = "Checking backup…"
            coroutineScope.launch {
                try {
                    val candidate = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { readBoundedBytes(it, MAX_VAULT_FILE_BYTES) }
                            ?: throw IllegalStateException("Could not read the selected file")
                    }
                    val decoded = withContext(Dispatchers.IO) { store.validateEncryptedBackup(candidate) }
                    restoreBytes = candidate
                    restoreItemCount = decoded.size
                    backupStatus = null
                } catch (error: Exception) {
                    backupStatus = if (error.message?.contains("64 MB") == true) "This backup is larger than 64 MB." else "This backup is invalid or was created on another device."
                }
            }
        }
    }

    LaunchedEffect(store) {
        try {
            val storedItems = withContext(Dispatchers.IO) { store.loadOrNull() }
            if (storedItems != null) items.addAll(storedItems)
            storageReady = true
        } catch (_: Exception) {
            storageError = true
            storageReady = true
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (!onboardingComplete) {
            OnboardingScreen(
                onFinish = {
                    preferences.edit().putBoolean("onboarding_complete", true).apply()
                    onboardingComplete = true
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            VaultBackground()
            if (vaultLocked) {
                VaultLockScreen(onUnlock = {
                    val unlockIntent = keyguardManager.createConfirmDeviceCredentialIntent(
                        "Unlock Stash",
                        "Confirm your device screen lock to open your vault."
                    )
                    if (unlockIntent != null) unlockLauncher.launch(unlockIntent)
                    else {
                        updateSettings(settings.copy(appLockEnabled = false))
                        vaultLocked = false
                    }
                })
            } else Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                if (!storageReady) {
                    LocalVaultMessage("Opening your local vault…", "Your records stay on this device.")
                } else if (storageError) {
                    LocalVaultMessage("Vault could not be opened", "The encrypted local file was left untouched. Check that this device still has its original app key.")
                } else when (screen) {
                    LockrScreen.HOME -> HomeScreen(
                        items = items,
                        onOpenCards = { screen = LockrScreen.CARDS },
                        onOpenDocuments = { screen = LockrScreen.DOCUMENTS },
                        onOpenSettings = { screen = LockrScreen.SETTINGS },
                        onOpenAllItems = { screen = LockrScreen.ALL_ITEMS },
                        onOpenProfile = { screen = LockrScreen.PROFILE },
                        onAdd = { showAdd = true },
                        modifier = Modifier.weight(1f)
                    )
                    LockrScreen.CARDS -> CardsScreen(
                        items = items,
                        autoClearClipboard = settings.autoClearClipboard,
                        revealTimeout = settings.revealTimeout,
                        onBack = { screen = LockrScreen.HOME },
                        onAdd = { showAdd = true },
                        modifier = Modifier.weight(1f)
                    )
                    LockrScreen.SETTINGS -> SettingsScreen(
                        settings = settings,
                        onSettingsChange = updateSettings,
                        onBack = { screen = LockrScreen.HOME },
                        onExportBackup = { exportBackupLauncher.launch("Stash-Encrypted-Backup.stash") },
                        onImportBackup = { importBackupLauncher.launch(arrayOf("*/*")) },
                        backupStatus = backupStatus,
                        modifier = Modifier.weight(1f)
                    )
                    LockrScreen.PROFILE -> ProfileScreen(
                        items = items,
                        onBack = { screen = LockrScreen.HOME },
                        onOpenSettings = { screen = LockrScreen.SETTINGS },
                        onOpenAllItems = { screen = LockrScreen.ALL_ITEMS },
                        modifier = Modifier.weight(1f)
                    )
                    else -> CategoryScreen(
                        screen = screen,
                        items = items,
                        revealTimeout = settings.revealTimeout,
                        onBack = { screen = LockrScreen.HOME },
                        onAdd = { showAdd = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (showAdd) {
            ModalBottomSheet(
                onDismissRequest = { showAdd = false },
                sheetState = sheetState,
                containerColor = Color(0xFFF9FAFA),
                dragHandle = null
            ) {
                AddItemSheet(
                    isSaving = savingItem,
                    saveError = addSaveError,
                    onCancel = { showAdd = false },
                    onSave = { title, category, detail, metadata, attachments, lastFour ->
                        val updatedItems = items.toList().toMutableList().apply {
                            add(0, VaultItem(title, category, detail, "Just now", lastFour, metadata, attachments))
                        }
                        coroutineScope.launch {
                            savingItem = true
                            addSaveError = null
                            try {
                                withContext(Dispatchers.IO) { store.save(updatedItems) }
                                items.clear()
                                items.addAll(updatedItems)
                                showAdd = false
                            } catch (error: Exception) {
                                addSaveError = if (error.message?.contains("64 MB") == true) {
                                    "The encrypted vault is full. Remove a large attachment before saving."
                                } else {
                                    "Could not save this item. Your vault was left unchanged."
                                }
                            } finally {
                                savingItem = false
                            }
                        }
                    }
                )
            }
        }

        restoreBytes?.let { bytes ->
            AlertDialog(
                onDismissRequest = { restoreBytes = null; bytes.fill(0) },
                title = { Text("Restore this backup?") },
                text = { Text("Replace your current vault with $restoreItemCount saved items from this encrypted file? Backups work only on the device that created them.", color = LockrColors.TextSecondary) },
                dismissButton = {
                    TextButton(onClick = { restoreBytes = null; bytes.fill(0) }) { Text("Cancel") }
                },
                confirmButton = {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            try {
                                val restored = withContext(Dispatchers.IO) { store.restoreEncryptedBackup(bytes) }
                                items.clear()
                                items.addAll(restored)
                                backupStatus = "Restored ${restored.size} vault items."
                            } catch (_: Exception) {
                                backupStatus = "Restore failed. Your current vault was not replaced."
                            } finally {
                                bytes.fill(0)
                                restoreBytes = null
                            }
                        }
                    }) { Text("Restore") }
                }
            )
        }
    }
}

@Composable
private fun LocalVaultMessage(title: String, message: String) {
    Column(Modifier.fillMaxSize().padding(horizontal = 32.dp), verticalArrangement = Arrangement.Center) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(message, color = LockrColors.TextSecondary, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun VaultLockScreen(onUnlock: () -> Unit) {
    Box(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassPanel(Modifier.fillMaxWidth(), radius = 28.dp) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.size(82.dp), contentAlignment = Alignment.Center) {
                    IridescentOrb(orbSize = 78.dp)
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color(0xFF477C9C), modifier = Modifier.size(31.dp))
                }
                Text("Stash is locked", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 15.dp))
                Text("Unlock with your device screen lock to return to your vault.", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                Button(onClick = onUnlock, modifier = Modifier.fillMaxWidth().padding(top = 19.dp)) {
                    Icon(Icons.Outlined.Fingerprint, contentDescription = null, modifier = Modifier.size(19.dp))
                    Text("Unlock Stash", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun AddItemSheet(
    isSaving: Boolean,
    saveError: String?,
    onCancel: () -> Unit,
    onSave: (String, VaultCategory, String, Map<String, String>, List<VaultAttachment>, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(VaultCategory.CARD) }
    var cardType by remember { mutableStateOf("Credit") }
    var documentType by remember { mutableStateOf("Other") }
    var cardNumber by remember { mutableStateOf("") }
    var cardholder by remember { mutableStateOf("") }
    var issuer by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var securityCode by remember { mutableStateOf("") }
    var documentNumber by remember { mutableStateOf("") }
    var holderName by remember { mutableStateOf("") }
    var authority by remember { mutableStateOf("") }
    var issueDate by remember { mutableStateOf("") }
    var documentExpiry by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showSecurityCode by remember { mutableStateOf(false) }
    val attachments = remember { mutableStateListOf<VaultAttachment>() }
    var attachmentStatus by remember { mutableStateOf<String?>(null) }
    var attachmentBusy by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val attachmentScope = rememberCoroutineScope()

    suspend fun importAttachments(uris: List<Uri>) {
        attachmentBusy = true
        attachmentStatus = null
        try {
            val availableSlots = MAX_VAULT_ATTACHMENT_COUNT - attachments.size
            if (availableSlots <= 0) {
                attachmentStatus = "This item already has the maximum of 20 attachments."
                return
            }
            if (uris.size > availableSlots) attachmentStatus = "Up to 20 attachments are allowed per item."
            for (uri in uris.take(availableSlots)) {
                val remainingBytes = MAX_VAULT_TOTAL_ATTACHMENT_BYTES - attachments.sumOf { it.byteCount }
                if (remainingBytes <= 0L) {
                    attachmentStatus = "Attachments are limited to 24 MB per item."
                    break
                }
                try {
                    val added = readVaultAttachment(context, uri, minOf(MAX_VAULT_ATTACHMENT_BYTES, remainingBytes))
                    attachments.add(added)
                    if (title.isBlank()) title = added.name.substringBeforeLast('.', added.name).take(80)
                } catch (error: Exception) {
                    attachmentStatus = error.message ?: "This file could not be added."
                    break
                }
            }
        } finally {
            attachmentBusy = false
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = MAX_VAULT_ATTACHMENT_COUNT)
    ) { uris ->
        if (uris.isNotEmpty()) attachmentScope.launch { importAttachments(uris) }
    }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) attachmentScope.launch { importAttachments(uris) }
    }

    val isIdCard = cardType == "ID Card"
    val cleanCardNumber = cardNumber.filter(Char::isDigit)
    val validCardNumber = if (isIdCard) cardNumber.filter { it.isLetterOrDigit() || it == '-' }.length >= 3
        else cleanCardNumber.length in 12..19
    val validSecurityCode = securityCode.isBlank() || securityCode.filter(Char::isDigit).length in 3..4
    val validExpiry = expiry.isBlank() || Regex("^(0[1-9]|1[0-2])/\\d{2}$").matches(expiry)
    val hasAttachment = attachments.isNotEmpty()
    val canSave = title.isNotBlank() && !isSaving && !attachmentBusy && when (category) {
        VaultCategory.CARD -> (if (cardNumber.isBlank()) hasAttachment else validCardNumber) && (isIdCard || (validSecurityCode && validExpiry))
        VaultCategory.DOCUMENT -> documentNumber.isNotBlank() || hasAttachment
        VaultCategory.PASSWORD -> password.isNotBlank() || hasAttachment
        VaultCategory.NOTE -> notes.isNotBlank() || hasAttachment
    }

    Column(
        Modifier.fillMaxWidth().fillMaxHeight(0.96f).imePadding().padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFDDF4F0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(vaultTypeIcon(category), contentDescription = null, tint = Color(0xFF318D86), modifier = Modifier.size(23.dp))
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("Add to Stash", style = MaterialTheme.typography.titleLarge)
                Text("Private · encrypted on this device", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onCancel, enabled = !isSaving) { Text("Cancel") }
        }

        Column(
            Modifier.weight(1f).fillMaxWidth().padding(top = 15.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                VaultCategory.entries.forEach { option ->
                    Column(
                        Modifier.weight(1f).height(64.dp).clip(RoundedCornerShape(15.dp))
                            .background(if (category == option) Color(0xDDE3D9FA) else Color.White.copy(alpha = 0.72f))
                            .border(1.dp, if (category == option) Color(0xFFB9A8EB) else Color.White, RoundedCornerShape(15.dp))
                            .clickable { category = option }.padding(horizontal = 2.dp, vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(vaultTypeIcon(option), contentDescription = null, tint = Color(0xFF527D9F), modifier = Modifier.size(19.dp))
                        Text(option.label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1)
                    }
                }
            }

            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.78f))
                    .border(1.dp, Color.White, RoundedCornerShape(20.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text("Photos & files", style = MaterialTheme.typography.titleSmall)
                Text("Choose photos or files from your device", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        enabled = !attachmentBusy && attachments.size < MAX_VAULT_ATTACHMENT_COUNT,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Gallery", modifier = Modifier.padding(start = 6.dp))
                    }
                    OutlinedButton(
                        onClick = { fileLauncher.launch(arrayOf("*/*")) },
                        enabled = !attachmentBusy && attachments.size < MAX_VAULT_ATTACHMENT_COUNT,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Files", modifier = Modifier.padding(start = 6.dp))
                    }
                }
                if (attachmentBusy) Text("Adding to your encrypted vault…", color = LockrColors.Accent, style = MaterialTheme.typography.bodySmall)
                attachments.forEach { attachment ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Color(0xBFEAF2FA)).padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(if (attachment.mimeType.startsWith("image/")) Icons.Outlined.Image else Icons.Outlined.Description, contentDescription = null, tint = Color(0xFF557FA8), modifier = Modifier.size(18.dp))
                        Text(attachment.name, modifier = Modifier.weight(1f).padding(start = 8.dp), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                        Text(formatAttachmentSize(attachment.byteCount), color = LockrColors.TextSecondary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 7.dp))
                        TextButton(onClick = { attachments.remove(attachment) }, enabled = !isSaving) { Text("Remove") }
                    }
                }
                attachmentStatus?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }

            VaultInputField(
                value = title,
                onValueChange = { title = it },
                label = when (category) {
                    VaultCategory.CARD -> "Card nickname *"
                    VaultCategory.DOCUMENT -> "Document name *"
                    VaultCategory.PASSWORD -> "Account name *"
                    VaultCategory.NOTE -> "Note title *"
                },
                placeholder = when (category) {
                    VaultCategory.CARD -> "e.g. Everyday Visa"
                    VaultCategory.DOCUMENT -> "e.g. Passport"
                    VaultCategory.PASSWORD -> "e.g. Email account"
                    VaultCategory.NOTE -> "e.g. Wi-Fi details"
                },
                capitalization = KeyboardCapitalization.Words
            )

            when (category) {
                VaultCategory.CARD -> {
                    Text("CARD TYPE", color = LockrColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("Credit", "Debit", "ID Card").forEach { option ->
                            FilterChip(selected = cardType == option, onClick = { cardType = option }, label = { Text(option) })
                        }
                    }
                    VaultInputField(
                        value = cardNumber,
                        onValueChange = { raw ->
                            cardNumber = if (isIdCard) raw.filter { it.isLetterOrDigit() || it == '-' }.take(32)
                            else raw.filter(Char::isDigit).take(19)
                        },
                        label = if (isIdCard) "ID number${if (hasAttachment) " (optional)" else " *"}" else "Card number${if (hasAttachment) " (optional)" else " *"}",
                        placeholder = if (isIdCard) "Enter the number on your ID" else "Enter the number printed on your card",
                        keyboardType = if (isIdCard) KeyboardType.Text else KeyboardType.Number,
                        isError = cardNumber.isNotBlank() && !validCardNumber
                    )
                    if (cardNumber.isNotBlank() && !validCardNumber) {
                        Text(if (isIdCard) "Enter at least 3 letters or digits." else "Enter a card number with 12–19 digits.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    if (isIdCard) {
                        VaultInputField(value = cardholder, onValueChange = { cardholder = it }, label = "Name on ID (optional)", capitalization = KeyboardCapitalization.Words)
                        VaultInputField(value = issuer, onValueChange = { issuer = it }, label = "Issuing authority (optional)")
                        VaultInputField(value = expiry, onValueChange = { expiry = it }, label = "Expiry date (optional)", placeholder = "e.g. 08/29")
                    } else {
                        VaultInputField(value = cardholder, onValueChange = { cardholder = it }, label = "Name on card (optional)", capitalization = KeyboardCapitalization.Words)
                        VaultInputField(value = expiry, onValueChange = { raw ->
                            val digits = raw.filter(Char::isDigit).take(4)
                            expiry = if (digits.length > 2) "${digits.take(2)}/${digits.drop(2)}" else digits
                        }, label = "Expiry date (optional)", placeholder = "MM/YY", keyboardType = KeyboardType.Number)
                        if (!validExpiry) Text("Use MM/YY, for example 08/29.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        VaultInputField(
                            value = securityCode,
                            onValueChange = { securityCode = it.filter(Char::isDigit).take(4) },
                            label = "Security code (optional)",
                            placeholder = "CVV / CVC",
                            keyboardType = KeyboardType.NumberPassword,
                            visualTransformation = if (showSecurityCode) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showSecurityCode = !showSecurityCode }) {
                                    Icon(if (showSecurityCode) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = if (showSecurityCode) "Hide security code" else "Show security code")
                                }
                            },
                            isError = !validSecurityCode
                        )
                        VaultInputField(value = issuer, onValueChange = { issuer = it }, label = "Bank or issuer (optional)")
                    }
                    VaultInputField(value = notes, onValueChange = { notes = it }, label = "Notes (optional)", singleLine = false, minLines = 3)
                }
                VaultCategory.DOCUMENT -> {
                    Text("DOCUMENT TYPE", color = LockrColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("Passport", "National ID", "Driver license", "Other").forEach { option ->
                            FilterChip(selected = documentType == option, onClick = { documentType = option }, label = { Text(option) })
                        }
                    }
                    VaultInputField(value = documentNumber, onValueChange = { documentNumber = it }, label = if (hasAttachment) "Document number (optional)" else "Document number *", placeholder = "Enter the number on the document")
                    VaultInputField(value = holderName, onValueChange = { holderName = it }, label = "Name on document (optional)", capitalization = KeyboardCapitalization.Words)
                    VaultInputField(value = authority, onValueChange = { authority = it }, label = "Country or issuing authority (optional)")
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        VaultInputField(value = issueDate, onValueChange = { issueDate = it }, label = "Issued (optional)", placeholder = "DD/MM/YYYY", modifier = Modifier.weight(1f))
                        VaultInputField(value = documentExpiry, onValueChange = { documentExpiry = it }, label = "Expires (optional)", placeholder = "DD/MM/YYYY", modifier = Modifier.weight(1f))
                    }
                    VaultInputField(value = notes, onValueChange = { notes = it }, label = "Notes (optional)", singleLine = false, minLines = 3)
                }
                VaultCategory.PASSWORD -> {
                    VaultInputField(value = website, onValueChange = { website = it }, label = "Website or app (optional)", placeholder = "example.com", keyboardType = KeyboardType.Uri)
                    VaultInputField(value = username, onValueChange = { username = it }, label = "Username or email (optional)", keyboardType = KeyboardType.Email)
                    VaultInputField(
                        value = password,
                        onValueChange = { password = it },
                        label = if (hasAttachment) "Password (optional)" else "Password *",
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = if (showPassword) "Hide password" else "Show password")
                            }
                        }
                    )
                    VaultInputField(value = notes, onValueChange = { notes = it }, label = "Notes (optional)", singleLine = false, minLines = 3)
                }
                VaultCategory.NOTE -> VaultInputField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = if (hasAttachment) "Your note (optional)" else "Your note *",
                    placeholder = "Write the details you want to keep private…",
                    singleLine = false,
                    minLines = 5
                )
            }
            Text("Your entry is encrypted and stays on this device.", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 3.dp, bottom = 8.dp))
        }

        saveError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
        }
        if (!canSave && !isSaving) {
            val requiredHint = when (category) {
                VaultCategory.CARD -> if (isIdCard) "Add a nickname and ID number, or attach a file." else "Add a nickname and card number, or attach a file."
                VaultCategory.DOCUMENT -> "Add a document name and number, or attach a file."
                VaultCategory.PASSWORD -> "Add an account name and password, or attach a file."
                VaultCategory.NOTE -> "Add a title and note text, or attach a file."
            }
            Text(requiredHint, color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        }
        Button(
            onClick = {
                if (!canSave) return@Button
                val detail = when (category) {
                    VaultCategory.CARD -> cardType
                    VaultCategory.DOCUMENT -> documentType
                    VaultCategory.PASSWORD -> "Login"
                    VaultCategory.NOTE -> "Note"
                }
                val metadata = when (category) {
                    VaultCategory.CARD -> if (isIdCard) mapOf(
                        "idNumber" to cardNumber,
                        "holderName" to cardholder,
                        "issuer" to issuer,
                        "expiry" to expiry,
                        "notes" to notes
                    ) else mapOf(
                        "cardNumber" to cleanCardNumber,
                        "cardholder" to cardholder,
                        "issuer" to issuer,
                        "expiry" to expiry,
                        "securityCode" to securityCode,
                        "notes" to notes
                    )
                    VaultCategory.DOCUMENT -> mapOf(
                        "documentType" to documentType,
                        "documentNumber" to documentNumber,
                        "holderName" to holderName,
                        "authority" to authority,
                        "issueDate" to issueDate,
                        "expiry" to documentExpiry,
                        "notes" to notes
                    )
                    VaultCategory.PASSWORD -> mapOf(
                        "website" to website,
                        "username" to username,
                        "password" to password,
                        "notes" to notes
                    )
                    VaultCategory.NOTE -> mapOf("content" to notes)
                }.filterValues(String::isNotBlank)
                val lastFour = if (category == VaultCategory.CARD && !isIdCard) cleanCardNumber.takeLast(4) else ""
                onSave(title.trim(), category, detail, metadata, attachments.toList(), lastFour)
            },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp)
        ) { Text(if (isSaving) "Saving to your vault…" else "Save to vault") }
    }
}

private fun vaultTypeIcon(category: VaultCategory): ImageVector = when (category) {
    VaultCategory.CARD -> Icons.Outlined.CreditCard
    VaultCategory.DOCUMENT -> Icons.Outlined.Description
    VaultCategory.PASSWORD -> Icons.Outlined.Key
    VaultCategory.NOTE -> Icons.AutoMirrored.Outlined.Article
}

@Composable
private fun VaultInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { text -> ({ Text(text) }) },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = capitalization),
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        isError = isError,
        singleLine = singleLine,
        minLines = minLines
    )
}

private fun readBoundedBytes(input: InputStream, maximumBytes: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    try {
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (output.size() + count > maximumBytes) throw IOException("The selected backup is larger than 64 MB.")
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    } finally {
        buffer.fill(0)
    }
}
