package com.lockr.app

import android.graphics.Bitmap
import android.util.Base64
import androidx.core.graphics.scale
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.math.roundToInt

internal data class AssetCrop(val left: Int, val top: Int, val width: Int, val height: Int)
internal fun identityCropBounds(left: Float, top: Float, right: Float, bottom: Float, width: Int, height: Int): AssetCrop {
    require(width > 0 && height > 0)
    require(listOf(left,top,right,bottom).all { it.isFinite() && it in 0f..1f } && right > left && bottom > top)
    val x = (left*width).roundToInt().coerceIn(0,width-1)
    val y = (top*height).roundToInt().coerceIn(0,height-1)
    val w = ((right*width).roundToInt().coerceAtMost(width)-x)
    val h = ((bottom*height).roundToInt().coerceAtMost(height)-y)
    require(w >= 24 && h >= 24) { "Select a larger area of the original." }
    return AssetCrop(x,y,w,h)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityAssetCropSheet(source: VaultAttachment, role: IdentityAsset, onDismiss: () -> Unit, onApply: (VaultAttachment) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var page by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(1) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var start by remember(page) { mutableStateOf(Offset(.15f,.15f)) }
    var end by remember(page) { mutableStateOf(Offset(.85f,.85f)) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var dragging by remember { mutableIntStateOf(-1) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    LaunchedEffect(source, page) {
        bitmap = null; error = ""
        try {
            val result = if(isPdfAttachment(source)) renderPdfAttachmentPage(source,page,context)
                else withContext(Dispatchers.IO) { decodeAttachmentPreview(source.base64Data,2400,3200) to 1 }
            bitmap = result.first; pageCount = result.second
            if(bitmap == null) error = "This page could not be opened. Try an image or an unlocked PDF."
        } catch(cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch(_: Exception) { error = "Could not open the original page." }
    }
    ModalBottomSheet(onDismissRequest = { if(!busy) onDismiss() }, sheetGesturesEnabled = false,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { it != SheetValue.Hidden || !busy })) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal=20.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Crop ${role.label.replaceFirstChar(Char::lowercase)}", style=MaterialTheme.typography.titleLarge)
            Text(if(role==IdentityAsset.QR) "Keep the entire original QR and its white border inside the selection."
                else "Drag the two corners around the ${role.label.replaceFirstChar(Char::lowercase)} on your original.", style=MaterialTheme.typography.bodySmall)
            val image = bitmap
            if(image==null && error.isBlank()) LinearProgressIndicator(Modifier.fillMaxWidth())
            if(image!=null) Box(Modifier.weight(1f,fill=false).fillMaxWidth().heightIn(min=180.dp,max=360.dp).onSizeChanged { viewport=it }
                .pointerInput(image,viewport,busy) {
                    fun rect(): FloatArray {
                        val scale=minOf(viewport.width.toFloat()/image.width,viewport.height.toFloat()/image.height)
                        val w=image.width*scale;val h=image.height*scale
                        return floatArrayOf((viewport.width-w)/2,(viewport.height-h)/2,w,h)
                    }
                    detectDragGestures(onDragStart={ touch ->
                        val r=rect()
                        val a=Offset(r[0]+start.x*r[2],r[1]+start.y*r[3]);val b=Offset(r[0]+end.x*r[2],r[1]+end.y*r[3])
                        dragging=if((touch-a).getDistance()<(touch-b).getDistance()) 0 else 1
                    },onDragEnd={dragging=-1},onDragCancel={dragging=-1}) { change, _ ->
                        change.consume();val r=rect()
                        if(!busy && r[2]>0 && r[3]>0) {
                            val x=(change.position.x-r[0])/r[2];val y=(change.position.y-r[1])/r[3]
                            if(dragging==0) start=Offset(x.coerceIn(0f,end.x-.02f),y.coerceIn(0f,end.y-.02f))
                            else if(dragging==1) end=Offset(x.coerceIn(start.x+.02f,1f),y.coerceIn(start.y+.02f,1f))
                        }
                    }
                },contentAlignment=Alignment.Center) {
                Image(image.asImageBitmap(),"Original page for cropping",Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
                Canvas(Modifier.fillMaxSize()) {
                    val scale=minOf(size.width/image.width,size.height/image.height);val w=image.width*scale;val h=image.height*scale
                    val a=Offset((size.width-w)/2+start.x*w,(size.height-h)/2+start.y*h)
                    val b=Offset((size.width-w)/2+end.x*w,(size.height-h)/2+end.y*h)
                    drawRect(Color(0xFF36B8AF),a,androidx.compose.ui.geometry.Size(b.x-a.x,b.y-a.y),style=Stroke(3.dp.toPx()))
                    listOf(a,b).forEach { drawCircle(Color.White,12.dp.toPx(),it);drawCircle(Color(0xFF36B8AF),8.dp.toPx(),it) }
                }
            }
            if(pageCount>1) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                TextButton(enabled=page>0 && !busy,onClick={page--}) { Text("Previous") }
                Text("Page ${page+1} of $pageCount")
                TextButton(enabled=page<pageCount-1 && !busy,onClick={page++}) { Text("Next") }
            }
            if(error.isNotBlank()) Text(error,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
            Button(enabled=image!=null && !busy,onClick={
                busy=true
                scope.launch {
                    try {
                        val result=withContext(Dispatchers.Default) {
                            val original=image!!
                            val b=identityCropBounds(start.x,start.y,end.x,end.y,original.width,original.height)
                            val cropped=Bitmap.createBitmap(original,b.left,b.top,b.width,b.height)
                            var scaled: Bitmap?=null
                            try {
                                val ratio=minOf(1f,1200f/maxOf(b.width,b.height))
                                scaled=if(ratio<1f) cropped.scale((b.width*ratio).roundToInt().coerceAtLeast(1),(b.height*ratio).roundToInt().coerceAtLeast(1),role!=IdentityAsset.QR) else cropped
                                val output=ByteArrayOutputStream();require(scaled!!.compress(Bitmap.CompressFormat.PNG,100,output))
                                val bytes=output.toByteArray();require(bytes.size<=2*1024*1024) { "Select a smaller area (maximum 2 MB)." }
                                VaultAttachment("stash-id-${role.name.lowercase()}-${UUID.randomUUID()}.png","image/png",bytes.size.toLong(),Base64.encodeToString(bytes,Base64.NO_WRAP))
                            } finally { if(scaled!==cropped && scaled!==original) scaled?.recycle();if(cropped!==original) cropped.recycle() }
                        }
                        onApply(result)
                    } catch(cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                    catch(failure: Exception) { error=failure.message ?: "Could not crop this area." }
                    finally { busy=false }
                }
            },modifier=Modifier.fillMaxWidth()) { Text(if(busy) "Preparing…" else "Use ${role.label.replaceFirstChar(Char::lowercase)}") }
            TextButton(enabled=!busy,onClick=onDismiss,modifier=Modifier.fillMaxWidth().padding(bottom=12.dp)) { Text("Cancel") }
        }
    }
}
