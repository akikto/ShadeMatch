package com.example.ui.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.Executors

@Composable
fun CameraCaptureView(
    modifier: Modifier = Modifier,
    onPhotoCaptured: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var isCameraReady by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setTargetRotation(previewView.display?.rotation ?: 0)
                        .build()

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        val cam = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            capture
                        )
                        camera = cam
                        imageCapture = capture
                        isCameraReady = true
                    } catch (exc: Exception) {
                        Log.e("CameraCaptureView", "Use case binding failed", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize().testTag("camera_preview_surface")
        )

        // Clinical Tooth Reticle Overlay
        ToothTargetReticleOverlay(
            modifier = Modifier.fillMaxSize()
        )

        // Top Controls: Torch / Guidance
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Guidance",
                        tint = Color(0xFF67E8F9),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "5500K Diffuse Lighting • Align Tooth",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            IconButton(
                onClick = {
                    camera?.cameraControl?.let { control ->
                        isTorchOn = !isTorchOn
                        control.enableTorch(isTorchOn)
                    }
                },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
                    .testTag("torch_toggle_button")
            ) {
                Icon(
                    imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Toggle Torch",
                    tint = if (isTorchOn) Color(0xFFFBBF24) else Color.White
                )
            }
        }

        // Shutter Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            IconButton(
                onClick = {
                    takePhoto(context, imageCapture) { bitmap ->
                        onPhotoCaptured(bitmap)
                    }
                },
                enabled = isCameraReady,
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.White, RoundedCornerShape(36.dp))
                    .padding(4.dp)
                    .testTag("camera_shutter_button")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0D5C75), RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Capture Photo",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ToothTargetReticleOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height * 0.44f
        val ovalWidth = size.width * 0.42f
        val ovalHeight = size.height * 0.36f

        // Draw outer darkened mask
        val strokePaint = Color(0xFF67E8F9)
        val guidePaint = Color.White.copy(alpha = 0.5f)

        // Central anatomical tooth target oval
        drawOval(
            color = strokePaint,
            topLeft = Offset(cx - ovalWidth / 2f, cy - ovalHeight / 2f),
            size = Size(ovalWidth, ovalHeight),
            style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f))
        )

        // Zone division lines: Cervical (top 30%), Incisal (bottom 30%)
        val topZoneY = (cy - ovalHeight / 2f) + (ovalHeight * 0.30f)
        val botZoneY = (cy - ovalHeight / 2f) + (ovalHeight * 0.70f)
        val zoneLineHalfW = ovalWidth * 0.40f

        drawLine(
            color = Color(0xFFFBBF24),
            start = Offset(cx - zoneLineHalfW, topZoneY),
            end = Offset(cx + zoneLineHalfW, topZoneY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        )

        drawLine(
            color = Color(0xFF38BDF8),
            start = Offset(cx - zoneLineHalfW, botZoneY),
            end = Offset(cx + zoneLineHalfW, botZoneY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        )

        // Center crosshair
        drawLine(
            color = guidePaint,
            start = Offset(cx - 20f, cy),
            end = Offset(cx + 20f, cy),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = guidePaint,
            start = Offset(cx, cy - 20f),
            end = Offset(cx, cy + 20f),
            strokeWidth = 1.dp.toPx()
        )
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    onSuccess: (Bitmap) -> Unit
) {
    if (imageCapture == null) return

    val photoFile = File(context.cacheDir, "temp_capture_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                if (bitmap != null) {
                    onSuccess(bitmap)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraCaptureView", "Photo capture failed: ${exception.message}", exception)
            }
        }
    )
}
