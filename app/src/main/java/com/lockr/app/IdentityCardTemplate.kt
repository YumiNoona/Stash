package com.lockr.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val IDENTITY_WEBSITE_URL="https://venusapp.in"

/** Reference-matched raster faces; the user's information remains live text and encrypted media. */
@Composable
fun IdentityCardTemplate(item:VaultItem,modifier:Modifier=Modifier,showPrivate:Boolean=false,
                         backSide:Boolean=false,onClick:(()->Unit)?=null) {
    val type=identityType(item) ?: IdentityType.OTHER
    val art=when(type) {
        IdentityType.AADHAAR -> if(backSide) R.drawable.identity_aadhaar_back_v3 else R.drawable.identity_aadhaar_front_v3
        IdentityType.PAN -> if(backSide) R.drawable.identity_pan_back_v3 else R.drawable.identity_pan_front_v3
        IdentityType.VOTER -> if(backSide) R.drawable.identity_voter_back_v3 else R.drawable.identity_voter_front_v3
        IdentityType.LICENCE -> if(backSide) R.drawable.identity_licence_back_v3 else R.drawable.identity_licence_front_v3
        IdentityType.OTHER -> R.drawable.identity_pan_back_v3
    }
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(1.586f).clip(RoundedCornerShape(16.dp))
        .then(if(onClick!=null) Modifier.clickable(onClick=onClick) else Modifier)) {
        Image(painterResource(art),null,Modifier.fillMaxSize(),contentScale=ContentScale.FillBounds)
        val page=DocumentPrint(item,showPrivate,maxWidth.value,maxWidth.value/1.586f)
        with(page) {
            when(type) {
                IdentityType.AADHAAR -> AadhaarFace(backSide)
                IdentityType.PAN -> PanFace(backSide)
                IdentityType.VOTER -> VoterFace(backSide)
                IdentityType.LICENCE -> LicenceFace(backSide)
                IdentityType.OTHER -> PanFace(backSide,generic=true)
            }
        }
    }
}

private class DocumentPrint(val item:VaultItem,val privateVisible:Boolean,val width:Float,val height:Float) {
    val scale=width/360f
    val black=Color(0xFF101319)
    val navy=Color(0xFF092D65)
    fun Modifier.printArea(x:Float,y:Float,w:Float,h:Float)=this.offset((width*x).dp,(height*y).dp).width((width*w).dp).height((height*h).dp)
    fun value(key:String,private:Boolean=true):String=item.metadata[key].orEmpty().let { if(it.isNotBlank() && private && !privateVisible) "Hidden" else it }
    fun number():String=identityNumberText(identityType(item) ?: IdentityType.OTHER,item.metadata["idNumber"].orEmpty(),privateVisible).takeUnless { item.metadata["idNumber"].isNullOrBlank() }.orEmpty()
    @Composable fun Ink(text:String,x:Float,y:Float,w:Float,h:Float=.08f,size:Float=9f,bold:Boolean=false,
                        colour:Color=black,align:TextAlign=TextAlign.Start,lines:Int=1,serif:Boolean=false) {
        if(text.isBlank()) return
        Text(text,Modifier.printArea(x,y,w,h),color=colour,fontSize=(size*scale).sp,lineHeight=(size*scale*1.25f).sp,
            fontWeight=if(bold) FontWeight.Bold else FontWeight.Normal,fontFamily=if(serif) FontFamily.Serif else FontFamily.SansSerif,
            textAlign=align,maxLines=lines,overflow=TextOverflow.Ellipsis)
    }
    @Composable fun Pair(label:String,key:String,x:Float,y:Float,w:Float,size:Float=9f,labelSize:Float=6.5f,
                         lines:Int=1,private:Boolean=true,colour:Color=black) {
        Ink(label,x,y,w,.045f,labelSize,colour=colour)
        Ink(value(key,private),x,y+.048f,w,if(lines>1) .22f else .085f,size,true,lines=lines)
    }
    @Composable fun Photo(x:Float,y:Float,w:Float,h:Float) {
        identityAsset(item,IdentityAsset.PHOTO)?.let { IdentityAssetImage(it,"Your photo",Modifier.printArea(x,y,w,h).clip(RoundedCornerShape((3*scale).dp)),ContentScale.Crop) }
    }
    @Composable fun Signature(x:Float,y:Float,w:Float,h:Float) {
        if(privateVisible) identityAsset(item,IdentityAsset.SIGNATURE)?.let { IdentityAssetImage(it,"Your signature",Modifier.printArea(x,y,w,h),ContentScale.Fit) }
    }
    @Composable fun WebsiteQr(x:Float,y:Float,w:Float) {
        Column(Modifier.offset((width*x).dp,(height*y).dp).width((width*w).dp),horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally) {
            Image(ImageBitmap.imageResource(R.drawable.venus_website_qr),"Website QR: venusapp.in",Modifier.fillMaxWidth().aspectRatio(1f),
                contentScale=ContentScale.Fit,filterQuality=androidx.compose.ui.graphics.FilterQuality.None)
            Text("venusapp.in",fontSize=(5.3f*scale).sp,lineHeight=(6.6f*scale).sp,color=black,fontFamily=FontFamily.SansSerif,maxLines=1)
        }
    }
}

