package com.lockr.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

@Composable
fun IdentityCardDetail(item: VaultItem, authorized: Boolean, revealTimeout: String, autoClearClipboard: Boolean,
    onAuthenticate: () -> Unit, onAuthorizationExpired: () -> Unit, onBack: () -> Unit,
    onEdit: (VaultItem) -> Unit, onDelete: (VaultItem) -> Unit) {
    val type=identityType(item) ?: return
    val context=LocalContext.current
    var back by remember(item.id) { mutableStateOf(false) }
    var editing by remember(item.id) { mutableStateOf(false) }
    var delete by remember(item.id) { mutableStateOf(false) }
    var originals by remember(item.id) { mutableStateOf(false) }
    var qr by remember(item.id) { mutableStateOf(false) }
    var pending by remember(item.id) { mutableStateOf("") }
    var feedback by remember(item.id) { mutableStateOf("") }
    androidx.activity.compose.BackHandler(onBack=onBack)
    LaunchedEffect(authorized) {
        if(authorized) {
            when {
                pending=="edit" -> editing=true
                pending=="original" -> originals=true
                pending=="qr" -> qr=true
                pending.startsWith("copy:") -> {
                    val value=item.metadata[pending.removePrefix("copy:")].orEmpty()
                    if(value.isNotBlank()) {copyPrivateText(context,value,autoClearClipboard);feedback="Copied"}
                }
            }
            pending=""
        } else { originals=false;qr=false }
    }
    LaunchedEffect(authorized,revealTimeout,editing) {
        if(authorized && !editing) {
            delay(when { revealTimeout.startsWith("10") -> 10_000L;revealTimeout.startsWith("1 minute") -> 60_000L;revealTimeout.startsWith("5 minutes") -> 300_000L;else -> 30_000L })
            onAuthorizationExpired()
        }
    }
    val request: (String) -> Unit = { action ->
        if(!authorized) {pending=action;onAuthenticate()}
        else when(action) { "edit" -> editing=true;"original" -> originals=true;"qr" -> qr=true }
    }
    Box(Modifier.fillMaxSize().padding(horizontal=20.dp)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top=88.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            IdentityCardTemplate(item,showPrivate=authorized,backSide=back)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                FilterChip(!back,onClick={back=false},label={Text("Front")})
                FilterChip(back,onClick={back=true},label={Text("Back")})
                Spacer(Modifier.weight(1f))
                TextButton(onClick={if(authorized) onAuthorizationExpired() else request("reveal")}) {
                    Icon(Icons.Outlined.Visibility,null,Modifier.size(18.dp));Text(if(authorized) " Hide" else " Reveal")
                }
            }
            Text("Personal wallet view · refer to your original for official use",style=MaterialTheme.typography.bodySmall,color=LockrColors.TextSecondary)
            GlassPanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal=17.dp,vertical=6.dp)) {
                    identityFields(type).filter { !item.metadata[it.key].isNullOrBlank() }.forEach { field ->
                        Row(Modifier.fillMaxWidth().padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(field.label.removeSuffix(" *"),style=MaterialTheme.typography.labelSmall,color=LockrColors.TextSecondary)
                                Text(if(authorized || field.key in setOf("cardholder","localName","issuer")) item.metadata[field.key].orEmpty()
                                    else if(field.key=="idNumber") identityNumberText(type,item.metadata[field.key].orEmpty(),false) else "Hidden · unlock to view",
                                    style=MaterialTheme.typography.bodyMedium,color=LockrColors.TextPrimary)
                            }
                            IconButton(onClick={
                                if(authorized) {copyPrivateText(context,item.metadata[field.key].orEmpty(),autoClearClipboard);feedback="${field.label.removeSuffix(" *")} copied"}
                                else {pending="copy:${field.key}";onAuthenticate()}
                            }) {Icon(Icons.Outlined.ContentCopy,"Copy ${field.label.removeSuffix(" *")}",Modifier.size(20.dp))}
                        }
                        HorizontalDivider(color=LockrColors.Line.copy(alpha=.5f))
                    }
                }
            }
            if(identityAsset(item,IdentityAsset.QR)!=null) OutlinedButton(onClick={request("qr")},modifier=Modifier.fillMaxWidth()) {Text("Open original QR")}
            if(identityOriginals(item).isNotEmpty()) OutlinedButton(onClick={request("original")},modifier=Modifier.fillMaxWidth()) {Text("View original scans · ${identityOriginals(item).size}")}
            if(feedback.isNotBlank()) Text(feedback,style=MaterialTheme.typography.bodySmall,color=LockrColors.Accent)
        }
        StashTopBar(type.label,subtitle="Encrypted on this device",onBack=onBack,trailing={
            IconButton(onClick={request("edit")}) {Icon(Icons.Outlined.Edit,"Edit identity card")}
            IconButton(onClick={delete=true}) {Icon(Icons.Outlined.DeleteOutline,"Delete identity card")}
        })
    }
    if(editing) Dialog(onDismissRequest={editing=false},properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        Surface(Modifier.fillMaxSize()) { Box(Modifier.systemBarsPadding()) {
            VaultBackground()
            IdentityCardEditor(identityOriginals(item),{editing=false},{ title,metadata,files ->
                onEdit(item.copy(title=title,detail="ID Card",metadata=metadata+item.metadata.filterKeys { it=="folder" },attachments=files,lastFour=metadata["lastFour"].orEmpty()))
                editing=false
            },existing=item,locked=!authorized,onUnlock=onAuthenticate)
        } }
    }
    if(originals && authorized) AlertDialog(onDismissRequest={originals=false},title={Text("Original scans")},text={
        Column(Modifier.heightIn(max=480.dp).verticalScroll(rememberScrollState())) { VaultAttachmentsSection(identityOriginals(item)) }
    },confirmButton={TextButton(onClick={originals=false}) {Text("Close")}})
    if(qr && authorized) AlertDialog(onDismissRequest={qr=false},title={Text("QR from your original")},text={
        Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            identityAsset(item,IdentityAsset.QR)?.let { IdentityAssetImage(it,"Original QR",Modifier.fillMaxWidth().aspectRatio(1f)) }
            Text("Stored image · Stash has not verified its digital signature.",style=MaterialTheme.typography.bodySmall)
        }
    },confirmButton={TextButton(onClick={qr=false}) {Text("Close")}})
    if(delete) AlertDialog(onDismissRequest={delete=false},title={Text("Delete ${type.label}?")},text={Text("This removes the saved details, cropped images and original scans from Stash.")},
        confirmButton={TextButton(onClick={delete=false;onDelete(item)}) {Text("Delete")}},dismissButton={TextButton(onClick={delete=false}) {Text("Cancel")}})
}
