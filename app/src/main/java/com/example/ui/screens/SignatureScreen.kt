package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.DocumentEntity
import com.example.data.local.SignatureEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.ObsidianBackground
import com.example.ui.theme.AsifTheme
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricBlue
import com.example.ui.viewmodel.MainViewModel
import java.io.File
import kotlin.math.roundToInt

private data class InkStroke(
    val points: List<Offset>,
    val color: Color,
    val widthPx: Float,
)

@Composable
fun SignatureScreen(
    initialDocId: String?,
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val colors = AsifTheme.colors
    val documents by viewModel.libraryDocuments.collectAsStateWithLifecycle()
    val savedSignatures by viewModel.signatures.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(if (initialDocId != null) 1 else 0) }
    var sigName by remember { mutableStateOf("My Ink Signature") }
    var inkColor by remember { mutableStateOf(Color(0xFF0F1B2D)) }
    var strokeWidth by remember { mutableFloatStateOf(10f) }

    val strokes = remember { mutableStateListOf<InkStroke>() }
    var activePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasWidthPx by remember { mutableFloatStateOf(800f) }
    var canvasHeightPx by remember { mutableFloatStateOf(400f) }

    var selectedDocId by remember(initialDocId, documents) {
        mutableStateOf(initialDocId ?: documents.firstOrNull()?.id)
    }
    var selectedSig by remember(savedSignatures) { mutableStateOf(savedSignatures.firstOrNull()) }
    var sigCenterX by remember { mutableFloatStateOf(0.55f) }
    var sigCenterY by remember { mutableFloatStateOf(0.76f) }
    var sigWidthNorm by remember { mutableFloatStateOf(0.36f) }

    ObsidianBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("signature_back")) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                }
                Text("Digital Signature", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            }

            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.Transparent,
                contentColor = CyanGlow,
            ) {
                Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Draw Signature") })
                Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Place & Flatten PDF") })
            }

            if (activeTab == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = sigName,
                        onValueChange = { sigName = it },
                        label = { Text("Signature Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Smooth Ink Drawing Pad
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.5.dp, ElectricBlue.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
                            .pointerInput(inkColor, strokeWidth) {
                                canvasWidthPx = size.width.toFloat().coerceAtLeast(1f)
                                canvasHeightPx = size.height.toFloat().coerceAtLeast(1f)
                                detectDragGestures(
                                    onDragStart = { start -> activePoints = listOf(start) },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        activePoints = activePoints + change.position
                                    },
                                    onDragEnd = {
                                        if (activePoints.isNotEmpty()) {
                                            strokes.add(InkStroke(activePoints, inkColor, strokeWidth))
                                            activePoints = emptyList()
                                        }
                                    },
                                )
                            }
                            .testTag("signature_canvas"),
                    ) {
                        if (strokes.isEmpty() && activePoints.isEmpty()) {
                            Text(
                                text = "Sign smoothly with your finger here",
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            fun drawStroke(pts: List<Offset>, color: Color, width: Float) {
                                if (pts.size < 2) return
                                val path = Path().apply {
                                    moveTo(pts.first().x, pts.first().y)
                                    for (i in 1 until pts.size) {
                                        val prev = pts[i - 1]
                                        val curr = pts[i]
                                        quadraticBezierTo(prev.x, prev.y, (prev.x + curr.x) / 2f, (prev.y + curr.y) / 2f)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = color,
                                    style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round),
                                )
                            }
                            strokes.forEach { drawStroke(it.points, it.color, it.widthPx) }
                            drawStroke(activePoints, inkColor, strokeWidth)
                        }
                    }

                    // Ink Colors & Thickness
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Ink Color", color = colors.textSecondary)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    listOf(
                                        Color(0xFF0F1B2D),
                                        Color(0xFF1E3A8A),
                                        ElectricBlue,
                                        Color(0xFFDC2626),
                                    ).forEach { c ->
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(c)
                                                .border(
                                                    width = if (inkColor == c) 3.dp else 1.dp,
                                                    color = if (inkColor == c) CyanGlow else Color.White,
                                                    shape = CircleShape,
                                                )
                                                .clickable { inkColor = c },
                                        )
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Stroke", color = colors.textSecondary, modifier = Modifier.width(56.dp))
                                Slider(
                                    value = strokeWidth,
                                    onValueChange = { strokeWidth = it },
                                    valueRange = 4f..22f,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = { strokes.clear() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    Text("Clear")
                                }
                                Button(
                                    enabled = strokes.isNotEmpty(),
                                    onClick = {
                                        val bmp = renderStrokesToBitmap(
                                            strokes.toList(),
                                            canvasWidthPx.roundToInt(),
                                            canvasHeightPx.roundToInt(),
                                        )
                                        viewModel.saveSignature(sigName, bmp)
                                        strokes.clear()
                                        activeTab = 1
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                ) {
                                    Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save Ink", color = Color.White)
                                }
                            }
                        }
                    }

                    // Saved Signatures Strip
                    if (savedSignatures.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(savedSignatures, key = { it.id }) { sig ->
                                SavedSigChip(
                                    sig = sig,
                                    selected = selectedSig?.id == sig.id,
                                    onSelect = { selectedSig = sig; activeTab = 1 },
                                    onDelete = { viewModel.deleteSignature(sig) },
                                )
                            }
                        }
                    }
                }
            } else {
                // PLACE & FLATTEN ON DOCUMENT PAGE
                val selectedDoc: DocumentEntity? = documents.firstOrNull { it.id == selectedDocId }
                val docPages by if (selectedDocId != null) {
                    viewModel.observePages(selectedDocId!!).collectAsStateWithLifecycle(initialValue = emptyList())
                } else {
                    remember { mutableStateOf(emptyList()) }
                }

                val pageBmp by produceState<Bitmap?>(initialValue = null, key1 = docPages.firstOrNull()?.id) {
                    val first = docPages.firstOrNull()
                    value = if (first != null) viewModel.repo.loadPageBitmap(first) else null
                }

                val sigBmp by produceState<Bitmap?>(initialValue = null, key1 = selectedSig?.imagePath) {
                    value = selectedSig?.imagePath?.let { path ->
                        if (File(path).exists()) BitmapFactory.decodeFile(path) else null
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(documents, key = { it.id }) { doc ->
                            FilterChip(
                                selected = doc.id == selectedDocId,
                                onClick = { selectedDocId = doc.id },
                                label = { Text(doc.title, maxLines = 1) },
                            )
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(savedSignatures, key = { it.id }) { sig ->
                            SavedSigChip(
                                sig = sig,
                                selected = selectedSig?.id == sig.id,
                                onSelect = { selectedSig = sig },
                                onDelete = { viewModel.deleteSignature(sig) },
                            )
                        }
                    }

                    // Interactive Page Preview + Draggable Signature Overlay
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF0E1726)),
                        contentAlignment = Alignment.Center,
                    ) {
                        val boxW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                        val boxH = constraints.maxHeight.toFloat().coerceAtLeast(1f)

                        if (pageBmp != null) {
                            Image(
                                bitmap = pageBmp!!.asImageBitmap(),
                                contentDescription = selectedDoc?.title ?: "Document page",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Text("Select a document to sign", color = colors.textMuted)
                        }

                        if (sigBmp != null && pageBmp != null) {
                            val overlayW = (boxW * sigWidthNorm).roundToInt()
                            val aspect = sigBmp!!.height.toFloat() / sigBmp!!.width.coerceAtLeast(1).toFloat()
                            val overlayH = (overlayW * aspect).roundToInt()

                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            x = (sigCenterX * boxW - overlayW / 2f - boxW / 2f).roundToInt(),
                                            y = (sigCenterY * boxH - overlayH / 2f - boxH / 2f).roundToInt(),
                                        )
                                    }
                                    .size(width = (overlayW / 2.6f).dp, height = (overlayH / 2.6f).dp)
                                    .border(1.5.dp, CyanGlow, RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            sigCenterX = (sigCenterX + dragAmount.x / boxW).coerceIn(0.15f, 0.85f)
                                            sigCenterY = (sigCenterY + dragAmount.y / boxH).coerceIn(0.15f, 0.85f)
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    bitmap = sigBmp!!.asImageBitmap(),
                                    contentDescription = "Draggable signature overlay",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize().padding(4.dp),
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Size", color = colors.textMuted, modifier = Modifier.width(44.dp))
                        Slider(
                            value = sigWidthNorm,
                            onValueChange = { sigWidthNorm = it },
                            valueRange = 0.18f..0.65f,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Button(
                        enabled = selectedDoc != null && sigBmp != null,
                        onClick = {
                            if (selectedDoc != null && sigBmp != null) {
                                viewModel.flattenSignatureAndShare(
                                    docId = selectedDoc.id,
                                    pageIndex = 0,
                                    signatureBitmap = sigBmp!!,
                                    centerXNorm = sigCenterX,
                                    centerYNorm = sigCenterY,
                                    widthNorm = sigWidthNorm,
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("flatten_sign_pdf_btn"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    ) {
                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Flatten Signature & Export PDF", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedSigChip(
    sig: SignatureEntity,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    GlassCard(
        cornerRadius = 14.dp,
        featuredGlow = selected,
        onClick = onSelect,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Outlined.Draw, contentDescription = null, tint = CyanGlow, modifier = Modifier.size(16.dp))
            Text(sig.name, style = MaterialTheme.typography.labelMedium, color = AsifTheme.colors.textPrimary)
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Delete signature",
                tint = AsifTheme.colors.textMuted,
                modifier = Modifier
                    .size(16.dp)
                    .clickable(onClick = onDelete),
            )
        }
    }
}

private fun renderStrokesToBitmap(strokes: List<InkStroke>, w: Int, h: Int): Bitmap {
    val bmp = Bitmap.createBitmap(w.coerceAtLeast(300), h.coerceAtLeast(160), Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    canvas.drawColor(AndroidColor.TRANSPARENT)
    strokes.forEach { stroke ->
        if (stroke.points.size < 2) return@forEach
        val p = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = stroke.color.toArgb()
            style = AndroidPaint.Style.STROKE
            strokeWidth = stroke.widthPx
            strokeCap = AndroidPaint.Cap.ROUND
            strokeJoin = AndroidPaint.Join.ROUND
        }
        val path = AndroidPath().apply {
            moveTo(stroke.points.first().x, stroke.points.first().y)
            for (i in 1 until stroke.points.size) {
                val prev = stroke.points[i - 1]
                val curr = stroke.points[i]
                quadTo(prev.x, prev.y, (prev.x + curr.x) / 2f, (prev.y + curr.y) / 2f)
            }
        }
        canvas.drawPath(path, p)
    }
    return bmp
}
