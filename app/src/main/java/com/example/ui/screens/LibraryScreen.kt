package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DocumentEntity
import com.example.data.local.FolderEntity
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
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.folderColorForKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    documents: List<DocumentEntity>,
    folders: List<FolderEntity>,
    onDocumentClick: (String) -> Unit,
    onCreateFolder: (String, String) -> Unit,
    onLockToVault: (List<String>) -> Unit,
    onMoveToFolder: (List<String>, String) -> Unit,
    onDeleteDocuments: (List<String>) -> Unit,
    onExportDocument: (String) -> Unit,
    onOpenVault: () -> Unit,
) {
    val colors = AsifTheme.colors
    val strings = LocalAppStrings.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var recentOnlyFilter by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(true) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    val folderMap = remember(folders) { folders.associateBy { it.id } }

    val filteredDocs = remember(documents, searchQuery, selectedFolderId, selectedTypeFilter, recentOnlyFilter) {
        val sevenDaysAgo = System.currentTimeMillis() - 7L * 86_400_000L
        documents.filter { doc ->
            val matchFolder = selectedFolderId == null || doc.folderId == selectedFolderId
            val matchType = selectedTypeFilter == "ALL" || doc.docType.equals(selectedTypeFilter, ignoreCase = true)
            val matchRecent = !recentOnlyFilter || doc.updatedAt >= sevenDaysAgo
            val q = searchQuery.trim().lowercase()
            val matchQuery = q.isEmpty() ||
                doc.title.lowercase().contains(q) ||
                doc.ocrText.lowercase().contains(q) ||
                doc.tagsCsv.lowercase().contains(q) ||
                doc.category.lowercase().contains(q)
            matchFolder && matchType && matchRecent && matchQuery
        }
    }

    val multiSelectActive = selectedIds.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                GlowingBrandTitle(strings.appName)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = FolderGreen,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = "${strings.storedOnDevice} • ${strings.noInternetNeeded}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textMuted,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { isGridView = !isGridView },
                    modifier = Modifier.testTag("toggle_grid_list"),
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.Outlined.ViewList else Icons.Outlined.GridView,
                        contentDescription = "Toggle Grid or List",
                        tint = colors.textSecondary,
                    )
                }
                IconButton(
                    onClick = onOpenVault,
                    modifier = Modifier.testTag("header_vault_shortcut"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "Open Obsidian Vault",
                        tint = CyanGlow,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Rounded Glass Search Bar with Filter Icon
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            cornerRadius = 26.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = CyanGlow,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = strings.searchPlaceholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
                        cursorBrush = SolidColor(CyanGlow),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("library_search_input"),
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Clear search", tint = colors.textMuted)
                    }
                }
                IconButton(
                    onClick = { showFilterSheet = true },
                    modifier = Modifier.testTag("library_filter_button"),
                ) {
                    Icon(
                        Icons.Outlined.FilterList,
                        contentDescription = "Filters",
                        tint = if (selectedTypeFilter != "ALL" || recentOnlyFilter) CyanGlow else colors.textSecondary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Multi-Select Action Bar
        AnimatedVisibility(visible = multiSelectActive) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                cornerRadius = 18.dp,
                featuredGlow = true,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Cancel selection", tint = colors.textPrimary)
                        }
                        Text(
                            text = "${selectedIds.size} selected",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                        )
                    }
                    Row {
                        IconButton(onClick = {
                            selectedIds.firstOrNull()?.let { onExportDocument(it) }
                            selectedIds = emptySet()
                        }) {
                            Icon(Icons.Outlined.Share, contentDescription = "Share", tint = CyanGlow)
                        }
                        IconButton(onClick = { showMoveDialog = true }) {
                            Icon(Icons.Outlined.DriveFileMove, contentDescription = "Move", tint = colors.textPrimary)
                        }
                        IconButton(onClick = {
                            onLockToVault(selectedIds.toList())
                            selectedIds = emptySet()
                        }) {
                            Icon(Icons.Outlined.Lock, contentDescription = "Lock to Vault", tint = FolderGreen)
                        }
                        IconButton(onClick = {
                            onDeleteDocuments(selectedIds.toList())
                            selectedIds = emptySet()
                        }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = colors.danger)
                        }
                    }
                }
            }
        }

        // Colorful Folder Tabs Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 8.dp),
        ) {
            item {
                FolderTabChip(
                    label = strings.allFolders,
                    accent = ElectricBlue,
                    selected = selectedFolderId == null,
                    onClick = { selectedFolderId = null },
                )
            }
            items(folders, key = { it.id }) { folder ->
                FolderTabChip(
                    label = folder.name,
                    accent = folderColorForKey(folder.colorKey),
                    selected = selectedFolderId == folder.id,
                    onClick = {
                        selectedFolderId = if (selectedFolderId == folder.id) null else folder.id
                    },
                )
            }
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.glassSurface,
                    modifier = Modifier
                        .height(36.dp)
                        .clickable { showNewFolderDialog = true }
                        .testTag("new_folder_chip"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.CreateNewFolder,
                            contentDescription = "New folder",
                            tint = CyanGlow,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            strings.newFolder,
                            style = MaterialTheme.typography.labelMedium,
                            color = CyanGlow,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredDocs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                GlassCard(modifier = Modifier.padding(16.dp), cornerRadius = 24.dp) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Outlined.Description,
                            contentDescription = null,
                            tint = CyanGlow,
                            modifier = Modifier.size(44.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No matching documents",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Tap + below to scan a page or clear active search filters.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textMuted,
                        )
                    }
                }
            }
        } else if (isGridView) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 110.dp),
            ) {
                items(filteredDocs, key = { it.id }) { doc ->
                    val folderColor = folderColorForKey(folderMap[doc.folderId]?.colorKey ?: "blue")
                    val isSelected = selectedIds.contains(doc.id)
                    DocumentGridCard(
                        doc = doc,
                        folderColor = folderColor,
                        selected = isSelected,
                        onClick = {
                            if (multiSelectActive) {
                                selectedIds = if (isSelected) selectedIds - doc.id else selectedIds + doc.id
                            } else {
                                onDocumentClick(doc.id)
                            }
                        },
                        onLongClick = {
                            selectedIds = if (isSelected) selectedIds - doc.id else selectedIds + doc.id
                        },
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 110.dp),
            ) {
                items(filteredDocs, key = { it.id }) { doc ->
                    val folderColor = folderColorForKey(folderMap[doc.folderId]?.colorKey ?: "blue")
                    val isSelected = selectedIds.contains(doc.id)
                    DocumentListCard(
                        doc = doc,
                        folderColor = folderColor,
                        selected = isSelected,
                        onClick = {
                            if (multiSelectActive) {
                                selectedIds = if (isSelected) selectedIds - doc.id else selectedIds + doc.id
                            } else {
                                onDocumentClick(doc.id)
                            }
                        },
                        onLongClick = {
                            selectedIds = if (isSelected) selectedIds - doc.id else selectedIds + doc.id
                        },
                    )
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = colors.glassSurfaceElevated,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Filter Library", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                Text("FILE FORMAT", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL", "PDF", "DOCX", "XLSX", "JPG").forEach { fmt ->
                        FilterChip(
                            selected = selectedTypeFilter == fmt,
                            onClick = { selectedTypeFilter = fmt },
                            label = { Text(fmt) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricBlue,
                                selectedLabelColor = Color.White,
                            ),
                        )
                    }
                }
                Text("DATE", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !recentOnlyFilter,
                        onClick = { recentOnlyFilter = false },
                        label = { Text("All Time") },
                    )
                    FilterChip(
                        selected = recentOnlyFilter,
                        onClick = { recentOnlyFilter = true },
                        label = { Text("Last 7 Days") },
                    )
                }
                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                ) {
                    Text("Apply Filters", color = Color.White)
                }
            }
        }
    }

    if (showNewFolderDialog) {
        var name by remember { mutableStateOf("") }
        var colorKey by remember { mutableStateOf("blue") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            containerColor = colors.glassSurfaceElevated,
            title = { Text("New Folder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Folder Name") },
                        singleLine = true,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("blue", "green", "orange", "purple").forEach { key ->
                            val c = folderColorForKey(key)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(
                                        width = if (colorKey == key) 3.dp else 0.dp,
                                        color = Color.White,
                                        shape = CircleShape,
                                    )
                                    .clickable { colorKey = key },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) onCreateFolder(name, colorKey)
                        showNewFolderDialog = false
                    },
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showMoveDialog) {
        AlertDialog(
            onDismissRequest = { showMoveDialog = false },
            containerColor = colors.glassSurfaceElevated,
            title = { Text("Move to Folder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    folders.forEach { folder ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 14.dp,
                            onClick = {
                                onMoveToFolder(selectedIds.toList(), folder.id)
                                selectedIds = emptySet()
                                showMoveDialog = false
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Outlined.Folder,
                                    contentDescription = null,
                                    tint = folderColorForKey(folder.colorKey),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(folder.name, color = colors.textPrimary)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMoveDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun FolderTabChip(
    label: String,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = AsifTheme.colors
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) accent.copy(alpha = 0.24f) else colors.glassSurface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (selected) accent else colors.glassBorder,
        ),
        modifier = Modifier
            .height(36.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) colors.textPrimary else colors.textSecondary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DocumentGridCard(
    doc: DocumentEntity,
    folderColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colors = AsifTheme.colors
    val thumbBitmap by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = doc.thumbnailPath) {
        value = withContext(Dispatchers.IO) {
            if (doc.thumbnailPath.isNotBlank() && File(doc.thumbnailPath).exists()) {
                BitmapFactory.decodeFile(doc.thumbnailPath)
            } else null
        }
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("doc_card_${doc.id}"),
        cornerRadius = 20.dp,
        selected = selected,
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Colorful folder top tab accent strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(folderColor),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
                    .background(Color(0xFF0E1726)),
            ) {
                if (thumbBitmap != null) {
                    Image(
                        bitmap = thumbBitmap!!.asImageBitmap(),
                        contentDescription = doc.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        Icons.Outlined.Description,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.align(Alignment.Center).size(36.dp),
                    )
                }
                // File Type Badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = docTypeColor(doc.docType).copy(alpha = 0.92f),
                ) {
                    Text(
                        text = doc.docType,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                }
                if (selected) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = "Selected",
                        tint = CyanGlow,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .size(24.dp),
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = doc.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatShortDate(doc.updatedAt),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                        color = colors.textMuted,
                    )
                    Text(
                        text = "${doc.pageCount}p",
                        style = MaterialTheme.typography.labelSmall,
                        color = folderColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentListCard(
    doc: DocumentEntity,
    folderColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colors = AsifTheme.colors
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        selected = selected,
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(folderColor),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${doc.category} • ${doc.pageCount} page(s) • ${formatShortDate(doc.updatedAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                )
            }
            StatusPillBadge(text = doc.docType, color = docTypeColor(doc.docType))
        }
    }
}

private fun docTypeColor(docType: String): Color = when (docType.uppercase()) {
    "PDF" -> ElectricBlue
    "XLSX" -> FolderGreen
    "DOCX" -> FolderPurple
    else -> FolderOrange
}

private fun formatShortDate(epochMs: Long): String =
    SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(epochMs))
