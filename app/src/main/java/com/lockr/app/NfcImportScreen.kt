package com.lockr.app

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun NfcImportScreen(
    onBack: () -> Unit,
    onRead: (CardDraft) -> Unit,
    onScanCard: () -> Unit,
    onManual: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val adapter = remember(context) { NfcAdapter.getDefaultAdapter(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val reading = remember { AtomicBoolean(false) }
    var message by remember { mutableStateOf("Hold a supported contactless card against the back of your phone.") }
    var working by remember { mutableStateOf(false) }
    var nfcEnabled by remember { mutableStateOf(adapter?.isEnabled == true) }

    DisposableEffect(activity, adapter, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (activity == null || adapter == null) return@LifecycleEventObserver
            if (event == Lifecycle.Event.ON_RESUME) {
                nfcEnabled = adapter.isEnabled
                if (nfcEnabled) {
                    try {
                        adapter.enableReaderMode(activity, { tag ->
                            if (!reading.compareAndSet(false, true)) return@enableReaderMode
                            scope.launch {
                                working = true
                                message = "Reading card…"
                                try {
                                    val draft = withContext(Dispatchers.IO) { readSupportedNfcCard(tag) }
                                    onRead(draft)
                                } catch (error: Exception) {
                                    message = error.message ?: "This card could not be read. Try a scan or manual entry."
                                    working = false
                                    reading.set(false)
                                }
                            }
                        }, NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null)
                    } catch (_: Exception) {
                        message = "NFC could not start. Scan or enter the card instead."
                    }
                }
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                try { adapter.disableReaderMode(activity) } catch (_: Exception) { }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            observer.onStateChanged(lifecycleOwner, Lifecycle.Event.ON_RESUME)
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (activity != null && adapter != null) {
                try { adapter.disableReaderMode(activity) } catch (_: Exception) { }
            }
        }
    }

    Box(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).padding(top = 88.dp).fillMaxWidth(), verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
                GlassPanel(Modifier.fillMaxWidth(), radius = 29.dp) {
                    Column(Modifier.fillMaxWidth().padding(27.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
                            IridescentOrb(orbSize = 100.dp)
                            Icon(Icons.Outlined.Nfc, contentDescription = null, tint = Color(0xFF132351), modifier = Modifier.size(45.dp))
                        }
                        Text(when {
                            adapter == null -> "NFC is unavailable here"
                            !nfcEnabled -> "Turn on NFC"
                            working -> "Reading card"
                            else -> "Ready to tap"
                        }, style = MaterialTheme.typography.titleLarge, color = LockrColors.TextPrimary,
                            modifier = Modifier.padding(top = 18.dp))
                        Text(when {
                            adapter == null -> "This device has no NFC reader. Use the camera or manual entry."
                            !nfcEnabled -> "Enable NFC in Android settings, then return here."
                            else -> message
                        }, style = MaterialTheme.typography.bodyMedium, color = LockrColors.TextSecondary,
                            modifier = Modifier.padding(top = 9.dp))
                        if (adapter != null && !nfcEnabled) {
                            OutlinedButton(onClick = {
                                context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                            }, modifier = Modifier.padding(top = 14.dp)) { Text("Open NFC settings") }
                        }
                    }
                }
                Text("Some banks block card details over NFC. Nothing is saved until you review and confirm.",
                    style = MaterialTheme.typography.bodySmall, color = LockrColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 18.dp))
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedButton(onClick = onScanCard, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Outlined.CameraAlt, null, modifier = Modifier.size(18.dp))
                    Text("Scan", modifier = Modifier.padding(start = 6.dp))
                }
                Button(onClick = onManual, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Outlined.CreditCard, null, modifier = Modifier.size(18.dp))
                    Text("Manual", modifier = Modifier.padding(start = 6.dp))
                }
            }

        }
        StashTopBar("Tap a card", "Supported contactless cards only", onBack = onBack)
    }
}
