package com.lockr.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
        StashTopBar("Privacy policy", onBack = onBack)
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PolicySection("Stash by Veil", "Updated 1 November 2026. Stash is a local Android vault. It has no account, ads, analytics, server, cloud sync or internet permission.")
            PolicySection("What you choose to store", "You may add cards, identity details, documents, photos, passwords, notes, profile details and custom artwork. Camera, selected files and NFC are used only when you choose those features. Text recognition runs on your device.")
            PolicySection("Storage and protection", "Vault records and attachments are encrypted with AES-GCM in app-private storage. A device-bound key is held by Android Keystore. Profile name, photo and display settings are kept separately in app-private storage. A portable backup uses a passphrase-derived key. Veil cannot access or recover your vault or a forgotten backup passphrase.")
            PolicySection("Sharing and exports", "Stash sends data to another app only when you choose Share, save a file, or export. The receiving app or location then controls that copy. Summary exports omit names, free-form details and file names. Full readable exports may contain private data and are not encrypted. Card verification codes are never included in readable exports.")
            PolicySection("Retention and deletion", "Your data remains on your device until you delete it or uninstall Stash. Uninstalling deletes the local key and vault. User-created exports and shared copies outside Stash must be removed separately. Temporary share files in Stash's cache are removed after about ten minutes or on a later app launch. Android automatic cloud and device-transfer backup are disabled.")
            PolicySection("Contact", "For privacy questions, email Veil at rushikeshingale2001@gmail.com")
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = LockrColors.TextPrimary)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = LockrColors.TextSecondary)
    }
}
