package com.lockr.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Issuer-inspired personal wallet view. The original remains the source of truth. */
@Composable
fun IdentityCardTemplate(item: VaultItem, modifier: Modifier = Modifier, showPrivate: Boolean = false,
                         backSide: Boolean = false, onClick: (() -> Unit)? = null) {
    val type = identityType(item) ?: IdentityType.OTHER
    val accent = when (type) {
        IdentityType.AADHAAR -> Color(0xFFB64732)
        IdentityType.PAN -> Color(0xFF235B9C)
        IdentityType.VOTER -> Color(0xFF297660)
        IdentityType.LICENCE -> Color(0xFF982D3D)
        IdentityType.OTHER -> Color(0xFF55547A)
    }
    val palette = when (type) {
        IdentityType.AADHAAR -> listOf(Color(0xFFFFFEF9), Color(0xFFF3F4EC), Color(0xFFFFF0E7))
        IdentityType.PAN -> listOf(Color(0xFFDEEEFA), Color(0xFFD9DFF3), Color(0xFFBFE5F2))
        IdentityType.VOTER -> listOf(Color(0xFFF7F2DC), Color(0xFFE7EFDD), Color(0xFFF1DFC6))
        IdentityType.LICENCE -> listOf(Color(0xFFFFF5EC), Color(0xFFF5E8D9), Color(0xFFEEE7DF))
        IdentityType.OTHER -> listOf(Color(0xFFF3EDFF), Color(0xFFE5EDF9), Color(0xFFDDEFEF))
    }
    val shape = RoundedCornerShape(17.dp)
    CompositionLocalProvider(androidx.compose.material3.LocalTextStyle provides androidx.compose.material3.LocalTextStyle.current.copy(lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified)) {
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(1.586f).clip(shape)
        .background(Brush.linearGradient(palette)).border(1.dp, accent.copy(alpha = .25f), shape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)) {
        val s = maxWidth.value / 360f
        val ink = Color(0xFF142738)
        fun field(key: String) = item.metadata[key].orEmpty().ifBlank { "—" }
        Canvas(Modifier.fillMaxSize()) {
            // Fine, understated print texture, intentionally without security motifs.
            repeat(14) { i ->
                val x = size.width * (.32f + i * .065f)
                drawLine(accent.copy(alpha = .035f), Offset(x, 0f), Offset(x - size.width * .3f, size.height), 1f)
            }
        }
        Column(Modifier.fillMaxSize().padding((13*s).dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy((8*s).dp)) {
                val logo = identityAsset(item, IdentityAsset.LOGO)
                if (logo != null) IdentityAssetImage(logo, "Issuer logo", Modifier.size((28*s).dp), ContentScale.Fit)
                else Box(Modifier.size((28*s).dp).clip(RoundedCornerShape((7*s).dp)).background(accent.copy(alpha = .1f)), contentAlignment = Alignment.Center) {
                    Text(when(type) { IdentityType.AADHAAR -> "IN"; IdentityType.PAN -> "PAN"; IdentityType.VOTER -> "ECI"; IdentityType.LICENCE -> "DL"; else -> "ID" }, color = accent,
                        fontWeight = FontWeight.Bold, fontSize = (8*s).sp, maxLines = 1)
                }
                Column(Modifier.weight(1f)) {
                    Text(when(type) { IdentityType.AADHAAR -> "AADHAAR"; IdentityType.PAN -> "INCOME TAX DEPARTMENT"; IdentityType.VOTER -> "ELECTION COMMISSION OF INDIA"; IdentityType.LICENCE -> "INDIAN UNION DRIVING LICENCE"; else -> field("issuer").takeIf { it != "—" } ?: "IDENTITY CARD" },
                        color = accent, fontSize = (10*s).sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(when(type) { IdentityType.AADHAAR -> "Unique Identification Authority of India"; IdentityType.PAN -> "Permanent Account Number Card"; IdentityType.VOTER -> "Elector Photo Identity Card"; IdentityType.LICENCE -> field("issuer"); else -> "Personal identity record" },
                        color = ink.copy(alpha = .7f), fontSize = (7*s).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height((7*s).dp))
            Row(Modifier.fillMaxWidth().height((2*s).dp)) {
                val colors = if(type == IdentityType.AADHAAR) listOf(Color(0xFFE49E50),Color.White,Color(0xFF609B7B)) else listOf(accent,accent.copy(alpha=.35f),accent.copy(alpha=.1f))
                colors.forEach { Box(Modifier.weight(1f).fillMaxHeight().background(it)) }
            }
            Spacer(Modifier.height((10*s).dp))
            if (backSide) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy((14*s).dp)) {
                    IdentityQr(item, showPrivate, Modifier.size((82*s).dp), s)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy((7*s).dp)) {
                        TemplateField("ADDRESS", if(showPrivate) field("address") else "Unlock to view address", s, lines = 3)
                        if(type == IdentityType.VOTER) TemplateField("CONSTITUENCY", if(showPrivate) field("constituency") else "Hidden", s)
                        if(type == IdentityType.LICENCE) {
                            TemplateField("VEHICLE CLASSES", if(showPrivate) field("vehicleClasses") else "Hidden", s)
                            TemplateField("TRANSPORT VALIDITY", if(showPrivate) field("transportValidTill") else "Hidden", s)
                        }
                        val signature = identityAsset(item, IdentityAsset.SIGNATURE)
                        if(signature != null && showPrivate) IdentityAssetImage(signature, "Signature from original", Modifier.height((24*s).dp).width((95*s).dp), ContentScale.Fit)
                    }
                }
            } else {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy((11*s).dp)) {
                    if(type != IdentityType.LICENCE) IdentityPortrait(item, Modifier.width((64*s).dp).height((84*s).dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy((6*s).dp)) {
                        TemplateField(if(type == IdentityType.VOTER) "ELECTOR'S NAME" else "NAME", field("cardholder"), s, bold = true, lines = 2)
                        if(!item.metadata["localName"].isNullOrBlank()) Text(field("localName"),fontSize=(8*s).sp,color=ink,maxLines=1,overflow=TextOverflow.Ellipsis)
                        if(type == IdentityType.PAN || type == IdentityType.VOTER) TemplateField(if(type == IdentityType.PAN) "PARENT'S NAME" else "RELATIVE'S NAME", if(showPrivate) field("relativeName") else "Hidden", s)
                        Row(horizontalArrangement = Arrangement.spacedBy((12*s).dp)) {
                            val age=type==IdentityType.VOTER && item.metadata["dateOfBirth"].isNullOrBlank() && !item.metadata["age"].isNullOrBlank()
                            TemplateField(if(age) "AGE" else if(item.metadata["dateOfBirth"]?.length==4) "YEAR OF BIRTH" else "DATE OF BIRTH", if(showPrivate) field(if(age) "age" else "dateOfBirth") else "••/••/••••", s, modifier = Modifier.weight(1f))
                            if(type == IdentityType.AADHAAR || type == IdentityType.VOTER || type == IdentityType.LICENCE) TemplateField(if(type == IdentityType.LICENCE) "BLOOD" else "GENDER", if(showPrivate) field(if(type == IdentityType.LICENCE) "bloodGroup" else "gender") else "—", s)
                        }
                        if(type == IdentityType.LICENCE) Row(horizontalArrangement = Arrangement.spacedBy((10*s).dp)) {
                            TemplateField("ISSUED", if(showPrivate) field("issueDate") else "Hidden", s, modifier = Modifier.weight(1f))
                            TemplateField("VALID UNTIL", if(showPrivate) field("validTill") else "Hidden", s, modifier = Modifier.weight(1f))
                        }
                        if(type==IdentityType.PAN && showPrivate) identityAsset(item,IdentityAsset.SIGNATURE)?.let {
                            IdentityAssetImage(it,"Signature from original",Modifier.width((80*s).dp).height((18*s).dp))
                        }
                    }
                    if(type == IdentityType.AADHAAR || type == IdentityType.PAN) IdentityQr(item, showPrivate, Modifier.size((60*s).dp), s)
                    if(type == IdentityType.LICENCE) IdentityPortrait(item, Modifier.width((64*s).dp).height((84*s).dp))
                }
            }
            Column {
                Text(if(backSide) type.label else identityNumberText(type, field("idNumber").takeUnless { it == "—" }.orEmpty(), showPrivate),
                    color = ink, fontSize = (16*s).sp, fontWeight = FontWeight.Bold, letterSpacing = (1*s).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height((5*s).dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("STASH · PERSONAL RECORD", color = accent, fontSize = (6*s).sp, fontWeight = FontWeight.Bold)
                    Text(if(backSide) "BACK" else type.numberLabel.uppercase(), color = ink.copy(alpha=.6f), fontSize = (6*s).sp)
                }
            }
        }
    }
}

}