@Composable private fun DocumentPrint.AadhaarFace(back:Boolean) {
    if(!back) {
        Ink("भारत सरकार",.20f,.115f,.56f,.075f,13f,true,align=TextAlign.Center)
        Ink("GOVERNMENT OF INDIA",.20f,.19f,.56f,.06f,9f,true,align=TextAlign.Center)
        Ink("सत्यमेव जयते",.035f,.268f,.115f,.04f,3.6f,align=TextAlign.Center)
        Photo(.034f,.327f,.226f,.439f)
        Pair("नाम / Name","cardholder",.30f,.31f,.40f,12f,8f,private=false)
        if(value("localName",false).isNotBlank()) Ink(value("localName",false),.30f,.425f,.40f,.06f,8f)
        Pair("जन्म तिथि / DOB","dateOfBirth",.30f,.48f,.37f,10f,8f)
        Pair("लिंग / Gender","gender",.30f,.64f,.37f,10f,8f)
        WebsiteQr(.735f,.415f,.215f)
        Ink(number(),.285f,.767f,.65f,.09f,16f,true)
        Ink("मेरा आधार, मेरी पहचान",.17f,.867f,.66f,.085f,14f,align=TextAlign.Center)
    } else {
        Ink("भारतीय विशिष्ट पहचान प्राधिकरण",.23f,.102f,.59f,.07f,8.5f,true,align=TextAlign.Center)
        Ink("UNIQUE IDENTIFICATION AUTHORITY OF INDIA",.23f,.184f,.59f,.06f,6.6f,true,align=TextAlign.Center)
        Ink("पता / Address",.04f,.31f,.63f,.07f,9f,true)
        Ink(value("cardholder",false),.04f,.405f,.63f,.08f,10f)
        Ink(value("address"),.04f,.495f,.63f,.32f,9.5f,lines=5)
        WebsiteQr(.768f,.53f,.184f)
        Ink("1947",.12f,.918f,.16f,.06f,7.2f)
        Ink("help@uidai.gov.in",.35f,.918f,.33f,.06f,7.2f,align=TextAlign.Center)
        Ink("www.uidai.gov.in",.70f,.918f,.28f,.06f,7.2f,align=TextAlign.Center)
    }
}

