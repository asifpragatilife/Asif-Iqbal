package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.PageEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.ObsidianBackground
import com.example.ui.components.StatusPillBadge
import com.example.ui.theme.AsifTheme
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.FolderGreen
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun DocumentDetailScreen(
    docId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenSign: () -> Unit,
    onOpenRedact: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val colors = AsifTheme.colors
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val doc by viewModel.observeDocument(docId).collectAsStateWithLifecycle(initialValue = null)
    val pages by viewModel.observePages(docId).collectAsStateWithLifecycle(initialValue = emptyList())

    var activeTab by remember { mutableStateOf(0) } // 0 = Pages, 1 = OCR Text
    var showRenameDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var ocrSearch by remember { mutableStateOf("") }

    val addPagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                }
                if (bmp != null) {
                    viewModel.repo.addPageToDocument(docId, bmp)
                    viewModel.showMessage("Page appended & OCR updated")
                }
            }
        }
    }

    ObsidianBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back")) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = doc?.title ?: "Document",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${doc?.category ?: ""} • ${pages.size} page(s) • ${doc?.docType ?: "PDF"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted,
                    )
                }
                IconButton(onClick = { showRenameDialog = true }) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Rename", tint = colors.textSecondary)
                }
                IconButton(onClick = {
                    doc?.let {
                        if (it.isLockedInVault) viewModel.unlockDocumentFromVault(it.id)
                        else viewModel.lockDocumentsToVault(listOf(it.id))
                    }
                }) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = "Toggle Vault Lock",
                        tint = if (doc?.isLockedInVault == true) FolderGreen else colors.textSecondary,
                    )
                }
                IconButton(onClick = onOpenExport, modifier = Modifier.testTag("detail_share")) {
                    Icon(Icons.Outlined.Share, contentDescription = "Export & Share", tint = CyanGlow)
                }
            }

            // Tags strip
            val tags = remember(doc?.tagsCsv) {
                doc?.tagsCsv?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tags.take(4).forEach { tag ->
                    StatusPillBadge(text = tag, color = ElectricBlue)
                }
                TextButton(onClick = { showTagDialog = true }) {
                    Text("+ Tags", style = MaterialTheme.typography.labelMedium, color = CyanGlow)
                }
            }

            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.Transparent,
                contentColor = CyanGlow,
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Pages (${pages.size})") },
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("OCR Text") },
                )
            }

            if (activeTab == 0) {
                if (pages.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Loading pages...", color = colors.textMuted)
                    }
                } else {
                    val pagerState = rememberPagerState(pageCount = { pages.size })
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { idx ->
                            val page = pages[idx]
                            PageBitmapViewer(page = page, viewModel = viewModel)
                        }
                        StatusPillBadge(
                            text = "Page ${pagerState.currentPage + 1} / ${pages.size}",
                            color = CyanGlow,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp),
                        )
                    }

                    // Page controls & Quick Actions
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        cornerRadius = 20.dp,
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val curIdx = pagerState.currentPage
                                IconButton(
                                    enabled = curIdx > 0,
                                    onClick = {
                                        scope.launch {
                                            viewModel.repo.swapPageOrder(docId, curIdx, curIdx - 1)
                                            pagerState.animateScrollToPage(curIdx - 1)
                                        }
                                    },
                                ) {
                                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Move Left", tint = colors.textPrimary)
                                }
                                IconButton(
                                    enabled = curIdx < pages.lastIndex,
                                    onClick = {
                                        scope.launch {
                                            viewModel.repo.swapPageOrder(docId, curIdx, curIdx + 1)
                                            pagerState.animateScrollToPage(curIdx + 1)
                                        }
                                    },
                                ) {
                                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Move Right", tint = colors.textPrimary)
                                }
                                IconButton(onClick = {
                                    addPagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }) {
                                    Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = "Add Page", tint = CyanGlow)
                                }
                                IconButton(
                                    enabled = pages.size > 1,
                                    onClick = {
                                        pages.getOrNull(curIdx)?.let { p ->
                                            scope.launch { viewModel.repo.deletePage(docId, p.id) }
                                        }
                                    },
                                ) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete Page", tint = colors.danger)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                OutlinedButton(
                                    onClick = onOpenSign,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    Icon(Icons.Outlined.Draw, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sign")
                                }
                                OutlinedButton(
                                    onClick = onOpenRedact,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    Icon(Icons.Outlined.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Redact")
                                }
                                Button(
                                    onClick = onOpenExport,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                ) {
                                    Text("Export", color = Color.White)
                                }
                            }
                        }
                    }
                }
            } else {
                // Searchable, Selectable OCR Text View
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = ocrSearch,
                            onValueChange = { ocrSearch = it },
                            placeholder = { Text("Search inside OCR text...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = { viewModel.runOcr(docId) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        ) {
                            Icon(Icons.Outlined.TextFields, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan", color = Color.White)
                        }
                    }

                    val fullText = doc?.ocrText.orEmpty()
                    val displayText = remember(fullText, ocrSearch) {
                        if (ocrSearch.isBlank()) fullText
                        else fullText.lines().filter { it.contains(ocrSearch, ignoreCase = true) }
                            .joinToString("\n").ifBlank { "No lines matching \"$ocrSearch\"" }
                    }

                    GlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        cornerRadius = 20.dp,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            SelectionContainer {
                                Text(
                                    text = displayText.ifBlank { "Tap Scan above to run offline ML Kit text recognition." },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colors.textPrimary,
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            clipboard.setText(AnnotatedString(fullText))
                            viewModel.showMessage("OCR text copied")
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy All Text", color = Color.White)
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        var newName by remember(doc?.title) { mutableStateOf(doc?.title.orEmpty()) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = colors.glassSurfaceElevated,
            title = { Text("Rename Document") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text("Title") },
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.renameDocument(docId, newName)
                    showRenameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showTagDialog) {
        var rawTags by remember(doc?.tagsCsv) { mutableStateOf(doc?.tagsCsv.orEmpty()) }
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            containerColor = colors.glassSurfaceElevated,
            title = { Text("Edit Tags (comma-separated)") },
            text = {
                OutlinedTextField(
                    value = rawTags,
                    onValueChange = { rawTags = it },
                    label = { Text("Tags") },
                )
            },
            confirmButton = {
                Button(onClick = {
                    val list = rawTags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    viewModel.updateTags(docId, list)
                    showTagDialog = false
                }) { Text("Update") }
            },
            dismissButton = {
                TextButton(onClick = { showTagDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun PageBitmapViewer(
    page: PageEntity,
    viewModel: MainViewModel,
) {
    val bmp by produceState<Bitmap?>(initialValue = null, key1 = page.id, key2 = page.imagePath) {
        value = viewModel.repo.loadPageBitmap(page)
    }

    GlassCard(modifier = Modifier.fillMaxSize(), cornerRadius = 20.dp) {
        if (bmp != null) {
            Image(
                bitmap = bmp!!.asImageBitmap(),
                contentDescription = "Document page ${page.pageIndex + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Decrypting page...", color = AsifTheme.colors.textMuted)
            }
        }
    }
}