@Composable
private fun TemplateField(label: String, value: String, s: Float, modifier: Modifier = Modifier, bold: Boolean = false, lines: Int = 1) {
    Column(modifier) {
        Text(label, fontSize = (5.5*s).sp, color = Color(0xFF516371), fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, fontSize = ((if(bold) 11f else 8f)*s).sp, color = Color(0xFF142738), fontWeight = if(bold) FontWeight.Bold else FontWeight.Medium,
            maxLines = lines, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun IdentityPortrait(item: VaultItem, modifier: Modifier) {
    val portrait = identityAsset(item, IdentityAsset.PHOTO)
    Box(modifier.clip(RoundedCornerShape(5.dp)).background(Color.White.copy(alpha=.7f)).border(.5.dp, Color(0xFFBCCCD4), RoundedCornerShape(5.dp)), contentAlignment = Alignment.Center) {
        if(portrait != null) IdentityAssetImage(portrait, "Portrait from original", Modifier.fillMaxSize(), ContentScale.Fit)
        else Icon(Icons.Outlined.PersonOutline, "No portrait added", tint = Color(0xFF97ABB9), modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun IdentityQr(item: VaultItem, visible: Boolean, modifier: Modifier, s: Float) {
    val qr = identityAsset(item, IdentityAsset.QR)
    Box(modifier.clip(RoundedCornerShape(4.dp)).background(Color.White), contentAlignment = Alignment.Center) {
        if(qr != null && visible) IdentityAssetImage(qr, "QR copied from original document", Modifier.fillMaxSize().padding((3*s).dp), ContentScale.Fit)
        else Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.QrCode2, null, tint = Color(0xFF96A8B2), modifier = Modifier.size((22*s).dp))
            Text(if(qr == null) "NO QR ADDED" else "UNLOCK QR", fontSize = (5*s).sp, color = Color(0xFF667E8C))
        }
    }
}

@Composable
internal fun IdentityAssetImage(file: VaultAttachment, description: String?, modifier: Modifier, scale: ContentScale = ContentScale.Fit) {
    val bitmap by produceState<android.graphics.Bitmap?>(null, file.base64Data) {
        value = withContext(Dispatchers.IO) { decodeAttachmentPreview(file.base64Data, 1200, 1200) }
    }
    bitmap?.let { Image(it.asImageBitmap(), description, modifier, contentScale = scale, filterQuality = androidx.compose.ui.graphics.FilterQuality.None) }
}
