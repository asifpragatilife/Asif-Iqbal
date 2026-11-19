package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MergeType
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.DocumentEntity
import com.example.data.preferences.ScanQuality
import com.example.pdf.ExportFormat
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowingBrandTitle
import com.example.ui.components.StatusPillBadge
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.AsifTheme
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.FolderGreen
import com.example.ui.theme.FolderOrange
import com.example.ui.theme.FolderPurple
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

private enum class ToolId {
    SIGN_PDF,
    CONVERT_JPG,
    MERGE_FILES,
    SPLIT_PDF,
    COMPRESS_PDF,
    EDIT_PDF,
    ORGANIZE_PAGES,
    REDACT,
    QR_HUB,
    IMAGE_TO_PDF,
    IMPORT_PDF,
}

private data class ToolTileSpec(
    val id: ToolId,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
)

@Composable
fun ToolsScreen(
    documents: List<DocumentEntity>,
    viewModel: MainViewModel,
    onOpenSignature: (String?) -> Unit,
    onOpenRedact: (String?) -> Unit,
    onOpenQrHub: () -> Unit,
    onOpenDocument: (String) -> Unit,
) {
    val colors = AsifTheme.colors
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeToolDialog by remember { mutableStateOf<ToolId?>(null) }
    var compressQuality by remember { mutableFloatStateOf(72f) }

    val pdfImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.importPdf(uri, "Imported_${System.currentTimeMillis() % 10000}") { doc ->
                doc?.let { onOpenDocument(it.id) }
            }
        }
    }

    val imageToPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10),
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val bitmaps = uris.mapNotNull { uri ->
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                }
                if (bitmaps.isNotEmpty()) {
                    val saved = viewModel.repo.saveScannedDocument(
                        title = "Image_Bundle_${System.currentTimeMillis() % 1000}",
                        folderId = "folder_work",
                        docType = "PDF",
                        category = "Document",
                        tags = listOf("Image to PDF"),
                        pageBitmaps = bitmaps,
                        pageFilters = bitmaps.map { "Original" },
                        lockToVault = false,
                        quality = ScanQuality.HIGH,
                    )
                    viewModel.showMessage("Created ${saved.pageCount}-page PDF")
                    onOpenDocument(saved.id)
                }
            }
        }
    }

    val tools = remember {
        listOf(
            ToolTileSpec(ToolId.CONVERT_JPG, "Convert to JPG", "Export crisp image", Icons.Outlined.Image, CyanGlow),
            ToolTileSpec(ToolId.MERGE_FILES, "Merge Files", "Combine multiple docs", Icons.Outlined.MergeType, ElectricBlue),
            ToolTileSpec(ToolId.SPLIT_PDF, "Split PDF", "Divide multi-page PDF", Icons.Outlined.CallSplit, FolderOrange),
            ToolTileSpec(ToolId.COMPRESS_PDF, "Compress PDF", "Reduce file size", Icons.Outlined.Compress, FolderGreen),
            ToolTileSpec(ToolId.EDIT_PDF, "Edit PDF", "OCR, rename & tags", Icons.Outlined.EditNote, ElectricBlue),
            ToolTileSpec(ToolId.ORGANIZE_PAGES, "Organize Pages", "Reorder or remove", Icons.Outlined.DashboardCustomize, FolderPurple),
            ToolTileSpec(ToolId.REDACT, "Redact", "Permanent black-box burn", Icons.Outlined.VisibilityOff, FolderOrange),
            ToolTileSpec(ToolId.QR_HUB, "QR Hub", "Scan & generate codes", Icons.Outlined.QrCodeScanner, CyanGlow),
            ToolTileSpec(ToolId.IMAGE_TO_PDF, "Image to PDF", "Convert photos offline", Icons.Outlined.PhotoLibrary, FolderGreen),
            ToolTileSpec(ToolId.IMPORT_PDF, "Import PDF", "Open local PDF file", Icons.Outlined.FileOpen, FolderPurple),
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 110.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                GlowingBrandTitle("PDF Toolkit")
                Text(
                    text = "All PDF, OCR, signature & redaction engines run locally on-device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                )
            }
        }

        // Featured Hero Card: Sign PDF with electric blue glowing border (Image 2 style)
        item(span = { GridItemSpan(maxLineSpan) }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tool_sign_pdf_hero"),
                cornerRadius = 24.dp,
                featuredGlow = true,
                onClick = { onOpenSignature(documents.firstOrNull()?.id) },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(ElectricBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Draw,
                            contentDescription = strings.signPdf,
                            tint = CyanGlow,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        StatusPillBadge(text = strings.featuredTool, color = CyanGlow)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.signPdf,
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = strings.signPdfDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
        }

        // 2-Column Tool Tiles
        items(tools, key = { it.id.name }) { tool ->
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tool_${tool.id.name.lowercase()}"),
                cornerRadius = 20.dp,
                onClick = {
                    when (tool.id) {
                        ToolId.QR_HUB -> onOpenQrHub()
                        ToolId.REDACT -> onOpenRedact(documents.firstOrNull()?.id)
                        ToolId.IMPORT_PDF -> pdfImportLauncher.launch(arrayOf("application/pdf"))
                        ToolId.IMAGE_TO_PDF -> imageToPdfLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                        else -> activeToolDialog = tool.id
                    }
                },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(tool.accent.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = tool.icon,
                            contentDescription = tool.title,
                            tint = tool.accent,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Text(
                        text = tool.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = tool.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted,
                    )
                }
            }
        }
    }

    // Tool Pickers & Execution Dialogs
    when (activeToolDialog) {
        ToolId.MERGE_FILES -> {
            val selected = remember { mutableStateListOf<String>() }
            var mergeTitle by remember { mutableStateOf("Merged_Packet") }
            AlertDialog(
                onDismissRequest = { activeToolDialog = null },
                containerColor = colors.glassSurfaceElevated,
                title = { Text("Merge Files (Pick 2+)") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = mergeTitle,
                            onValueChange = { mergeTitle = it },
                            label = { Text("Output Title") },
                            singleLine = true,
                        )
                        documents.take(6).forEach { doc ->
                            val checked = selected.contains(doc.id)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = {
                                        if (it) selected.add(doc.id) else selected.remove(doc.id)
                                    },
                                )
                                Text("${doc.title} (${doc.pageCount}p)", modifier = Modifier.weight(1f))
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        enabled = selected.size >= 2,
                        onClick = {
                            viewModel.mergeDocuments(selected.toList(), mergeTitle) { merged ->
                                activeToolDialog = null
                                merged?.let { onOpenDocument(it.id) }
                            }
                        },
                    ) { Text("Merge") }
                },
                dismissButton = {
                    TextButton(onClick = { activeToolDialog = null }) { Text("Cancel") }
                },
            )
        }

        ToolId.CONVERT_JPG, ToolId.EDIT_PDF, ToolId.ORGANIZE_PAGES, ToolId.SPLIT_PDF, ToolId.COMPRESS_PDF -> {
            val tool = activeToolDialog!!
            AlertDialog(
                onDismissRequest = { activeToolDialog = null },
                containerColor = colors.glassSurfaceElevated,
                title = {
                    Text(
                        when (tool) {
                            ToolId.CONVERT_JPG -> "Convert to JPG"
                            ToolId.SPLIT_PDF -> "Split Multi-Page PDF"
                            ToolId.COMPRESS_PDF -> "Compress PDF Quality"
                            ToolId.ORGANIZE_PAGES -> "Organize Pages"
                            else -> "Select Document to Edit"
                        },
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (tool == ToolId.COMPRESS_PDF) {
                            Text("Compression Quality: ${compressQuality.toInt()}%")
                            Slider(
                                value = compressQuality,
                                onValueChange = { compressQuality = it },
                                valueRange = 35f..90f,
                            )
                        }
                        if (documents.isEmpty()) {
                            Text("No documents available yet.")
                        } else {
                            documents.take(6).forEach { doc ->
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    cornerRadius = 14.dp,
                                    onClick = {
                                        when (tool) {
                                            ToolId.CONVERT_JPG -> {
                                                viewModel.exportAndShare(doc.id, ExportFormat.JPG, 92)
                                                activeToolDialog = null
                                            }
                                            ToolId.COMPRESS_PDF -> {
                                                viewModel.exportAndShare(doc.id, ExportFormat.PDF, compressQuality.toInt())
                                                activeToolDialog = null
                                            }
                                            ToolId.SPLIT_PDF -> {
                                                viewModel.splitDocument(doc.id, 0) {
                                                    activeToolDialog = null
                                                }
                                            }
                                            else -> {
                                                activeToolDialog = null
                                                onOpenDocument(doc.id)
                                            }
                                        }
                                    },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            "${doc.title} (${doc.pageCount}p)",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = colors.textPrimary,
                                            modifier = Modifier.weight(1f),
                                        )
                                        StatusPillBadge(text = doc.docType, color = ElectricBlue)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { activeToolDialog = null }) { Text("Close") }
                },
            )
        }

        else -> Unit
    }
}