@Composable private fun DocumentPrint.PanFace(back:Boolean,generic:Boolean=false) {
    if(generic) {
        Ink(value("issuer",false).ifBlank { "IDENTITY CARD" },.06f,.09f,.88f,.10f,13f,true,navy,TextAlign.Center)
        if(!back) {
            Ink(number(),.06f,.275f,.60f,.09f,13f,true)
            Pair("Name","cardholder",.06f,.42f,.60f,11f,7f,private=false)
            Pair("Date of birth","dateOfBirth",.06f,.60f,.60f,9f,7f)
            Pair("Valid until","validTill",.06f,.75f,.60f,9f,7f)
            Photo(.733f,.468f,.184f,.30f)
        } else {
            Pair("Address","address",.06f,.29f,.60f,9.5f,8f,lines=5)
            WebsiteQr(.735f,.47f,.20f)
        }
        Ink("venusapp.in",.18f,.89f,.64f,.06f,9f,colour=navy,align=TextAlign.Center)
        return
    }
    if(!back) {
        if(!generic) {
            Ink("स्थायी लेखा संख्या कार्ड",.22f,.285f,.53f,.06f,9f,true,navy,TextAlign.Center)
            Ink("Permanent Account Number Card",.18f,.35f,.62f,.06f,8.2f,true,navy,TextAlign.Center)
        } else Ink(value("issuer",false).ifBlank { "IDENTITY CARD" },.06f,.08f,.86f,.12f,13f,true)
        Ink(number(),.27f,.432f,.46f,.09f,16f,true,align=TextAlign.Center)
        Pair("नाम / Name","cardholder",.05f,.535f,.64f,11f,7f,private=false,colour=navy)
        Pair("पिता / माता का नाम / Parent's name","relativeName",.05f,.685f,.64f,10f,6.7f,colour=navy)
        Pair("जन्म की तारीख / Date of Birth","dateOfBirth",.05f,.845f,.35f,10f,6.4f,colour=navy)
        Photo(.767f,.554f,.184f,.35f)
        Signature(.40f,.825f,.26f,.095f)
        Ink("हस्ताक्षर / Signature",.39f,.925f,.30f,.05f,7f,colour=navy,align=TextAlign.Center)
    } else {
        Ink("इस कार्ड के खोने / पाने पर कृपया निकटतम आयकर कार्यालय को सूचित करें / लौटाएँ।",.06f,.09f,.88f,.11f,8f,colour=navy,lines=2)
        Ink("If this card is lost / found, please inform / return to the nearest Income Tax Office.",.06f,.22f,.88f,.13f,7.6f,colour=navy,lines=2)
        Pair("पता / Address","address",.06f,.42f,.60f,9.5f,8f,lines=5,colour=navy)
        WebsiteQr(.735f,.445f,.20f)
        Ink("venusapp.in",.18f,.89f,.64f,.06f,9f,colour=navy,align=TextAlign.Center)
    }
}

@Composable private fun DocumentPrint.VoterFace(back:Boolean) {
    if(back) Box(Modifier.printArea(.20f,.125f,.58f,.005f).background(black))
    Ink("भारतीय निर्वाचन आयोग",.20f,.035f,.58f,.07f,10f,true,align=TextAlign.Center)
    Ink("ELECTION COMMISSION OF INDIA",.20f,.145f,.58f,.065f,8.5f,true,align=TextAlign.Center)
    if(!back) {
        Ink("मतदाता पहचान पत्र / Elector Photo Identity Card",.22f,.215f,.57f,.05f,5.8f,align=TextAlign.Center)
        Ink(number(),.045f,.25f,.40f,.055f,8.5f,true)
        Photo(.045f,.296f,.232f,.479f)
        Pair("नाम / Name","cardholder",.315f,.345f,.63f,10.5f,7f,private=false)
        Pair("संबंधी का नाम / Relative's Name","relativeName",.315f,.505f,.63f,9f,6.5f)
        Pair("लिंग / Gender","gender",.315f,.66f,.28f,9f,6.5f)
        val age=item.metadata["dateOfBirth"].isNullOrBlank()
        Pair(if(age) "आयु / Age" else "जन्म तिथि / DOB",if(age) "age" else "dateOfBirth",.315f,.80f,.63f,9f,6.5f)
    } else {
        Ink("EPIC No.:",.045f,.255f,.53f,.05f,6.8f)
        Ink(number(),.045f,.31f,.60f,.07f,10f,true)
        Pair("Address:","address",.045f,.405f,.60f,8f,6.8f,lines=3)
        Pair("Assembly Constituency:","constituency",.045f,.615f,.60f,8f,6.8f,lines=2)
        Pair("Part No.:","partNumber",.045f,.745f,.25f,8f,6.5f)
        Pair("Serial No.:","serialNumber",.335f,.745f,.25f,8f,6.5f)
        Pair("Date of Issue:","issueDate",.045f,.875f,.53f,8f,6.5f)
        WebsiteQr(.73f,.545f,.225f)
    }
}

