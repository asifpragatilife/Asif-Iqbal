package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Preview
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pdf.DocumentExportEngine
import com.example.pdf.ExportFormat
import com.example.pdf.NormalizedRect
import com.example.ui.components.ObsidianBackground
import com.example.ui.components.StatusPillBadge
import com.example.ui.theme.AsifTheme
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.FolderOrange
import com.example.ui.viewmodel.MainViewModel
import kotlin.math.max
import kotlin.math.min

@Composable
fun RedactionScreen(
    initialDocId: String?,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onDoneDocument: (String) -> Unit,
) {
    BackHandler(onBack = onBack)

    val colors = AsifTheme.colors
    val context = LocalContext.current
    val documents by viewModel.libraryDocuments.collectAsStateWithLifecycle()

    var selectedDocId by remember(initialDocId, documents) {
        mutableStateOf(initialDocId ?: documents.firstOrNull()?.id)
    }
    val docPages by if (selectedDocId != null) {
        viewModel.observePages(selectedDocId!!).collectAsStateWithLifecycle(initialValue = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    val pageBmp by produceState<Bitmap?>(initialValue = null, key1 = docPages.firstOrNull()?.id) {
        val first = docPages.firstOrNull()
        value = if (first != null) viewModel.repo.loadPageBitmap(first) else null
    }

    val boxes = remember { mutableStateListOf<NormalizedRect>() }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragCurrent by remember { mutableStateOf<Offset?>(null) }
    var previewBurned by remember { mutableStateOf(false) }

    val renderedPreview = remember(pageBmp, boxes.toList(), previewBurned) {
        if (previewBurned && pageBmp != null) {
            DocumentExportEngine.burnRedactionsOntoBitmap(pageBmp!!, boxes.toList())
        } else {
            pageBmp
        }
    }

    ObsidianBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("redact_back")) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                    }
                    Text("Redaction Tool", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                }
                StatusPillBadge(text = "PIXEL BURN", color = FolderOrange, icon = Icons.Outlined.Shield)
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(documents, key = { it.id }) { doc ->
                    FilterChip(
                        selected = doc.id == selectedDocId,
                        onClick = {
                            selectedDocId = doc.id
                            boxes.clear()
                            previewBurned = false
                        },
                        label = { Text(doc.title, maxLines = 1) },
                    )
                }
            }

            Text(
                text = "Drag across sensitive text to draw black redaction boxes. Pixels are permanently overwritten on export.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0E1726)),
                contentAlignment = Alignment.Center,
            ) {
                if (renderedPreview != null) {
                    Image(
                        bitmap = renderedPreview.asImageBitmap(),
                        contentDescription = "Redaction canvas",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )

                    if (!previewBurned) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { start ->
                                            dragStart = start
                                            dragCurrent = start
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            dragCurrent = change.position
                                        },
                                        onDragEnd = {
                                            val s = dragStart
                                            val e = dragCurrent
                                            if (s != null && e != null && size.width > 0 && size.height > 0) {
                                                val rect = NormalizedRect(
                                                    left = (min(s.x, e.x) / size.width).coerceIn(0f, 1f),
                                                    top = (min(s.y, e.y) / size.height).coerceIn(0f, 1f),
                                                    right = (max(s.x, e.x) / size.width).coerceIn(0f, 1f),
                                                    bottom = (max(s.y, e.y) / size.height).coerceIn(0f, 1f),
                                                )
                                                if (rect.right - rect.left > 0.02f && rect.bottom - rect.top > 0.015f) {
                                                    boxes.add(rect)
                                                }
                                            }
                                            dragStart = null
                                            dragCurrent = null
                                        },
                                    )
                                }
                                .testTag("redaction_canvas"),
                        ) {
                            boxes.forEach { b ->
                                val tl = Offset(b.left * size.width, b.top * size.height)
                                val sz = Size((b.right - b.left) * size.width, (b.bottom - b.top) * size.height)
                                drawRect(color = Color.Black, topLeft = tl, size = sz)
                                drawRect(color = CyanGlow, topLeft = tl, size = sz, style = Stroke(width = 2.dp.toPx()))
                            }
                            val s = dragStart
                            val e = dragCurrent
                            if (s != null && e != null) {
                                val tl = Offset(min(s.x, e.x), min(s.y, e.y))
                                val sz = Size(kotlin.math.abs(e.x - s.x), kotlin.math.abs(e.y - s.y))
                                drawRect(color = Color.Black.copy(alpha = 0.8f), topLeft = tl, size = sz)
                                drawRect(color = FolderOrange, topLeft = tl, size = sz, style = Stroke(width = 2.dp.toPx()))
                            }
                        }
                    }
                } else {
                    Text("No document page selected", color = colors.textMuted)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    enabled = boxes.isNotEmpty(),
                    onClick = { if (boxes.isNotEmpty()) boxes.removeAt(boxes.lastIndex) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Undo")
                }
                OutlinedButton(
                    enabled = boxes.isNotEmpty(),
                    onClick = { boxes.clear(); previewBurned = false },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear")
                }
                OutlinedButton(
                    enabled = boxes.isNotEmpty(),
                    onClick = { previewBurned = !previewBurned },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Outlined.Preview, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (previewBurned) "Edit" else "Preview")
                }
            }

            Button(
                enabled = selectedDocId != null && boxes.isNotEmpty(),
                onClick = {
                    selectedDocId?.let { id ->
                        viewModel.applyPermanentRedaction(id, 0, boxes.toList()) { saved, pdf ->
                            if (pdf != null) {
                                DocumentExportEngine.shareFile(context, pdf, ExportFormat.PDF.mimeType, pdf.nameWithoutExtension)
                            }
                            saved?.let { onDoneDocument(it.id) }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("burn_redaction_btn"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
            ) {
                Text("Permanently Burn & Export (${boxes.size} boxes)", color = Color.White)
            }
        }
    }
}
