package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.PointF
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.components.GlassCard
import com.example.ui.components.ObsidianBackground
import com.example.ui.components.StatusPillBadge
import com.example.ui.theme.AsifTheme
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.FolderGreen
import com.example.ui.theme.Navy950
import com.example.ui.viewmodel.MainViewModel
import com.example.vision.ImageProcessor
import com.example.vision.NormalizedQuad
import com.example.vision.OnDeviceOcrEngine
import com.example.vision.ScanFilter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot

private data class CapturedPageDraft(
    val rawBitmap: Bitmap,
    val quad: NormalizedQuad,
    val filter: ScanFilter = ScanFilter.AUTO,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val rotation: Int = 0,
) {
    fun renderProcessed(): Bitmap {
        val warped = ImageProcessor.perspectiveWarp(rawBitmap, quad)
        val rotated = ImageProcessor.rotateBitmap(warped, rotation)
        return ImageProcessor.applyFilterAndAdjustments(rotated, filter, brightness, contrast)
    }
}

@Composable
fun ScannerScreen(
    viewModel: MainViewModel,
    onClose: () -> Unit,
    onSavedDocument: (String) -> Unit,
) {
    BackHandler(onBack = onClose)

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val colors = AsifTheme.colors

    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasCameraPerm = it
    }

    var flashEnabled by remember { mutableStateOf(false) }
    var autoCapture by remember { mutableStateOf(false) }
    var batchMode by remember { mutableStateOf(true) }
    var editingMode by remember { mutableStateOf(false) }
    var activeDraftIndex by remember { mutableIntStateOf(0) }
    var docTitle by remember { mutableStateOf("") }
    var detectedCategory by remember { mutableStateOf("Document") }
    var saveToVault by remember { mutableStateOf(false) }
    var showCropHandles by remember { mutableStateOf(false) }

    val drafts = remember { mutableStateListOf<CapturedPageDraft>() }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)?.let { bmp ->
                    val quad = ImageProcessor.estimateDocumentQuad(bmp)
                    drafts.add(CapturedPageDraft(bmp, quad))
                    activeDraftIndex = drafts.lastIndex
                    editingMode = true
                }
            }
        }
    }

    // Auto-capture countdown simulation when enabled in live viewfinder
    LaunchedEffect(autoCapture, editingMode, hasCameraPerm) {
        if (autoCapture && !editingMode && hasCameraPerm && imageCapture != null) {
            delay(2500)
            imageCapture?.takePicture(
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val bmp = imageProxyToBitmap(image)
                        image.close()
                        if (bmp != null) {
                            val quad = ImageProcessor.estimateDocumentQuad(bmp)
                            drafts.add(CapturedPageDraft(bmp, quad))
                            if (!batchMode) {
                                activeDraftIndex = drafts.lastIndex
                                editingMode = true
                            }
                        }
                    }
                },
            )
        }
    }

    if (!editingMode) {
        // LIVE CAMERA VIEWFINDER
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Navy950),
        ) {
            if (hasCameraPerm) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener(
                            {
                                val provider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                                val cap = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .build()
                                imageCapture = cap
                                provider.unbindAll()
                                val cam = provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    cap,
                                )
                                cameraControl = cam.cameraControl
                            },
                            ContextCompat.getMainExecutor(ctx),
                        )
                        previewView
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                // Permission prompt / offline gallery scanner fallback
                ObsidianBackground {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            tint = CyanGlow,
                            modifier = Modifier.size(64.dp),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Camera Access Required for Live Scan",
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "All vision edge-detection and OCR run 100% locally. Or import an image directly from your device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { permLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            shape = RoundedCornerShape(50),
                        ) {
                            Text("Enable Camera", color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            shape = RoundedCornerShape(50),
                        ) {
                            Text("Import Photo Instead")
                        }
                    }
                }
            }

            // Animated Edge-Detection Overlay + Laser Scan-Line
            if (hasCameraPerm) {
                ScannerDetectionOverlay()
            }

            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .testTag("scanner_close"),
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.White)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = autoCapture,
                        onClick = { autoCapture = !autoCapture },
                        label = { Text("Auto") },
                        leadingIcon = {
                            Icon(Icons.Outlined.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                    )
                    FilterChip(
                        selected = batchMode,
                        onClick = { batchMode = !batchMode },
                        label = { Text(if (batchMode) "Batch (${drafts.size})" else "Single") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                    )
                }

                IconButton(
                    onClick = {
                        flashEnabled = !flashEnabled
                        cameraControl?.enableTorch(flashEnabled)
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                ) {
                    Icon(
                        if (flashEnabled) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                        contentDescription = "Flash",
                        tint = if (flashEnabled) CyanGlow else Color.White,
                    )
                }
            }

            // Bottom Shutter Controls
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .testTag("scanner_gallery_btn"),
                ) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = "Import Photo", tint = Color.White)
                }

                // Glowing Shutter Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(4.dp, Brush.linearGradient(listOf(ElectricBlue, CyanGlow)), CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            if (hasCameraPerm && imageCapture != null) {
                                imageCapture?.takePicture(
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val bmp = imageProxyToBitmap(image)
                                            image.close()
                                            if (bmp != null) {
                                                val quad = ImageProcessor.estimateDocumentQuad(bmp)
                                                drafts.add(CapturedPageDraft(bmp, quad))
                                                if (!batchMode) {
                                                    activeDraftIndex = drafts.lastIndex
                                                    editingMode = true
                                                } else {
                                                    viewModel.showMessage("Page ${drafts.size} captured")
                                                }
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            viewModel.showMessage("Capture failed: ${exception.message}")
                                        }
                                    },
                                )
                            } else {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        }
                        .testTag("scanner_shutter_btn"),
                )

                if (drafts.isNotEmpty()) {
                    Button(
                        onClick = {
                            activeDraftIndex = 0
                            editingMode = true
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        modifier = Modifier.testTag("scanner_review_btn"),
                    ) {
                        Text("Edit (${drafts.size})", color = Color.White)
                    }
                } else {
                    Spacer(modifier = Modifier.size(52.dp))
                }
            }
        }
    } else if (drafts.isNotEmpty()) {
        // POST-CAPTURE CROP / ROTATE / FILTER EDITOR
        val draft = drafts[activeDraftIndex.coerceIn(drafts.indices)]
        var previewBmp by remember(draft, showCropHandles) {
            mutableStateOf(if (showCropHandles) draft.rawBitmap else draft.renderProcessed())
        }

        LaunchedEffect(draft) {
            val txt = OnDeviceOcrEngine.recognizeText(previewBmp)
            val cls = OnDeviceOcrEngine.classify(txt, previewBmp.width, previewBmp.height)
            detectedCategory = cls.category
            if (docTitle.isBlank()) docTitle = cls.suggestedTitle
        }

        ObsidianBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = { editingMode = false }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Back to camera", tint = colors.textPrimary)
                    }
                    StatusPillBadge(text = "AI: $detectedCategory", color = CyanGlow)
                    Button(
                        onClick = {
                            val rendered = drafts.map { it.renderProcessed() }
                            val filters = drafts.map { it.filter.label }
                            viewModel.classifyAndSaveScan(
                                title = docTitle,
                                folderId = "",
                                bitmaps = rendered,
                                filters = filters,
                                lockToVault = saveToVault,
                            ) { saved ->
                                onSavedDocument(saved.id)
                            }
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        modifier = Modifier.testTag("scanner_save_btn"),
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save (${drafts.size})", color = Color.White)
                    }
                }

                // Page switcher for batch scans
                if (drafts.size > 1) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 6.dp),
                    ) {
                        items(drafts.indices.toList()) { idx ->
                            FilterChip(
                                selected = idx == activeDraftIndex,
                                onClick = { activeDraftIndex = idx },
                                label = { Text("Page ${idx + 1}") },
                            )
                        }
                    }
                }

                // Image Preview or Interactive 4-Corner Perspective Cropper
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF090F1A)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        bitmap = previewBmp.asImageBitmap(),
                        contentDescription = "Edited scan preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (showCropHandles) {
                        QuadCropOverlay(
                            quad = draft.quad,
                            onQuadChange = { updated ->
                                drafts[activeDraftIndex] = draft.copy(quad = updated)
                            },
                        )
                    }
                }

                // Controls Panel
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = docTitle,
                            onValueChange = { docTitle = it },
                            label = { Text("Document Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Filter Chips
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ScanFilter.entries) { filter ->
                                FilterChip(
                                    selected = draft.filter == filter,
                                    onClick = {
                                        drafts[activeDraftIndex] = draft.copy(filter = filter)
                                    },
                                    label = { Text(filter.label) },
                                )
                            }
                        }

                        // Brightness & Contrast
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Bright", style = MaterialTheme.typography.labelMedium, color = colors.textMuted, modifier = Modifier.width(56.dp))
                            Slider(
                                value = draft.brightness,
                                onValueChange = { drafts[activeDraftIndex] = draft.copy(brightness = it) },
                                valueRange = -50f..50f,
                                colors = SliderDefaults.colors(thumbColor = CyanGlow, activeTrackColor = ElectricBlue),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Contrast", style = MaterialTheme.typography.labelMedium, color = colors.textMuted, modifier = Modifier.width(56.dp))
                            Slider(
                                value = draft.contrast,
                                onValueChange = { drafts[activeDraftIndex] = draft.copy(contrast = it) },
                                valueRange = 0.6f..1.8f,
                                colors = SliderDefaults.colors(thumbColor = CyanGlow, activeTrackColor = ElectricBlue),
                                modifier = Modifier.weight(1f),
                            )
                        }

                        // Action Buttons Row (Crop, Rotate, Vault Lock)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedButton(
                                onClick = { showCropHandles = !showCropHandles },
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(Icons.Outlined.Crop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (showCropHandles) "Apply Crop" else "4-Corner Crop")
                            }
                            OutlinedButton(
                                onClick = {
                                    drafts[activeDraftIndex] = draft.copy(rotation = (draft.rotation + 90) % 360)
                                },
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(Icons.Outlined.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Rotate")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Lock, contentDescription = "Save to Vault", tint = FolderGreen, modifier = Modifier.size(18.dp))
                                Switch(checked = saveToVault, onCheckedChange = { saveToVault = it })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannerDetectionOverlay() {
    val transition = rememberInfiniteTransition(label = "scan_line")
    val scanProgress by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "laser_y",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val left = size.width * 0.1f
        val right = size.width * 0.9f
        val top = size.height * 0.18f
        val bottom = size.height * 0.82f

        val quadPath = Path().apply {
            moveTo(left, top)
            lineTo(right, top)
            lineTo(right, bottom)
            lineTo(left, bottom)
            close()
        }
        drawPath(quadPath, color = CyanGlow.copy(alpha = 0.08f))
        drawPath(quadPath, color = CyanGlow.copy(alpha = 0.75f), style = Stroke(width = 3.dp.toPx()))

        val laserY = size.height * scanProgress
        drawLine(
            brush = Brush.horizontalGradient(listOf(Color.Transparent, CyanGlow, ElectricBlue, Color.Transparent)),
            start = Offset(left, laserY),
            end = Offset(right, laserY),
            strokeWidth = 3.dp.toPx(),
        )
    }
}

@Composable
private fun QuadCropOverlay(
    quad: NormalizedQuad,
    onQuadChange: (NormalizedQuad) -> Unit,
) {
    var draggingCorner by remember { mutableIntStateOf(-1) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(quad) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val corners = listOf(quad.topLeft, quad.topRight, quad.bottomRight, quad.bottomLeft)
                        draggingCorner = corners.indexOfFirst { p ->
                            hypot((p.x * size.width - offset.x).toDouble(), (p.y * size.height - offset.y).toDouble()) < 72
                        }
                    },
                    onDragEnd = { draggingCorner = -1 },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val dx = dragAmount.x / size.width
                        val dy = dragAmount.y / size.height
                        fun PointF.shifted() = PointF((x + dx).coerceIn(0.02f, 0.98f), (y + dy).coerceIn(0.02f, 0.98f))
                        val updated = when (draggingCorner) {
                            0 -> quad.copy(topLeft = quad.topLeft.shifted())
                            1 -> quad.copy(topRight = quad.topRight.shifted())
                            2 -> quad.copy(bottomRight = quad.bottomRight.shifted())
                            3 -> quad.copy(bottomLeft = quad.bottomLeft.shifted())
                            else -> quad
                        }
                        onQuadChange(updated)
                    },
                )
            },
    ) {
        val pts = listOf(quad.topLeft, quad.topRight, quad.bottomRight, quad.bottomLeft)
            .map { Offset(it.x * size.width, it.y * size.height) }

        val path = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            lineTo(pts[1].x, pts[1].y)
            lineTo(pts[2].x, pts[2].y)
            lineTo(pts[3].x, pts[3].y)
            close()
        }
        drawPath(path, color = CyanGlow, style = Stroke(width = 2.5.dp.toPx()))
        pts.forEach { pt ->
            drawCircle(color = ElectricBlue, radius = 12.dp.toPx(), center = pt)
            drawCircle(color = Color.White, radius = 6.dp.toPx(), center = pt)
        }
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    val rot = image.imageInfo.rotationDegrees
    if (rot == 0) return raw
    val m = Matrix().apply { postRotate(rot.toFloat()) }
    return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
}
