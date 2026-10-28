package com.lockr.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun IdentityCardEditor(originalAttachments: List<VaultAttachment>, onBack: () -> Unit,
    onSave: (String, Map<String,String>, List<VaultAttachment>) -> Unit,
    existing: VaultItem? = null, rawSuggestion: String = "", saving: Boolean = false,
    locked: Boolean = false, onUnlock: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var type by remember { mutableStateOf(existing?.let(::identityType) ?: recognizeIdentity(rawSuggestion)?.type ?: IdentityType.AADHAAR) }
    var fields by remember { mutableStateOf(existing?.metadata.orEmpty().toMutableMap().apply {
        if(get("idNumber").isNullOrBlank()) put("idNumber",get("cardNumber").orEmpty())
    }.toMap()) }
    var sources by remember { mutableStateOf(existing?.let(::identityOriginals) ?: originalAttachments) }
    val typeDrafts=remember { mutableMapOf<IdentityType,Map<String,String>>() }
    var assets by remember { mutableStateOf(IdentityAsset.entries.mapNotNull { role -> existing?.let { identityAsset(it,role) }?.let { role to it } }.toMap()) }
    var tags by remember { mutableStateOf(existing?.let(::cardTags).orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var attempted by remember { mutableStateOf(false) }
    var back by remember { mutableStateOf(false) }
    var cropRole by remember { mutableStateOf<IdentityAsset?>(null) }
    var cropSource by remember { mutableStateOf<VaultAttachment?>(null) }
    var chooseRole by remember { mutableStateOf<IdentityAsset?>(null) }
    val ocrEnabled = LocalOcrOnImport.current
    val editorView=androidx.compose.ui.platform.LocalView.current
    val dark=LocalDarkTheme.current
    SideEffect {
        (editorView.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window?.let { window ->
            androidx.core.view.WindowInsetsControllerCompat(window,window.decorView).apply {
                isAppearanceLightStatusBars=!dark;isAppearanceLightNavigationBars=!dark
            }
        }
    }
    fun mergeSuggestion(raw: String) {
        recognizeIdentity(raw)?.let { suggestion ->
            if(fields["idNumber"].isNullOrBlank()) type=suggestion.type
            fields=fields.toMutableMap().apply { suggestion.fields.forEach { (k,v) -> if(get(k).isNullOrBlank()) put(k,v) } }
            status="Details suggested on this device. Check every field against the original."
        } ?: run { status="No supported ID detected. Choose a type and enter your details." }
    }
    suspend fun readSuggestions(files: List<VaultAttachment>) {
        val raw=buildString {
            files.forEach { file ->
                try { appendLine(recognizeAttachmentText(context,file)) }
                catch(cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch(_: Exception) { /* Manual entry and original remain available. */ }
            }
        }
        mergeSuggestion(raw)
    }
    LaunchedEffect(Unit) {
        if(existing==null) {
            if(rawSuggestion.isNotBlank()) mergeSuggestion(rawSuggestion)
            else if(sources.isNotEmpty() && ocrEnabled) {
                busy=true
                try { readSuggestions(sources) } finally { busy=false }
            }
        }
    }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if(uris.isEmpty()) busy=false
        else scope.launch {
            try {
                val added=mutableListOf<VaultAttachment>()
                var bytes=(sources+assets.values).sumOf { it.byteCount }
                val slots=remainingDocumentSlots(sources.size+assets.size)
                for(uri in uris.take(slots)) {
                    val file=readVaultAttachment(context,uri,MAX_VAULT_TOTAL_ATTACHMENT_BYTES-bytes)
                    added+=file;bytes+=file.byteCount
                }
                validateDocumentPages(sources+added+assets.values)
                sources=sources+added
                status=if(uris.size>slots) "Some files were omitted: the limit is 20 files / 24 MB." else "Originals added. You can add your portrait below."
            } catch(cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch(failure: Exception) { status=failure.message ?: "Could not add these files." }
            finally { busy=false }
        }
    }
    val portraitPicker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if(uri==null) busy=false
        else scope.launch {
            try {
                val photo=importIdentityPortrait(context,uri)
                val next=assets+(IdentityAsset.PHOTO to photo)
                validateDocumentPages(sources+next.values)
                assets=next;status="Photo added to your card."
            } catch(cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch(failure: Exception) { status=failure.message ?: "Could not add this photo." }
            finally { busy=false }
        }
    }
    val metadata=identityMetadata(type,fields,assets,tags)
    val title=identityTitle(type,fields)
    val preview=VaultItem(title,VaultCategory.CARD,"ID Card","",lastFour=metadata["lastFour"].orEmpty(),metadata=metadata,attachments=sources+assets.values,id="identity-draft")
    val numberError=identityNumberError(type,fields["idNumber"].orEmpty())
    val dateKeys=setOf("dateOfBirth","issueDate","validTill","transportValidTill")
    val dateErrors=identityFields(type).filter { it.key in dateKeys }.mapNotNull { field -> identityDateError(fields[field.key].orEmpty())?.let { field.key to it } }.toMap()
    val nameError=fields["cardholder"].isNullOrBlank()
    val blocked=busy || saving
    if(locked) {
        androidx.activity.compose.BackHandler { onBack() }
        Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally) {
            Text("Your edit is kept privately",style=MaterialTheme.typography.titleLarge)
            Text("Unlock to continue editing after leaving Stash.",modifier=Modifier.padding(vertical=16.dp))
            Button(onClick=onUnlock) {Text("Unlock to continue editing")}
            TextButton(onClick=onBack) {Text("Cancel changes")}
        }
        return
    }
    androidx.activity.compose.BackHandler { if(!blocked) onBack() }
    Box(Modifier.fillMaxSize().padding(horizontal=20.dp)) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top=88.dp),verticalArrangement=Arrangement.spacedBy(13.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                    IdentityType.entries.forEach { choice -> FilterChip(selected=type==choice,enabled=!blocked,onClick={
                        if(choice!=type) { typeDrafts[type]=fields;fields=typeDrafts[choice] ?: newIdentityTypeFields(choice,fields);type=choice }
                        attempted=false
                    },label={Text(choice.label)}) }
                }
                IdentityCardTemplate(preview,showPrivate=true,backSide=back)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    FilterChip(!back,onClick={back=false},label={Text("Front")})
                    FilterChip(back,onClick={back=true},label={Text("Back")})
                }
                Text("Your personal wallet view · originals kept separately",style=MaterialTheme.typography.bodySmall,color=LockrColors.TextSecondary)
                identityFields(type).forEach { field ->
                    val error=when(field.key) {
                        "idNumber" -> numberError
                        "cardholder" -> if(nameError) "Enter the name on the original." else null
                        else -> dateErrors[field.key]
                    }
                    OutlinedTextField(value=fields[field.key].orEmpty(),onValueChange={ value ->
                        fields=fields+(field.key to if(field.key=="idNumber") normalizeIdentityNumber(type,value) else value.take(if(field.multiline) 800 else 100))
                    },enabled=!blocked,label={Text(field.label)},singleLine=!field.multiline,minLines=if(field.multiline) 2 else 1,
                        isError=attempted && error!=null,supportingText=if(attempted && error!=null) ({Text(error)}) else if(field.key in dateKeys) ({Text("DD/MM/YYYY or YYYY · leave blank if unavailable")}) else null,
                        keyboardOptions=KeyboardOptions(capitalization=if(field.key=="idNumber") KeyboardCapitalization.Characters else KeyboardCapitalization.Words,
                            keyboardType=if(field.key=="idNumber" && type==IdentityType.AADHAAR) KeyboardType.Number else KeyboardType.Text),
                        shape=androidx.compose.foundation.shape.RoundedCornerShape(17.dp),modifier=Modifier.fillMaxWidth())
                }
                Text("Originals & card images",style=MaterialTheme.typography.titleMedium)
                OutlinedButton(enabled=!blocked && sources.size+assets.size<20,onClick={busy=true;picker.launch(arrayOf("image/*","application/pdf"))},modifier=Modifier.fillMaxWidth()) { Text("Add front / back scans") }
                sources.forEachIndexed { i,file ->
                    Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                        Text("${i+1}. ${file.name}",maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,modifier=Modifier.weight(1f),style=MaterialTheme.typography.bodySmall)
                        TextButton(enabled=!blocked,onClick={sources=sources.filterIndexed { index,_ -> index!=i }}) { Text("Remove") }
                    }
                }
                if(sources.isNotEmpty()) OutlinedButton(enabled=!blocked,onClick={busy=true;scope.launch { try { readSuggestions(sources) } finally { busy=false } }},modifier=Modifier.fillMaxWidth()) { Text("Read details from scans · offline") }
                OutlinedButton(enabled=!blocked && (assets.containsKey(IdentityAsset.PHOTO) || sources.size+assets.size<20),
                    onClick={busy=true;portraitPicker.launch("image/*")},modifier=Modifier.fillMaxWidth()) {
                    Text(if(assets[IdentityAsset.PHOTO]==null) "Choose your photo" else "Replace your photo")
                }
                listOf(IdentityAsset.PHOTO,IdentityAsset.SIGNATURE).forEach { role ->
                    Surface(shape=androidx.compose.foundation.shape.RoundedCornerShape(17.dp),color=MaterialTheme.colorScheme.surface.copy(alpha=.8f)) {
                        Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                            assets[role]?.let { IdentityAssetImage(it,role.label,Modifier.size(48.dp)) }
                            Column(Modifier.weight(1f).padding(start=9.dp)) {
                                Text(role.label,style=MaterialTheme.typography.titleSmall)
                                Text(if(assets[role]==null) "Crop from an original scan" else "From your original",style=MaterialTheme.typography.bodySmall,color=LockrColors.TextSecondary)
                            }
                            TextButton(enabled=!blocked && sources.any { it.mimeType.startsWith("image/") || isPdfAttachment(it) },onClick={chooseRole=role}) { Text(if(assets[role]==null) "Add" else "Change") }
                            if(assets[role]!=null) TextButton(enabled=!blocked,onClick={assets=assets-role}) { Text("Clear") }
                        }
                    }
                }
                Text("The small QR links to venusapp.in and contains no personal data.",style=MaterialTheme.typography.bodySmall,color=LockrColors.TextSecondary)
                TagEditor(VaultCategory.CARD,tags,{tags=it})
                if(status.isNotBlank()) Text(status,style=MaterialTheme.typography.bodySmall,color=LockrColors.TextSecondary)
                if(attempted && (nameError || numberError!=null || dateErrors.isNotEmpty())) Text("Check the highlighted fields before saving.",color=MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
            }
            if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Button(enabled=!blocked,onClick={
                attempted=true
                if(!nameError && numberError==null && dateErrors.isEmpty()) {
                    try {
                        val files=sources+assets.values
                        if(files.isNotEmpty()) validateDocumentPages(files)
                        onSave(title,metadata,files)
                    } catch(failure: Exception) { status=failure.message ?: "The files could not be saved." }
                }
            },modifier=Modifier.fillMaxWidth().padding(vertical=10.dp).height(54.dp)) { Text(if(saving) "Saving…" else "Save identity card") }
        }
        StashTopBar(if(existing==null) "Add identity card" else "Edit identity card",subtitle="Private · encrypted · available offline",onBack={if(!blocked) onBack()})
    }
    chooseRole?.let { role -> AlertDialog(onDismissRequest={chooseRole=null},title={Text("Choose original for ${role.label.replaceFirstChar(Char::lowercase)}")},text={
        Column(Modifier.heightIn(max=340.dp).verticalScroll(rememberScrollState())) {
            sources.filter { it.mimeType.startsWith("image/") || isPdfAttachment(it) }.forEach { file -> TextButton(onClick={cropRole=role;cropSource=file;chooseRole=null}) { Text(file.name) } }
        }
    },confirmButton={TextButton(onClick={chooseRole=null}) {Text("Cancel")}}) }
    val source=cropSource;val role=cropRole
    if(source!=null && role!=null) IdentityAssetCropSheet(source,role,{cropSource=null;cropRole=null}) { cropped ->
        val next=assets+(role to cropped)
        validateDocumentPages(sources+next.values)
        assets=next;cropSource=null;cropRole=null
    }
}