@Composable private fun DocumentPrint.LicenceFace(back:Boolean) {
    Ink("THE UNION OF INDIA",.18f,.037f,.68f,.035f,5.4f,true,Color.White,TextAlign.Center)
    val state=value("issuer",false).uppercase().takeUnless { it=="TRANSPORT DEPARTMENT" }.orEmpty()
    Ink(if(state.isBlank()) "INDIAN UNION DRIVING LICENCE" else "$state STATE MOTOR DRIVING LICENCE",.17f,.079f,.69f,.055f,7.6f,true,Color.White,TextAlign.Center)
    if(!back) {
        Ink("FORM 7",.835f,.218f,.14f,.03f,5.3f,true,align=TextAlign.Center)
        Ink("RULE 16(2)",.835f,.25f,.14f,.03f,4.7f,align=TextAlign.Center)
        Ink("DL No: ${number()}",.15f,.245f,.48f,.065f,7.6f,true)
        Ink("DOI: ${value("issueDate")}",.58f,.245f,.20f,.065f,6.7f)
        Ink("Valid Till (NT): ${value("validTill")}",.29f,.325f,.49f,.065f,7f)
        Ink("Valid Till (T): ${value("transportValidTill")}",.29f,.375f,.49f,.05f,6.4f)
        Ink("AUTHORISATION TO DRIVE FOLLOWING CLASS OF VEHICLES THROUGHOUT INDIA",.29f,.425f,.42f,.12f,5.5f,true,lines=3)
        Ink(value("vehicleClasses"),.29f,.57f,.42f,.10f,9f,true,lines=2)
        Photo(.807f,.29f,.165f,.363f)
        Ink("DOB: ${value("dateOfBirth")}",.29f,.685f,.35f,.065f,7f)
        Ink("BG: ${value("bloodGroup")}",.64f,.685f,.12f,.065f,6.6f)
        Ink("Name: ${value("cardholder",false)}",.035f,.755f,.70f,.065f,8f,true)
        Ink("S/D/W of: ${value("relativeName")}",.035f,.82f,.70f,.065f,7f)
        Ink("Address: ${value("address")}",.035f,.88f,.70f,.105f,6.4f,lines=3)
        Signature(.78f,.80f,.17f,.09f)
        Ink("Signature / Thumb",.765f,.915f,.21f,.035f,4.9f,align=TextAlign.Center)
        Ink("impression of holder",.765f,.95f,.21f,.035f,4.7f,align=TextAlign.Center)
    } else {
        Pair("Name:","cardholder",.04f,.25f,.64f,10f,6.5f,private=false)
        Pair("Address:","address",.04f,.40f,.64f,8.5f,6.5f,lines=3)
        Pair("Vehicle classes:","vehicleClasses",.04f,.62f,.61f,8.5f,6.5f)
        Pair("Validity (NT):","validTill",.04f,.77f,.31f,8f,6f)
        Pair("Validity (T):","transportValidTill",.37f,.77f,.30f,8f,6f)
        Ink(number(),.04f,.92f,.64f,.065f,9f,true)
        WebsiteQr(.76f,.54f,.19f)
    }
}

@Composable internal fun IdentityAssetImage(file:VaultAttachment,description:String?,modifier:Modifier,scale:ContentScale=ContentScale.Fit) {
    val bitmap by produceState<android.graphics.Bitmap?>(null,file.base64Data) {
        value=withContext(Dispatchers.IO) { decodeAttachmentPreview(file.base64Data,1200,1200) }
    }
    bitmap?.let { Image(it.asImageBitmap(),description,modifier,contentScale=scale) }
}
