package com.lockr.app

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.Date
import java.util.Locale

class MainActivity : androidx.activity.ComponentActivity() {
    private var widgetRequest by mutableStateOf<Pair<String, Long>?>(null)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        widgetRequest = intent.getStringExtra("widget_target")?.let { it to System.nanoTime() }
    }
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (android.os.Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        widgetRequest = intent.getStringExtra("widget_target")?.let { it to System.nanoTime() }
        setContent { LockrTheme { LockrApp(widgetRequest) { widgetRequest = null } } }
    }
}

enum class LockrScreen { HOME, ALL_ITEMS, CARDS, DOCUMENTS, PASSWORDS, NOTES, SETTINGS, PROFILE, SEARCH, SCANNER, NFC_CARD, IMPORTING, REVIEW, CARD_REVIEW, MANUAL, ITEM_DETAIL, ITEM_PREVIEW }

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
    val attachments: List<VaultAttachment> = emptyList(),
    val id: String = java.util.UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis()
)

data class VaultAttachment(
    val name: String,
    val mimeType: String,
    val byteCount: Long,
    val base64Data: String
)

private const val MAX_IMPORT_ITEMS = 20

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LockrApp(widgetRequest: Pair<String, Long>?, consumeWidget: () -> Unit) {
    // Drafts and selected items live in memory; do not restore a route without its data.
    var screen by remember { mutableStateOf(LockrScreen.HOME) }
    var returnScreen by rememberSaveable { mutableStateOf(LockrScreen.HOME) }
    var returnFromProfile by rememberSaveable { mutableStateOf(LockrScreen.HOME) }
    var returnFromSettings by rememberSaveable { mutableStateOf(LockrScreen.HOME) }
    var returnFromSearch by rememberSaveable { mutableStateOf(LockrScreen.HOME) }
    var returnFromDetail by rememberSaveable { mutableStateOf(LockrScreen.HOME) }
    var returnFromPreview by rememberSaveable { mutableStateOf(LockrScreen.HOME) }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var addStage by rememberSaveable { mutableStateOf(AddStage.CHOOSE) }
    var savingItem by remember { mutableStateOf(false) }
    var storageReady by remember { mutableStateOf(false) }
    var storageError by remember { mutableStateOf(false) }
    var pendingAuthentication by remember { mutableStateOf<String?>(null) }
    var authorizedItem by remember { mutableStateOf<String?>(null) }
    var currentDetailItem by remember { mutableStateOf<VaultItem?>(null) }
    var manualCategory by remember { mutableStateOf(VaultCategory.PASSWORD) }
    var importCategoryHint by remember { mutableStateOf<VaultCategory?>(null) }
    var cardDraft by remember { mutableStateOf(CardDraft()) }
    var importedAttachments by remember { mutableStateOf<List<VaultAttachment>>(emptyList()) }
    var backupStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var restoreBytes by remember { mutableStateOf<ByteArray?>(null) }
    var restoreItemCount by rememberSaveable { mutableStateOf(0) }

    val activity = LocalActivity.current as? MainActivity
    val context = activity?.applicationContext ?: LocalContext.current.applicationContext
    val preferences = remember(context) { context.getSharedPreferences("lockr_preferences", Context.MODE_PRIVATE) }
    var vaultLocked by remember {
        mutableStateOf(preferences.getBoolean("app_lock_enabled", false))
    }
    var onboardingComplete by rememberSaveable { mutableStateOf(preferences.getBoolean("onboarding_complete", false)) }
    val tagCatalog = remember(preferences) { VaultTagCatalog(preferences) }
    val store = remember(context) { LocalVaultStore(context) }
    val profileStore = remember(context) { LocalProfileStore(context) }
    var profileName by remember { mutableStateOf(profileStore.name()) }
    var profilePhoto by remember { mutableStateOf(profileStore.photo()) }
    var pendingExport by remember { mutableStateOf<Pair<DataExportFormat, Boolean>?>(null) }
    val items = remember { mutableStateListOf<VaultItem>() }
    var settings by remember(context) {
        mutableStateOf(
            LockrSettingsState(
                appLockEnabled = preferences.getBoolean("app_lock_enabled", false),
                autoLock = preferences.getString("auto_lock", "After 1 minute") ?: "After 1 minute",
                revealTimeout = preferences.getString("reveal_timeout", "30 seconds") ?: "30 seconds",
                hideCardNumber = preferences.getBoolean("hide_card_number", true),
                autoClearClipboard = preferences.getBoolean("auto_clear_clipboard", true),
                hideInRecents = preferences.getBoolean("hide_in_recents", false)
            )
        )
    }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    tagCatalog.onDelete = { category, tag ->
        if (!savingItem && storageReady && !storageError) {
            savingItem = true
            tagCatalog.deleting = true
            coroutineScope.launch {
                try {
                    val updated = items.map { withoutTag(it, category, tag) }
                    withContext(Dispatchers.IO) { store.save(updated) }
                    items.clear(); items.addAll(updated)
                    currentDetailItem = currentDetailItem?.let { withoutTag(it, category, tag) }
                    tagCatalog.remove(category, tag)
                } catch (_: Exception) {
                    snackbarHostState.showSnackbar("Could not remove this tag. Try again.")
                } finally { savingItem = false; tagCatalog.deleting = false }
            }
        }
    }
    LaunchedEffect(items.map { it.category to it.metadata }) {
        items.forEach { item ->
            if (item.category == VaultCategory.CARD) cardTags(item).forEach { tagCatalog.add(VaultCategory.CARD, it) }
            if (item.category == VaultCategory.DOCUMENT) item.metadata["documentCategory"]?.let { tagCatalog.add(VaultCategory.DOCUMENT, it) }
        }
    }
    val keyguardManager = remember(context) { context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager }
    val lifecycleOwner = LocalLifecycleOwner.current
    var backgroundedAt by remember { mutableStateOf(0L) }

    val unlockLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (vaultLocked) vaultLocked = false
            else authorizedItem = pendingAuthentication
        }
        pendingAuthentication = null
    }

    BackHandler(enabled = vaultLocked) { }
    BackHandler(enabled = !vaultLocked && (screen == LockrScreen.IMPORTING || savingItem)) { }
    BackHandler(
        enabled = !vaultLocked && !savingItem && screen != LockrScreen.IMPORTING && !showAdd && restoreBytes == null && screen != LockrScreen.HOME
    ) {
        when (screen) {
            LockrScreen.ITEM_DETAIL -> {
                if (returnFromDetail != LockrScreen.ITEM_PREVIEW) currentDetailItem = null
                authorizedItem = null
                screen = returnFromDetail
            }
            LockrScreen.ITEM_PREVIEW -> { currentDetailItem = null; authorizedItem = null; screen = returnFromPreview }
            LockrScreen.REVIEW, LockrScreen.CARD_REVIEW -> { importedAttachments = emptyList(); cardDraft = CardDraft(); screen = returnScreen; showAdd = true }
            LockrScreen.SCANNER -> { screen = returnScreen; showAdd = true }
            LockrScreen.NFC_CARD -> { screen = returnScreen; showAdd = true }
            LockrScreen.MANUAL -> { screen = returnScreen; showAdd = true }
            LockrScreen.SETTINGS -> screen = returnFromSettings
            LockrScreen.PROFILE -> screen = returnFromProfile
            LockrScreen.SEARCH -> screen = returnFromSearch
            else -> screen = LockrScreen.HOME
        }
    }

    DisposableEffect(lifecycleOwner, settings.appLockEnabled, settings.autoLock) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    authorizedItem = null
                    if (settings.appLockEnabled) backgroundedAt = System.currentTimeMillis()
                }
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
            android.widget.Toast.makeText(activity, "Set a device screen lock before enabling App Lock.", android.widget.Toast.LENGTH_LONG).show()
            activity?.startActivity(Intent(AndroidSettings.ACTION_SECURITY_SETTINGS))
        } else {
            settings = updated
            preferences.edit()
                .putBoolean("app_lock_enabled", updated.appLockEnabled)
                .putString("auto_lock", updated.autoLock)
                .putString("reveal_timeout", updated.revealTimeout)
                .putBoolean("hide_card_number", updated.hideCardNumber)
                .putBoolean("auto_clear_clipboard", updated.autoClearClipboard)
                .putBoolean("hide_in_recents", updated.hideInRecents)
                .apply()
            activity?.window?.let { window ->
                if (updated.hideInRecents) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    LaunchedEffect(settings.hideInRecents) {
        activity?.window?.let { window ->
            if (settings.hideInRecents) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
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
                    backupStatus = "Encrypted backup saved. This device's key is needed to restore it."
                } catch (_: Exception) {
                    backupStatus = "Backup could not be saved. Your vault is unchanged."
                }
            }
        }
    }
    val exportDataLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val request = pendingExport
        pendingExport = null
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null && request != null) {
            backupStatus = "Creating ${request.first.label} export…"
            coroutineScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            writeReadableExport(output, items.toList(), request.first, request.second)
                        } ?: throw IllegalStateException("Could not open destination")
                    }
                    backupStatus = "${request.first.label} export saved."
                } catch (_: Exception) {
                    backupStatus = "Export failed. Your vault is unchanged."
                }
            }
        }
    }
    val importBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) coroutineScope.launch {
            backupStatus = "Checking backup…"
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

    val beginImport: (List<Uri>, VaultCategory?) -> Unit = { uris, categoryHint ->
        showAdd = false
        if (uris.isEmpty()) {
            showAdd = true
        } else {
            screen = LockrScreen.IMPORTING
            importCategoryHint = categoryHint
            coroutineScope.launch {
                val imported = mutableListOf<VaultAttachment>()
                var totalBytes = 0L
                var failure: String? = null
                for (uri in uris.take(MAX_IMPORT_ITEMS)) {
                    val remaining = MAX_VAULT_TOTAL_ATTACHMENT_BYTES - totalBytes
                    if (remaining <= 0) { failure = "The 24 MB per-item limit was reached."; break }
                    try {
                        val attachment = readVaultAttachment(context, uri, minOf(MAX_VAULT_ATTACHMENT_BYTES, remaining))
                        imported += attachment
                        totalBytes += attachment.byteCount
                    } catch (error: Exception) {
                        failure = error.message ?: "One of the selected files could not be opened."
                        break
                    }
                }
                if (imported.isEmpty()) {
                    screen = returnScreen
                    snackbarHostState.showSnackbar(failure ?: "No files were selected.")
                } else {
                    importedAttachments = imported
                    screen = if (categoryHint == VaultCategory.CARD) LockrScreen.CARD_REVIEW else LockrScreen.REVIEW
                    if (failure != null) snackbarHostState.showSnackbar(failure)
                }
            }
        }
    }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems = MAX_IMPORT_ITEMS)) { uris ->
        beginImport(uris, importCategoryHint)
    }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        beginImport(uris, importCategoryHint)
    }
    val profilePhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) coroutineScope.launch {
            try { profilePhoto = withContext(Dispatchers.IO) { profileStore.savePhoto(uri) } }
            catch (_: Exception) { snackbarHostState.showSnackbar("Could not save that profile photo.") }
        }
    }

    val beginCameraImport: (List<File>, VaultCategory?) -> Unit = { files, categoryHint ->
        screen = LockrScreen.IMPORTING
        importCategoryHint = categoryHint
        coroutineScope.launch {
            val imported = mutableListOf<VaultAttachment>()
            var totalBytes = 0L
            var failure: String? = null
            try {
                for (file in files.take(MAX_IMPORT_ITEMS)) {
                    val remaining = MAX_VAULT_TOTAL_ATTACHMENT_BYTES - totalBytes
                    if (remaining <= 0) { failure = "The 24 MB per-item limit was reached."; break }
                    val attachment = readFileVaultAttachment(file, minOf(MAX_VAULT_ATTACHMENT_BYTES, remaining))
                    imported += attachment
                    totalBytes += attachment.byteCount
                }
                if (imported.isNotEmpty()) {
                    importedAttachments = imported
                    screen = if (categoryHint == VaultCategory.CARD) LockrScreen.CARD_REVIEW else LockrScreen.REVIEW
                    if (failure != null) snackbarHostState.showSnackbar(failure)
                } else {
                    screen = returnScreen
                    snackbarHostState.showSnackbar(failure ?: "The scan could not be opened.")
                }
            } catch (error: Exception) {
                screen = returnScreen
                snackbarHostState.showSnackbar(error.message ?: "The scan could not be opened.")
            } finally {
                files.forEach(File::delete)
            }
        }
    }

    val requestAuthentication: () -> Unit = {
        val prompt = keyguardManager.createConfirmDeviceCredentialIntent("Reveal private information", "Confirm your device screen lock to reveal this value.")
        if (prompt == null) {
            Toast.makeText(activity, "Set a screen lock in Android Settings to reveal private values.", Toast.LENGTH_LONG).show()
        } else {
            pendingAuthentication = currentDetailItem?.id
            unlockLauncher.launch(prompt)
        }
    }

    LaunchedEffect(store) {
        withContext(Dispatchers.IO) { clearSharedAttachmentCache(context) }
        try {
            val storedItems = withContext(Dispatchers.IO) { store.loadOrNull() }
            if (storedItems != null) items.addAll(storedItems)
            storageReady = true
        } catch (_: Exception) {
            storageError = true
            storageReady = true
        }
    }

    val persistNewItem: (VaultItem) -> Unit = save@{ item ->
        if (savingItem) return@save
        savingItem = true
        coroutineScope.launch {
            try {
                val lastFolder = preferences.getString("last_folder", "All folders").orEmpty().takeIf { it in preferences.getStringSet("vault_folders", setOf("School", "Personal")).orEmpty() }.orEmpty()
                val target = if (item.category == VaultCategory.DOCUMENT && "folder" !in item.metadata)
                    item.copy(metadata = item.metadata + ("folder" to lastFolder)) else item
                val updatedItems = items.toList().toMutableList().apply { add(0, uniqueItem(target, items.toList())) }
                withContext(Dispatchers.IO) { store.save(updatedItems) }
                items.clear()
                items.addAll(updatedItems)
                importedAttachments = emptyList()
                cardDraft = CardDraft()
                screen = when (item.category) {
                    VaultCategory.CARD -> LockrScreen.CARDS
                    VaultCategory.DOCUMENT -> LockrScreen.DOCUMENTS
                    VaultCategory.PASSWORD -> LockrScreen.PASSWORDS
                    VaultCategory.NOTE -> LockrScreen.NOTES
                }
                snackbarHostState.showSnackbar("Saved to ${item.category.label.lowercase()}s")
            } catch (error: Exception) {
                snackbarHostState.showSnackbar(if (error.message?.contains("64 MB") == true) "Your encrypted vault is full. Remove a large file before saving." else "Could not save this item. Your vault was left unchanged.")
            } finally {
                savingItem = false
            }
        }
    }

    val persistUpdatedItem: (VaultItem) -> Unit = update@{ updated ->
        if (savingItem) return@update
        savingItem = true
        coroutineScope.launch {
            val safeUpdate = uniqueItem(updated, items.toList())
            val updatedItems = items.map { if (it.id == updated.id) safeUpdate else it }
            try {
                withContext(Dispatchers.IO) { store.save(updatedItems) }
                items.clear(); items.addAll(updatedItems)
                currentDetailItem = safeUpdate
                snackbarHostState.showSnackbar("Item updated")
            } catch (_: Exception) {
                snackbarHostState.showSnackbar("Could not update this item. Your vault was left unchanged.")
            } finally {
                savingItem = false
            }
        }
    }
    val openItem: (VaultItem, LockrScreen) -> Unit = { item, origin ->
        currentDetailItem = item
        authorizedItem = null
        returnFromDetail = origin
        if (item.category !in setOf(VaultCategory.CARD, VaultCategory.PASSWORD) &&
            item.attachments.firstOrNull()?.let { it.mimeType.startsWith("image/") || it.mimeType == "application/pdf" } == true) {
            returnFromPreview = origin
            screen = LockrScreen.ITEM_PREVIEW
        } else {
            returnFromDetail = origin
            screen = LockrScreen.ITEM_DETAIL
        }
    }
    val deleteItem: (VaultItem) -> Unit = delete@{ deleting ->
        if (savingItem) return@delete
        savingItem = true
        val updatedItems = items.filterNot { it.id == deleting.id }
        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) { store.save(updatedItems) }
                items.clear(); items.addAll(updatedItems)
                currentDetailItem = null
                screen = if (screen == LockrScreen.ITEM_PREVIEW || returnFromDetail == LockrScreen.ITEM_PREVIEW) returnFromPreview else returnFromDetail
                snackbarHostState.showSnackbar("Item deleted")
            } catch (_: Exception) {
                snackbarHostState.showSnackbar("Could not delete this item. Your vault was left unchanged.")
            } finally {
                savingItem = false
            }
        }
    }

    var folders by remember { mutableStateOf(preferences.getStringSet("vault_folders", setOf("School", "Personal"))!!.toList().sorted()) }
    LaunchedEffect(items.toList()) {
        val recovered = (folders + items.mapNotNull { it.metadata["folder"]?.takeIf(String::isNotBlank) }).distinct().sorted()
        if (recovered != folders) { folders = recovered; preferences.edit().putStringSet("vault_folders", recovered.toSet()).apply() }
    }
    var downloadItems by remember { mutableStateOf<List<VaultItem>>(emptyList()) }
    val downloadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val selected = downloadItems; downloadItems = emptyList()
        if (uri != null) coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { writeDocumentDownload(context, selected, it) } ?: error("Cannot open destination") }
                snackbarHostState.showSnackbar("Download saved")
            } catch (_: Exception) { snackbarHostState.showSnackbar("Could not save the download. Choose another destination.") }
        }
    }
    val batchSave: (List<VaultItem>, String, List<String>?) -> Unit = batch@{ replacement, message, newFolders ->
        if (savingItem) return@batch
        savingItem = true
        val removed = items.filter { old -> replacement.none { it.id == old.id } }
        coroutineScope.launch {
            var success = false
            try {
                withContext(Dispatchers.IO) { store.save(replacement) }
                items.clear(); items.addAll(replacement)
                currentDetailItem = currentDetailItem?.let { current -> replacement.firstOrNull { it.id == current.id } }
                if (newFolders != null) { folders = newFolders.distinct().sorted(); preferences.edit().putStringSet("vault_folders", folders.toSet()).apply() }
                success = true
            } catch (_: Exception) { /* The original vault remains intact. */ }
            finally { savingItem = false }
            if (!success) snackbarHostState.showSnackbar("Could not save. Your vault was left unchanged.")
            else {
                val result = snackbarHostState.showSnackbar(message, actionLabel = if (removed.isNotEmpty()) "Undo" else null)
                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed && !savingItem) {
                    savingItem = true
                    var restoredSuccessfully = false
                    try {
                        val restored = items.toMutableList()
                        removed.filter { old -> restored.none { it.id == old.id } }.forEach { restored += uniqueItem(it, restored) }
                        withContext(Dispatchers.IO) { store.save(restored) }
                        items.clear(); items.addAll(restored)
                        restoredSuccessfully = true
                    } catch (_: Exception) { /* Keep the saved vault intact. */ } finally { savingItem = false }
                    if (!restoredSuccessfully) snackbarHostState.showSnackbar("Could not restore these items.")
                }
            }
        }
    }
    val actions = VaultActions(
        move = { ids, folder ->
            val changed = mutableListOf<VaultItem>()
            items.filterNot { it.id in ids }.forEach { changed += it }
            items.filter { it.id in ids }.forEach { item -> changed += uniqueItem(item.copy(metadata = item.metadata + ("folder" to folder)), changed) }
            val order = items.mapIndexed { index, item -> item.id to index }.toMap()
            batchSave(changed.sortedBy { order[it.id] }, "Moved ${ids.size} items", if (folder.isNotBlank()) folders + folder else folders)
        },
        delete = { ids -> batchSave(items.filterNot { it.id in ids }, "Deleted ${ids.size} items", null) },
        download = { selected ->
            val documents = selected.filter { it.attachments.isNotEmpty() && it.category != VaultCategory.CARD && it.category != VaultCategory.PASSWORD }
            if (documents.isNotEmpty()) {
                downloadItems = documents
                downloadLauncher.launch(documentDownloadName(documents))
            }
        },
        folderChanged = { old, new ->
            val renamed = mutableListOf<VaultItem>()
            val unaffected = items.filterNot { old.isNotEmpty() && it.metadata["folder"] == old }
            renamed.addAll(unaffected)
            items.filter { old.isNotEmpty() && it.metadata["folder"] == old }.forEach { item -> renamed += uniqueItem(item.copy(metadata = item.metadata + ("folder" to new.orEmpty())), renamed) }
            val order = items.mapIndexed { index, item -> item.id to index }.toMap()
            batchSave(renamed.sortedBy { order[it.id] }, "Folders updated", (folders - old) + listOfNotNull(new))
        })
    var widgetOpening by remember { mutableStateOf<String?>(null) }
    val motionAllowed = LocalMotionEnabled.current
    val theme = LocalDarkTheme.current
    LaunchedEffect(items.toList(), theme, settings.appLockEnabled) {
        preferences.edit().putInt("widget_cards_count", items.count { it.category == VaultCategory.CARD })
            .putInt("widget_documents_count", items.count { it.category == VaultCategory.DOCUMENT }).apply()
        updateWidgets(context)
    }
    LaunchedEffect(widgetRequest, vaultLocked, storageReady, onboardingComplete, storageError) {
        if (widgetRequest != null && !vaultLocked && storageReady && onboardingComplete && !storageError) {
            val target = widgetRequest.first
            widgetOpening = target
            if (motionAllowed) kotlinx.coroutines.delay(950)
            screen = if (target == "documents") LockrScreen.DOCUMENTS else LockrScreen.CARDS
            showAdd = false
            widgetOpening = null
            consumeWidget()
        } else widgetOpening = null
    }
    CompositionLocalProvider(LocalHideCardNumber provides settings.hideCardNumber, LocalTagCatalog provides tagCatalog, LocalVaultActions provides actions, LocalVaultFolders provides folders) {
    Box(Modifier.fillMaxSize()) {
        if (!onboardingComplete) {
            OnboardingScreen(
                onFinish = { preferences.edit().putBoolean("onboarding_complete", true).apply(); onboardingComplete = true },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            VaultBackground()
            if (vaultLocked) {
                VaultLockScreen(onUnlock = {
                    val prompt = keyguardManager.createConfirmDeviceCredentialIntent("Unlock Stash", "Confirm your device screen lock to open your vault.")
                    if (prompt != null) unlockLauncher.launch(prompt)
                    else {
                        updateSettings(settings.copy(appLockEnabled = false))
                        vaultLocked = false
                    }
                })
            } else Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                if (!storageReady) {
                    LocalVaultMessage("Opening your local vault…", "Your records stay on this device.")
                } else if (storageError) {
                    LocalVaultMessage("Vault could not be opened", "The encrypted file was left untouched. This device needs its original app key.")
                } else Box(Modifier.fillMaxSize()) {
                    when (screen) {
                    LockrScreen.HOME -> HomeScreen(
                        items = items,
                        profileName = profileName,
                        profilePhoto = profilePhoto,
                        onOpenSettings = { returnFromSettings = LockrScreen.HOME; screen = LockrScreen.SETTINGS },
                        onOpenAllItems = { screen = LockrScreen.ALL_ITEMS },
                        onOpenProfile = { returnFromProfile = LockrScreen.HOME; screen = LockrScreen.PROFILE },
                        onOpenItem = { item -> openItem(item, LockrScreen.HOME) },
                        onOpenDetails = { item -> currentDetailItem = item; returnFromDetail = LockrScreen.HOME; screen = LockrScreen.ITEM_DETAIL }
                    )
                    LockrScreen.CARDS -> CardsScreen(items = items, onBack = { screen = LockrScreen.HOME }, onSearch = { returnFromSearch = LockrScreen.CARDS; screen = LockrScreen.SEARCH }, onOpenItem = { item -> openItem(item, LockrScreen.CARDS) })
                    LockrScreen.SETTINGS -> SettingsScreen(
                        settings = settings,
                        onSettingsChange = updateSettings,
                        onBack = { screen = returnFromSettings },
                        onExportBackup = { exportBackupLauncher.launch("Stash-Encrypted-Backup.stash") },
                        onExportData = { format, includePrivate ->
                            pendingExport = format to includePrivate
                            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = format.mimeType
                                putExtra(Intent.EXTRA_TITLE, "Stash-Export.${format.extension}")
                            }
                            exportDataLauncher.launch(intent)
                        },
                        onImportBackup = { importBackupLauncher.launch(arrayOf("*/*")) },
                        backupStatus = backupStatus
                    )
                    LockrScreen.PROFILE -> ProfileScreen(
                        name = profileName,
                        photo = profilePhoto,
                        itemCount = items.size,
                        onBack = { screen = returnFromProfile },
                        onChangeName = { name -> profileStore.saveName(name); profileName = profileStore.name() },
                        onChoosePhoto = { profilePhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        onRemovePhoto = { profileStore.removePhoto(); profilePhoto = null },
                        onOpenSettings = { returnFromSettings = LockrScreen.PROFILE; screen = LockrScreen.SETTINGS },
                        onOpenAllItems = { screen = LockrScreen.ALL_ITEMS }
                    )
                    LockrScreen.SEARCH -> VaultSearchScreen(
                        items,
                        onBack = { screen = returnFromSearch },
                        onOpenItem = { item -> openItem(item, LockrScreen.SEARCH) }
                    )
                    LockrScreen.SCANNER -> CameraScannerScreen(
                        category = importCategoryHint ?: VaultCategory.DOCUMENT,
                        onBack = { screen = returnScreen; showAdd = true },
                        onReviewPages = { files -> beginCameraImport(files, importCategoryHint) }
                    )
                    LockrScreen.NFC_CARD -> NfcImportScreen(
                        onBack = { screen = returnScreen; showAdd = true },
                        onRead = { draft -> cardDraft = draft; importedAttachments = emptyList(); screen = LockrScreen.CARD_REVIEW },
                        onScanCard = { importCategoryHint = VaultCategory.CARD; screen = LockrScreen.SCANNER },
                        onManual = { cardDraft = CardDraft(cardType = "Credit"); importedAttachments = emptyList(); screen = LockrScreen.CARD_REVIEW }
                    )
                    LockrScreen.IMPORTING -> ImportProgressScreen()
                    LockrScreen.REVIEW -> ReviewItemScreen(
                        attachments = importedAttachments,
                        onBack = { screen = returnScreen; showAdd = true },
                        onSave = { title, category, metadata, attachments ->
                            if (!savingItem) persistNewItem(VaultItem(title, category, metadata["cardType"].orEmpty(), nowLabel(), lastFour = metadata["lastFour"].orEmpty(), metadata = metadata, attachments = attachments))
                        }
                    )
                    LockrScreen.CARD_REVIEW -> CardReviewScreen(
                        saving = savingItem,
                        attachments = importedAttachments,
                        initial = cardDraft,
                        onBack = { importedAttachments = emptyList(); cardDraft = CardDraft(); screen = returnScreen; showAdd = true },
                        onSave = { title, metadata, attachments ->
                            if (!savingItem) persistNewItem(VaultItem(title, VaultCategory.CARD,
                                metadata["cardType"].orEmpty(), nowLabel(),
                                lastFour = metadata["lastFour"].orEmpty(), metadata = metadata,
                                attachments = attachments))
                        }
                    )
                    LockrScreen.MANUAL -> ManualItemScreen(
                        category = manualCategory,
                        onBack = { screen = returnScreen; showAdd = true },
                        onSave = { title, category, detail, metadata ->
                            if (!savingItem) persistNewItem(VaultItem(title, category, detail, nowLabel(), metadata = metadata))
                        }
                    )
                    LockrScreen.ITEM_DETAIL -> currentDetailItem?.let { item ->
                        if (item.category == VaultCategory.DOCUMENT) DocumentInfoScreen(item,
                            onBack = { if (returnFromDetail != LockrScreen.ITEM_PREVIEW) currentDetailItem = null; screen = returnFromDetail })
                        else VaultItemDetailScreen(
                            item = item,
                            autoClearClipboard = settings.autoClearClipboard,
                            revealTimeout = settings.revealTimeout,
                            authorized = authorizedItem == item.id,
                            onAuthenticate = requestAuthentication,
                            onAuthorizationExpired = { if (authorizedItem == item.id) authorizedItem = null },
                            onBack = { if (returnFromDetail != LockrScreen.ITEM_PREVIEW) currentDetailItem = null; authorizedItem = null; screen = returnFromDetail },
                            onEdit = persistUpdatedItem,
                            onDelete = deleteItem
                        )
                    } ?: LocalVaultMessage("Item unavailable", "Return to your vault and open the item again.")
                    LockrScreen.ITEM_PREVIEW -> currentDetailItem?.let { item ->
                        ItemPreviewScreen(
                            item = item,
                            onBack = { currentDetailItem = null; screen = returnFromPreview },
                            onRename = { name -> persistUpdatedItem(item.copy(title = name)) },
                            onUpdatePages = { pages -> persistUpdatedItem(item.copy(attachments = pages)) },
                            onSaveField = { key, value ->
                                persistUpdatedItem(item.copy(metadata = item.metadata + (key to value)))
                            },
                            autoClearClipboard = settings.autoClearClipboard,
                            onSend = { shareVaultAttachment(context, item.attachments.first()) },
                            onDelete = { deleteItem(item) },
                            onDetails = { returnFromDetail = LockrScreen.ITEM_PREVIEW; screen = LockrScreen.ITEM_DETAIL }
                        )
                    } ?: LocalVaultMessage("Item unavailable", "Return to your vault and open the item again.")
                    else -> CategoryScreen(
                        screen = screen,
                        items = items,
                        onBack = { screen = LockrScreen.HOME },
                        onSearch = { returnFromSearch = screen; screen = LockrScreen.SEARCH },
                        onOpenItem = { item -> openItem(item, screen) }
                    )
                    }
                }

                val showDock = screen in setOf(
                    LockrScreen.HOME, LockrScreen.CARDS, LockrScreen.DOCUMENTS, LockrScreen.PASSWORDS, LockrScreen.NOTES, LockrScreen.ALL_ITEMS
                )
                if (showDock) {
                    StashDock(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        selected = when (screen) {
                            LockrScreen.CARDS -> DockDestination.CARDS
                            LockrScreen.DOCUMENTS -> DockDestination.DOCUMENTS
                            else -> null
                        },
                        onCards = { screen = LockrScreen.CARDS },
                        onAdd = { returnScreen = screen; addStage = AddStage.CHOOSE; showAdd = true },
                        onDocuments = { screen = LockrScreen.DOCUMENTS }
                    )
                }
            }
        }

        if (!vaultLocked) widgetOpening?.let { WidgetOpening(it == "documents") }

        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(bottom = 82.dp))

        if (showAdd && !vaultLocked && storageReady && !storageError) {
            ModalBottomSheet(
                onDismissRequest = { showAdd = false },
                sheetState = sheetState,
                containerColor = LockrColors.Surface,
                dragHandle = null
            ) {
                AddSourceSheet(
                    stage = addStage,
                    onStageChange = { addStage = it },
                    onCamera = { category -> importCategoryHint = category; cardDraft = CardDraft(); showAdd = false; screen = LockrScreen.SCANNER },
                    onPhotos = { category ->
                        importCategoryHint = category; cardDraft = CardDraft(); showAdd = false
                        photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onFiles = { category ->
                        importCategoryHint = category; cardDraft = CardDraft(); showAdd = false
                        fileLauncher.launch(arrayOf("application/pdf", "image/*", "text/plain"))
                    },
                    onNfc = { showAdd = false; screen = LockrScreen.NFC_CARD },
                    onCardManual = { cardDraft = CardDraft(cardType = "Credit"); importedAttachments = emptyList(); showAdd = false; screen = LockrScreen.CARD_REVIEW },
                    onManual = { category -> manualCategory = category; showAdd = false; screen = LockrScreen.MANUAL }
                )
            }
        }

        restoreBytes?.takeIf { !vaultLocked }?.let { bytes ->
            AlertDialog(
                onDismissRequest = { restoreBytes = null; bytes.fill(0) },
                title = { Text("Restore this backup?") },
                text = { Text("Replace your current vault with $restoreItemCount items from this encrypted file? This device's key is required.", color = LockrColors.TextSecondary) },
                dismissButton = { TextButton(onClick = { restoreBytes = null; bytes.fill(0) }) { Text("Cancel") } },
                confirmButton = {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            try {
                                val restored = withContext(Dispatchers.IO) { store.restoreEncryptedBackup(bytes) }
                                items.clear(); items.addAll(restored)
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
}

@Composable
private fun ImportProgressScreen() {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(color = LockrColors.Accent)
        Text("Preparing your files", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 18.dp))
        Text("The selected files are copied into your encrypted local vault only after you save.", color = LockrColors.TextSecondary, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
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
    Box(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp), contentAlignment = Alignment.Center) {
        GlassPanel(Modifier.fillMaxWidth(), radius = 28.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(82.dp), contentAlignment = Alignment.Center) {
                    IridescentOrb(orbSize = 78.dp)
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color(0xFF477C9C), modifier = Modifier.size(31.dp))
                }
                Text("Stash is locked", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 15.dp))
                Text("Unlock with your device screen lock to return to your vault.", color = LockrColors.TextSecondary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                Button(onClick = onUnlock, modifier = Modifier.fillMaxWidth().padding(top = 19.dp)) {
                    Icon(Icons.Outlined.Fingerprint, contentDescription = null, modifier = Modifier.size(19.dp))
                    Text("Unlock Stash", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

private fun readBoundedBytes(input: InputStream, maximumBytes: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    var total = 0
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        total += count
        if (total > maximumBytes) throw java.io.IOException("The selected file is larger than 64 MB")
        output.write(buffer, 0, count)
    }
    buffer.fill(0)
    return output.toByteArray()
}

private fun nowLabel(): String = java.text.SimpleDateFormat("MMM d", Locale.getDefault()).format(Date())
