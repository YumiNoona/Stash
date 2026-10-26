package com.lockr.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume

@Composable
fun CameraScannerScreen(
    category: VaultCategory,
    onBack: () -> Unit,
    onReviewPages: (List<File>) -> Unit,
    modifier: Modifier = Modifier
) {
    val isCard = category == VaultCategory.CARD
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val capturedPages = remember { mutableStateListOf<File>() }
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var captureBusy by remember { mutableStateOf(false) }
    val capture = remember {
        val rotation = (context as? Activity)?.windowManager?.defaultDisplay?.rotation ?: Surface.ROTATION_0
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).setTargetRotation(rotation).build()
    }
    val previewView = remember(context) { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (!granted) cameraError = "Allow camera access to scan ${if (isCard) "a card" else "a document"}."
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) cameraPermission.launch(Manifest.permission.CAMERA)
    }

    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    LaunchedEffect(hasPermission, previewView) {
        if (!hasPermission) return@LaunchedEffect
        try {
            val cameraProvider = ProcessCameraProvider.getInstance(context).awaitCameraProvider(context)
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
            provider = cameraProvider
            cameraError = null
        } catch (_: Exception) {
            cameraError = "The camera could not be started. Try importing a photo or file instead."
        }
    }
    DisposableEffect(provider) {
        val boundProvider = provider
        onDispose { boundProvider?.unbindAll() }
    }
    val backAndClean: () -> Unit = {
        capturedPages.forEach(File::delete)
        onBack()
    }
    BackHandler { backAndClean() }

    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = backAndClean, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = LockrColors.TextPrimary)
            }
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(if (isCard) "Scan card" else "Scan documents", style = MaterialTheme.typography.headlineSmall, color = LockrColors.TextPrimary)
                Text(if (isCard) "Capture the front, then review" else "Capture each page, then review", style = MaterialTheme.typography.bodySmall, color = LockrColors.TextSecondary)
            }
            if (!isCard && capturedPages.isNotEmpty()) {
                IconButton(onClick = { capturedPages.removeAt(capturedPages.lastIndex).delete() }) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove last page", tint = LockrColors.TextPrimary)
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color(0xFF263640))) {
            if (hasPermission) AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            else Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
                Text(cameraError ?: "Camera permission is needed to scan.", color = Color.White, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
                Button(onClick = { cameraPermission.launch(Manifest.permission.CAMERA) }, modifier = Modifier.padding(top = 12.dp)) { Text("Allow camera") }
            }
            cameraError?.takeIf { hasPermission }?.let {
                Text(it, color = Color.White, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.align(Alignment.Center).padding(24.dp))
            }
            Box(if (isCard) Modifier.align(Alignment.Center).fillMaxWidth(0.88f).aspectRatio(1.58f)
                .border(2.dp, Color.White.copy(alpha = 0.87f), RoundedCornerShape(21.dp))
            else Modifier.align(Alignment.Center).fillMaxWidth(0.82f).fillMaxSize(0.72f)
                .border(1.dp, Color.White.copy(alpha = 0.75f), RoundedCornerShape(12.dp)))
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (isCard) "Position the card inside the frame" else if (capturedPages.isEmpty()) "Position the page in the frame" else "${capturedPages.size} page${if (capturedPages.size == 1) "" else "s"} captured",
                style = MaterialTheme.typography.bodyMedium,
                color = LockrColors.TextSecondary,
                modifier = Modifier.weight(1f)
            )
            if (!isCard && capturedPages.isNotEmpty()) {
                Button(onClick = { onReviewPages(capturedPages.toList()) }) {
                    Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Review", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().padding(bottom = 12.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(Color.White).border(5.dp, LockrColors.SeaGlass, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = {
                    if (captureBusy) return@IconButton
                    captureBusy = true
                    val file = File(context.cacheDir, "stash-scan-${UUID.randomUUID()}.jpg")
                    val options = ImageCapture.OutputFileOptions.Builder(file).build()
                    capture.takePicture(options, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            if (isCard) onReviewPages(listOf(file)) else capturedPages.add(file)
                            cameraError = null
                            captureBusy = false
                        }

                        override fun onError(exception: ImageCaptureException) {
                            file.delete()
                            cameraError = "Could not capture this page. Please try again."
                            captureBusy = false
                        }
                    })
                }, modifier = Modifier.fillMaxSize()) {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = if (isCard) "Capture card" else "Capture page", tint = LockrColors.TextPrimary, modifier = Modifier.size(29.dp))
                }
            }
        }
    }
}

private suspend fun com.google.common.util.concurrent.ListenableFuture<ProcessCameraProvider>.awaitCameraProvider(context: Context): ProcessCameraProvider =
    suspendCancellableCoroutine { continuation ->
        addListener({
            try {
                continuation.resume(get())
            } catch (error: Exception) {
                continuation.resumeWith(Result.failure(error))
            }
        }, ContextCompat.getMainExecutor(context))
    }
