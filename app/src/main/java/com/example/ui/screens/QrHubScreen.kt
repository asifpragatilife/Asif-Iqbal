package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pdf.DocumentExportEngine
import com.example.pdf.ExportFormat
import com.example.ui.components.GlassCard
import com.example.ui.components.ObsidianBackground
import com.example.ui.components.StatusPillBadge
import com.example.ui.theme.AsifTheme
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.FolderGreen
import com.example.ui.viewmodel.MainViewModel
import com.example.vision.ParsedQrResult
import com.example.vision.QrEngine
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
fun QrHubScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val colors = AsifTheme.colors
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val history by viewModel.qrHistory.collectAsStateWithLifecycle()
    var activeTab by remember { mutableStateOf(0) } // 0 = Scan, 1 = Generate, 2 = History
    var detectedQr by remember { mutableStateOf<ParsedQrResult?>(null) }

    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasCameraPerm = it
    }

    val imageQrPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                if (bmp != null) {
                    val results = QrEngine.scanBitmap(bmp)
                    if (results.isNotEmpty()) {
                        val first = results.first()
                        detectedQr = first
                        viewModel.saveQr(first.rawContent, first.qrType, first.title, false)
                    } else {
                        viewModel.showMessage("No QR or barcode found in image")
                    }
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("qr_back")) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                }
                Text("QR & Barcode Hub", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            }

            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.Transparent,
                contentColor = CyanGlow,
            ) {
                Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Scan") })
                Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Generate") })
                Tab(selected = activeTab == 2, onClick = { activeTab = 2 }, text = { Text("History (${history.size})") })
            }

            when (activeTab) {
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF0A1220)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (hasCameraPerm) {
                                AndroidView(
                                    factory = { ctx ->
                                        val previewView = PreviewView(ctx)
                                        val providerFuture = ProcessCameraProvider.getInstance(ctx)
                                        val executor = Executors.newSingleThreadExecutor()
                                        val scanner = BarcodeScanning.getClient()
                                        providerFuture.addListener(
                                            {
                                                val provider = providerFuture.get()
                                                val preview = Preview.Builder().build().also {
                                                    it.surfaceProvider = previewView.surfaceProvider
                                                }
                                                val analysis = ImageAnalysis.Builder()
                                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                                    .build()
                                                analysis.setAnalyzer(executor) { imageProxy ->
                                                    val mediaImg = imageProxy.image
                                                    if (mediaImg != null) {
                                                        val input = InputImage.fromMediaImage(
                                                            mediaImg,
                                                            imageProxy.imageInfo.rotationDegrees,
                                                        )
                                                        scanner.process(input)
                                                            .addOnSuccessListener { codes ->
                                                                val raw = codes.firstOrNull()?.rawValue
                                                                if (raw != null && detectedQr?.rawContent != raw) {
                                                                    val parsed = QrEngine.classifyPayload(
                                                                        raw,
                                                                        codes.first().valueType,
                                                                    )
                                                                    detectedQr = parsed
                                                                    viewModel.saveQr(
                                                                        parsed.rawContent,
                                                                        parsed.qrType,
                                                                        parsed.title,
                                                                        false,
                                                                    )
                                                                }
                                                            }
                                                            .addOnCompleteListener { imageProxy.close() }
                                                    } else {
                                                        imageProxy.close()
                                                    }
                                                }
                                                provider.unbindAll()
                                                provider.bindToLifecycle(
                                                    lifecycleOwner,
                                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                                    preview,
                                                    analysis,
                                                )
                                            },
                                            ContextCompat.getMainExecutor(ctx),
                                        )
                                        previewView
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp),
                                ) {
                                    Icon(Icons.Outlined.QrCode2, contentDescription = null, tint = CyanGlow, modifier = Modifier.size(56.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Enable camera for live QR scan, or pick a photo.", color = colors.textSecondary)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(onClick = { permLauncher.launch(Manifest.permission.CAMERA) }) {
                                        Text("Enable Camera")
                                    }
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                imageQrPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan QR from Photo")
                        }

                        detectedQr?.let { qr ->
                            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp, featuredGlow = true) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(qr.title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                        StatusPillBadge(text = qr.qrType, color = CyanGlow)
                                    }
                                    Text(qr.secondaryDetail, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = {
                                                if (qr.qrType == "URL") {
                                                    runCatching {
                                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(qr.rawContent)))
                                                    }
                                                } else {
                                                    clipboard.setText(AnnotatedString(qr.rawContent))
                                                    viewModel.showMessage("Copied to clipboard")
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(qr.primaryActionLabel, color = Color.White)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                clipboard.setText(AnnotatedString(qr.rawContent))
                                                viewModel.showMessage("Copied")
                                            },
                                        ) {
                                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // QR GENERATOR
                    var genMode by remember { mutableStateOf("URL") }
                    var inputText by remember { mutableStateOf("https://asifaraf.local") }
                    var wifiSsid by remember { mutableStateOf("ObsidianStudio") }
                    var wifiPass by remember { mutableStateOf("") }
                    var generatedBmp by remember { mutableStateOf<Bitmap?>(QrEngine.generateQrBitmap("https://asifaraf.local")) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("URL", "TEXT", "WIFI", "EMAIL").forEach { mode ->
                                FilterChip(
                                    selected = genMode == mode,
                                    onClick = { genMode = mode },
                                    label = { Text(mode) },
                                )
                            }
                        }

                        if (genMode == "WIFI") {
                            OutlinedTextField(
                                value = wifiSsid,
                                onValueChange = { wifiSsid = it },
                                label = { Text("Wi-Fi SSID") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = wifiPass,
                                onValueChange = { wifiPass = it },
                                label = { Text("Wi-Fi Password") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                label = { Text("Content ($genMode)") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        Button(
                            onClick = {
                                val payload = when (genMode) {
                                    "WIFI" -> "WIFI:T:WPA;S:$wifiSsid;P:$wifiPass;;"
                                    "EMAIL" -> if (inputText.startsWith("mailto:", true)) inputText else "mailto:$inputText"
                                    else -> inputText
                                }
                                generatedBmp = QrEngine.generateQrBitmap(payload)
                                if (generatedBmp != null) {
                                    val parsed = QrEngine.classifyPayload(payload)
                                    viewModel.saveQr(payload, parsed.qrType, parsed.title, true)
                                    viewModel.showMessage("QR Code generated offline")
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("qr_generate_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text("Generate QR Code", color = Color.White)
                        }

                        generatedBmp?.let { bmp ->
                            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp, featuredGlow = true) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Generated QR code",
                                        modifier = Modifier
                                            .size(220.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color.White)
                                            .padding(8.dp),
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            val file = DocumentExportEngine.exportJpg(context, bmp, "AsifAraf_QR", 98)
                                            DocumentExportEngine.shareFile(context, file, ExportFormat.JPG.mimeType, "QR Code")
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                    ) {
                                        Icon(Icons.Outlined.Share, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save & Share QR Image")
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    // QR HISTORY
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (history.isEmpty()) {
                            item {
                                Text("No QR history yet.", color = colors.textMuted)
                            }
                        } else {
                            items(history, key = { it.id }) { item ->
                                val icon = when (item.qrType) {
                                    "URL" -> Icons.Outlined.Link
                                    "WIFI" -> Icons.Outlined.Wifi
                                    "EMAIL" -> Icons.Outlined.Email
                                    "CONTACT" -> Icons.Outlined.Person
                                    else -> Icons.Outlined.QrCode2
                                }
                                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(icon, contentDescription = null, tint = CyanGlow)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text(item.summary, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                                StatusPillBadge(
                                                    text = if (item.isGenerated) "GEN" else "SCAN",
                                                    color = if (item.isGenerated) FolderGreen else ElectricBlue,
                                                )
                                            }
                                            Text(item.rawContent, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
                                        }
                                        IconButton(onClick = {
                                            clipboard.setText(AnnotatedString(item.rawContent))
                                            viewModel.showMessage("Copied")
                                        }) {
                                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = colors.textSecondary)
                                        }
                                        IconButton(onClick = { viewModel.deleteQr(item.id) }) {
                                            Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = colors.danger)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
