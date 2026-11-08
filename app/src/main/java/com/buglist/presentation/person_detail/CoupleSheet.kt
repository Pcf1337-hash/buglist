package com.buglist.presentation.person_detail

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.buglist.R
import com.buglist.domain.model.SyncRole
import com.buglist.presentation.components.GoldButton
import com.buglist.presentation.theme.BugListColors
import com.buglist.presentation.theme.OswaldFontFamily
import com.buglist.presentation.theme.RobotoCondensedFontFamily
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoupleSheet(
    role: SyncRole?,
    code: String?,
    error: String?,
    onStart: (bothCanEdit: Boolean) -> Unit,
    onAccept: (String) -> Unit,
    onUnlink: () -> Unit,
    onSyncNow: () -> Unit,
    onImportList: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var scanning by remember { mutableStateOf(false) }
    var pasted by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BugListColors.Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = stringResource(R.string.couple_title),
                fontFamily = OswaldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = BugListColors.Gold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.couple_mailbox_hint),
                fontFamily = RobotoCondensedFontFamily,
                fontSize = 14.sp,
                color = BugListColors.TextSecondary
            )
            Spacer(Modifier.height(16.dp))

            if (role != null && code == null) {
                Text(
                    text = when (role) {
                        SyncRole.HOST -> stringResource(R.string.couple_role_host)
                        SyncRole.GUEST -> stringResource(R.string.couple_role_guest)
                        SyncRole.BOTH -> stringResource(R.string.couple_role_both)
                    },
                    fontFamily = RobotoCondensedFontFamily,
                    fontSize = 16.sp,
                    color = BugListColors.TextPrimary
                )
                Spacer(Modifier.height(16.dp))
                if (role != SyncRole.GUEST) {
                    GoldButton(
                        text = stringResource(R.string.couple_show_code),
                        onClick = { onStart(role == SyncRole.BOTH) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
                GoldButton(text = stringResource(R.string.couple_sync_now), onClick = onSyncNow)
                Spacer(Modifier.height(8.dp))
                GoldButton(text = stringResource(R.string.couple_unlink), onClick = onUnlink)
            } else if (code != null) {
                Image(
                    bitmap = remember(code) { qrBitmap(code).asImageBitmap() },
                    contentDescription = stringResource(R.string.couple_title),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                )
                Spacer(Modifier.height(8.dp))
                SelectionContainer {
                    Text(
                        text = code,
                        fontFamily = RobotoCondensedFontFamily,
                        fontSize = 11.sp,
                        color = BugListColors.TextSecondary
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (role == SyncRole.BOTH) {
                        stringResource(R.string.couple_role_both)
                    } else {
                        stringResource(R.string.couple_role_host)
                    },
                    fontFamily = RobotoCondensedFontFamily,
                    fontSize = 14.sp,
                    color = BugListColors.TextSecondary
                )
            } else {
                Text(
                    text = stringResource(R.string.couple_host_hint),
                    fontFamily = RobotoCondensedFontFamily,
                    fontSize = 14.sp,
                    color = BugListColors.TextSecondary
                )
                Spacer(Modifier.height(8.dp))
                GoldButton(
                    text = stringResource(R.string.couple_host),
                    onClick = { onStart(false) }
                )
                Spacer(Modifier.height(16.dp))
                GoldButton(
                    text = stringResource(R.string.couple_scan),
                    onClick = { scanning = !scanning }
                )
                if (scanning) {
                    Spacer(Modifier.height(12.dp))
                    QrScanner(onCode = { scanned ->
                        scanning = false
                        onAccept(scanned)
                    })
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pasted,
                    onValueChange = { pasted = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = {
                        Text(
                            stringResource(R.string.couple_paste_hint),
                            fontFamily = RobotoCondensedFontFamily
                        )
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BugListColors.TextPrimary,
                        unfocusedTextColor = BugListColors.TextPrimary,
                        focusedBorderColor = BugListColors.Gold,
                        unfocusedBorderColor = BugListColors.BorderSubtle,
                        cursorColor = BugListColors.Gold
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(Modifier.height(8.dp))
                GoldButton(
                    text = stringResource(R.string.couple_link),
                    enabled = pasted.isNotBlank(),
                    onClick = { onAccept(pasted.trim()) }
                )
            }

            if (!error.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = error,
                    fontFamily = RobotoCondensedFontFamily,
                    fontSize = 14.sp,
                    color = BugListColors.DebtRed
                )
            }

            if (role?.canEdit != false) {
                Spacer(Modifier.height(16.dp))
                GoldButton(
                    text = stringResource(R.string.couple_import_list),
                    onClick = onImportList
                )
            }
        }
    }
}

@Composable
private fun QrScanner(onCode: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
    }
    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.CAMERA)
    }
    if (!granted) {
        Text(
            text = stringResource(R.string.couple_camera_needed),
            fontFamily = RobotoCondensedFontFamily,
            color = BugListColors.TextSecondary
        )
        return
    }
    val previewView = remember { PreviewView(context) }
    val delivered = remember { mutableStateOf(false) }
    AndroidView(
        factory = { previewView },
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
    )
    DisposableEffect(previewView) {
        val executor = Executors.newSingleThreadExecutor()
        val future = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            val provider = future.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            val reader = MultiFormatReader()
            analysis.setAnalyzer(executor) { image ->
                if (delivered.value) {
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
                    if (!delivered.value && result.text.startsWith("BL1.")) {
                        delivered.value = true
                        val text = result.text
                        ContextCompat.getMainExecutor(context).execute { onCode(text) }
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
        }
        future.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {
            if (future.isDone) future.get().unbindAll()
            executor.shutdown()
        }
    }
}

private fun qrBitmap(text: String): Bitmap {
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
