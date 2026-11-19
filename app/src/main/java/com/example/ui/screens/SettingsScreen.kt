package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.AppLanguage
import com.example.data.preferences.ScanQuality
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowingBrandTitle
import com.example.ui.components.StatusPillBadge
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.AsifTheme
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.FolderGreen
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
) {
    val colors = AsifTheme.colors
    val strings = LocalAppStrings.current
    val prefs by viewModel.preferencesState.collectAsStateWithLifecycle()

    val storageBreakdown by produceState(initialValue = Triple(0L, 0L, 0L)) {
        value = viewModel.repo.computeStorageBreakdownBytes()
    }

    var showChangePinDialog by remember { mutableStateOf(false) }
    var showPrivacyPromise by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                GlowingBrandTitle(strings.settingsTitle)
                Text(strings.tagline, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
            }
        }

        // Theme Toggle
        item {
            SettingsToggleCard(
                icon = Icons.Outlined.DarkMode,
                title = strings.darkTheme,
                subtitle = if (prefs.isDarkTheme) "Obsidian Navy active" else "Crisp Light active",
                checked = prefs.isDarkTheme,
                onCheckedChange = { viewModel.setDarkTheme(it) },
                tag = "setting_theme_toggle",
            )
        }

        // Language English + Bangla
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Language, contentDescription = null, tint = CyanGlow)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(strings.languageLabel, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = prefs.language == lang,
                                onClick = { viewModel.setLanguage(lang) },
                                label = { Text(lang.displayName) },
                                modifier = Modifier.testTag("lang_${lang.code}"),
                            )
                        }
                    }
                }
            }
        }

        // Default Scan Quality
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Tune, contentDescription = null, tint = ElectricBlue)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(strings.scanQuality, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScanQuality.entries.forEach { q ->
                            FilterChip(
                                selected = prefs.defaultScanQuality == q,
                                onClick = { viewModel.setScanQuality(q) },
                                label = { Text(q.label) },
                            )
                        }
                    }
                }
            }
        }

        // Auto-Lock Timer
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LockClock, contentDescription = null, tint = CyanGlow)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(strings.autoLockTimer, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(30 to "30s", 60 to "1m", 300 to "5m", 0 to "Manual").forEach { (sec, label) ->
                            FilterChip(
                                selected = prefs.autoLockSeconds == sec,
                                onClick = { viewModel.setAutoLockSeconds(sec) },
                                label = { Text(label) },
                            )
                        }
                    }
                }
            }
        }

        // Biometric Toggle & Change PIN
        item {
            SettingsToggleCard(
                icon = Icons.Outlined.Fingerprint,
                title = strings.biometricUnlock,
                subtitle = "Use hardware fingerprint / face unlock for Vault",
                checked = prefs.biometricEnabled,
                onCheckedChange = { viewModel.setBiometricEnabled(it) },
                tag = "setting_biometric_toggle",
            )
        }

        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth().testTag("setting_change_pin"),
                cornerRadius = 18.dp,
                onClick = { showChangePinDialog = true },
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Pin, contentDescription = null, tint = CyanGlow)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.changePin, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        Text("PBKDF2WithHmacSHA256 • 100,000 rounds", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
                    }
                }
            }
        }

        // Storage Usage & Clear Data
        item {
            val (libB, vaultB, cacheB) = storageBreakdown
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Storage, contentDescription = null, tint = FolderGreen)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(strings.storageUsage, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    }
                    Text(
                        "Library: ${formatBytes(libB)} • Encrypted Vault: ${formatBytes(vaultB)} • Cache: ${formatBytes(cacheB)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { viewModel.clearCache() }, modifier = Modifier.weight(1f)) {
                            Text("Clear Cache")
                        }
                        OutlinedButton(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.DeleteForever, contentDescription = null, tint = colors.danger)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.clearData, color = colors.danger)
                        }
                    }
                }
            }
        }

        // Privacy Promise & About
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth().testTag("setting_privacy_promise"),
                cornerRadius = 18.dp,
                featuredGlow = true,
                onClick = { showPrivacyPromise = true },
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.PrivacyTip, contentDescription = null, tint = FolderGreen)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.privacyPromise, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        Text("Zero tracking • Zero telemetry • 100% offline", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    }
                    StatusPillBadge(text = "VERIFIED", color = FolderGreen)
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = ElectricBlue)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("${strings.aboutApp} v1.0", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        Text("Offline CameraX + ML Kit OCR/QR + AndroidKeyStore AES-256-GCM", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
                    }
                }
            }
        }
    }

    if (showChangePinDialog) {
        var newPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            containerColor = colors.glassSurfaceElevated,
            title = { Text("Change Vault PIN") },
            text = {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6 && it.all { ch -> ch.isDigit() }) newPin = it },
                    label = { Text("New 4–6 Digit PIN") },
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(
                    enabled = newPin.length >= 4,
                    onClick = {
                        viewModel.changeVaultPin(newPin)
                        showChangePinDialog = false
                    },
                ) { Text("Save PIN") }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showPrivacyPromise) {
        AlertDialog(
            onDismissRequest = { showPrivacyPromise = false },
            containerColor = colors.glassSurfaceElevated,
            title = { Text("AsifAraf Privacy Promise") },
            text = {
                Text(
                    "1. Zero Analytics & Zero Trackers: No advertising SDKs or telemetry.\n\n" +
                        "2. 100% On-Device AI: Edge detection, perspective warp, ML Kit OCR, and QR parsing execute strictly offline.\n\n" +
                        "3. Hardware-Backed Obsidian Vault: Sensitive documents are sealed with AES-256-GCM and AndroidKeyStore.\n\n" +
                        "4. Opt-In Only Cloud: Cloud options remain completely off unless explicitly enabled by you.",
                )
            },
            confirmButton = {
                Button(onClick = { showPrivacyPromise = false }) { Text("Close") }
            },
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = colors.glassSurfaceElevated,
            title = { Text("Erase All Local Documents?") },
            text = { Text("This permanently deletes all scans, OCR text, signatures, and encrypted Vault items from this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger),
                ) { Text("Erase Everything", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SettingsToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String,
) {
    val colors = AsifTheme.colors
    GlassCard(modifier = Modifier.fillMaxWidth().testTag(tag), cornerRadius = 18.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = CyanGlow)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "${bytes}B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${"%.1f".format(kb)}KB"
    return "${"%.1f".format(kb / 1024.0)}MB"
}
