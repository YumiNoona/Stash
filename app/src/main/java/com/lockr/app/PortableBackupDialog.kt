package com.lockr.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
internal fun PortableBackupDialog(export: Boolean, onDismiss: () -> Unit, onConfirm: (CharArray) -> Unit) {
    var passphrase by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { passphrase = ""; confirmation = ""; onDismiss() },
        title = { Text(if (export) "Portable backup" else "Open portable backup") },
        text = {
            Column {
                Text(if (export) "Choose a passphrase you can remember. This encrypted file can restore your vault on another device. Stash cannot recover a forgotten passphrase."
                    else "Enter the passphrase used when this backup was created. Restoring will replace your current vault.")
                OutlinedTextField(passphrase, { passphrase = it; error = "" }, label = { Text("Passphrase") },
                    visualTransformation = PasswordVisualTransformation(), singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                if (export) OutlinedTextField(confirmation, { confirmation = it; error = "" },
                    label = { Text("Confirm passphrase") }, visualTransformation = PasswordVisualTransformation(),
                    singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                if (error.isNotBlank()) Text(error, color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = { TextButton(onClick = {
            error = when {
                passphrase.length < 12 -> "Use at least 12 characters."
                export && passphrase != confirmation -> "Passphrases do not match."
                else -> ""
            }
            if (error.isBlank()) {
                val secret = passphrase.toCharArray()
                passphrase = ""; confirmation = ""
                onConfirm(secret)
            }
        }) { Text(if (export) "Choose location" else "Unlock backup") } },
        dismissButton = { TextButton(onClick = { passphrase = ""; confirmation = ""; onDismiss() }) { Text("Cancel") } }
    )
}
