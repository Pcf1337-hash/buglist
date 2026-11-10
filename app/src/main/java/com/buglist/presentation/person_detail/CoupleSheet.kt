package com.buglist.presentation.person_detail

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.buglist.R
import com.buglist.MainActivity
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.RobotoCondensedFontFamily
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
internal fun QrScanner(onCode: (String) -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.bugListActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraError by remember { mutableStateOf(false) }
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        activity?.endRuntimePermissionRequest()
        granted = it
    }
    LaunchedEffect(Unit) {
        if (!granted) {
            activity?.beginRuntimePermissionRequest()
            try {
                launcher.launch(Manifest.permission.CAMERA)
            } catch (_: Exception) {
                activity?.endRuntimePermissionRequest()
                cameraError = true
            }
        }
    }
    if (!granted) {
        Text(
            text = stringResource(if (cameraError) R.string.wizard_camera_error else R.string.couple_camera_needed),
            fontFamily = RobotoCondensedFontFamily,
            color = BugListColors.TextSecondary
        )
        return
    }
    val previewView = remember { PreviewView(context) }
    val delivered = remember { AtomicBoolean(false) }
    AndroidView(
        factory = { previewView },
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
    )
    DisposableEffect(previewView) {
        val disposed = AtomicBoolean(false)
        val executor = Executors.newSingleThreadExecutor()
        val future = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            if (disposed.get()) return@Runnable
            try {
                val provider = future.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                val reader = MultiFormatReader()
                analysis.setAnalyzer(executor) { image ->
                    if (delivered.get() || disposed.get()) {
                        image.close()
                        return@setAnalyzer
                    }
                    try {
                        val plane = image.planes[0]
                        val buffer = plane.buffer
                        val data = ByteArray(buffer.remaining())
                        buffer.get(data)
                        val source = PlanarYUVLuminanceSource(
                            data,
                            plane.rowStride,
                            image.height,
                            0,
                            0,
                            image.width,
                            image.height,
                            false
                        )
                        val result = reader.decode(BinaryBitmap(HybridBinarizer(source)))
                        if (result.text.startsWith("BL1.") && delivered.compareAndSet(false, true)) {
                            val text = result.text
                            ContextCompat.getMainExecutor(context).execute {
                                if (!disposed.get()) onCode(text)
                            }
                        }
                    } catch (_: Exception) {
                        // Not a QR yet.
                    } finally {
                        image.close()
                    }
                }
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            } catch (_: Exception) {
                if (!disposed.get()) cameraError = true
            }
        }
        future.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {
            disposed.set(true)
            if (future.isDone) runCatching { future.get().unbindAll() }
            executor.shutdown()
        }
    }
    if (cameraError) {
        Text(stringResource(R.string.wizard_camera_error), color = BugListColors.Orange,
            fontFamily = RobotoCondensedFontFamily)
    }
}

internal fun qrBitmap(text: String): Bitmap {
    val size = 640
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bitmap
}

private fun Context.bugListActivity(): MainActivity? = when (this) {
    is MainActivity -> this
    is ContextWrapper -> baseContext.bugListActivity()
    else -> null
}
