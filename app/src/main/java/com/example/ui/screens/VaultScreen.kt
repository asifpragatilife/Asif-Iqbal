package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.security.BiometricAuthHelper
import com.example.ui.components.GlassCard
import com.example.ui.components.StatusPillBadge
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.AsifTheme
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.FolderGreen
import com.example.ui.theme.FolderOrange
import com.example.ui.theme.JetBrainsMono
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VaultScreen(
    viewModel: MainViewModel,
    onOpenDocument: (String) -> Unit,
) {
    val colors = AsifTheme.colors
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    val unlocked by viewModel.vaultUnlocked.collectAsStateWithLifecycle()
    val vaultError by viewModel.vaultError.collectAsStateWithLifecycle()
    val prefs by viewModel.preferencesState.collectAsStateWithLifecycle()
    val vaultDocs by viewModel.vaultDocuments.collectAsStateWithLifecycle()
    val libraryDocs by viewModel.libraryDocuments.collectAsStateWithLifecycle()
    val activities by viewModel.vaultActivities.collectAsStateWithLifecycle()

    var showPinModal by remember { mutableStateOf(false) }
    var pinBuffer by remember { mutableStateOf("") }
    var subTab by remember { mutableIntStateOf(0) } // 0=Vault, 1=Upload, 2=Activity, 3=Profile
    var showCloudConsent by remember { mutableStateOf(false) }

    if (!unlocked) {
        // LOCKED STATE — Image 3 style: glowing fingerprint inside shield with cyan neon halo
        val pulse = rememberInfiniteTransition(label = "vault_pulse")
        val haloScale by pulse.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.07f,
            animationSpec = infiniteRepeatable(
                animation = tween(1700, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "halo_scale",
        )

        val triggerUnlock: () -> Unit = {
            val activity = context as? FragmentActivity
            if (prefs.biometricEnabled && activity != null && BiometricAuthHelper.canAuthenticate(context)) {
                BiometricAuthHelper.authenticate(
                    activity = activity,
                    title = "Unlock Obsidian Vault",
                    subtitle = "Decrypt AES-256-GCM vault documents locally",
                    negativeButtonText = "Use PIN",
                    onSuccess = { viewModel.unlockVaultViaBiometric() },
                    onFallbackPin = { showPinModal = true },
                    onError = { viewModel.showMessage(it); showPinModal = true },
                )
            } else {
                showPinModal = true
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp)
                .padding(bottom = 110.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            StatusPillBadge(
                text = if (prefs.secureCloudOptIn) "SECURE CLOUD | LOCKED" else strings.vaultLockedStatus,
                color = CyanGlow,
                icon = Icons.Outlined.Lock,
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Glowing Fingerprint Shield
            Box(
                modifier = Modifier
                    .size(188.dp)
                    .scale(haloScale)
                    .shadow(
                        elevation = 32.dp,
                        shape = CircleShape,
                        ambientColor = CyanGlow,
                        spotColor = CyanGlow,
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                CyanGlow.copy(alpha = 0.25f),
                                ElectricBlue.copy(alpha = 0.14f),
                                Color.Transparent,
                            ),
                        ),
                    )
                    .border(2.dp, Brush.linearGradient(listOf(CyanGlow, ElectricBlue)), CircleShape)
                    .clickable(onClick = triggerUnlock)
                    .testTag("vault_unlock_shield"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = ElectricBlue.copy(alpha = 0.45f),
                    modifier = Modifier.size(124.dp),
                )
                Icon(
                    imageVector = Icons.Outlined.Fingerprint,
                    contentDescription = strings.tapToUnlock,
                    tint = CyanGlow,
                    modifier = Modifier.size(72.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = strings.tapToUnlock,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMono,
                    letterSpacing = 1.6.sp,
                ),
                color = CyanGlow,
                modifier = Modifier
                    .clickable(onClick = triggerUnlock)
                    .padding(8.dp),
            )
            Text(
                text = "AES-256-GCM • ${strings.storedOnDevice}",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Preview of encrypted file headers (zero plaintext exposed)
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(vaultDocs, key = { it.id }) { doc ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 18.dp,
                        onClick = triggerUnlock,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = CyanGlow)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(doc.title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                Text("Locked • ${doc.pageCount} page(s)", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
                            }
                            StatusPillBadge(text = strings.encryptedBadge, color = FolderGreen)
                        }
                    }
                }
            }
        }
    } else {
        // UNLOCKED STATE — Tabs: Vault, Upload, Activity, Profile
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPillBadge(
                    text = strings.vaultUnlockedStatus,
                    color = FolderGreen,
                    icon = Icons.Outlined.LockOpen,
                )
                OutlinedButton(
                    onClick = { viewModel.lockVault("Manual Lock") },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("vault_lock_now_btn"),
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Lock")
                }
            }

            TabRow(
                selectedTabIndex = subTab,
                containerColor = Color.Transparent,
                contentColor = CyanGlow,
            ) {
                Tab(selected = subTab == 0, onClick = { subTab = 0 }, text = { Text(strings.vaultTabVault) })
                Tab(selected = subTab == 1, onClick = { subTab = 1 }, text = { Text(strings.vaultTabUpload) })
                Tab(selected = subTab == 2, onClick = { subTab = 2 }, text = { Text(strings.vaultTabActivity) })
                Tab(selected = subTab == 3, onClick = { subTab = 3 }, text = { Text(strings.vaultTabProfile) })
            }

            when (subTab) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (vaultDocs.isEmpty()) {
                            item {
                                Text("No documents locked in Vault. Use the Upload tab to seal files.", color = colors.textMuted)
                            }
                        } else {
                            items(vaultDocs, key = { it.id }) { doc ->
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    cornerRadius = 18.dp,
                                    featuredGlow = true,
                                    onClick = { onOpenDocument(doc.id) },
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = CyanGlow)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(doc.title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                            Text("${doc.category} • ${doc.pageCount}p • Tap to decrypt in memory", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
                                        }
                                        TextButton(onClick = { viewModel.unlockDocumentFromVault(doc.id) }) {
                                            Text("Unseal", color = FolderOrange)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            Text(
                                "Move Library documents into AES-256-GCM encrypted Vault storage (zero plaintext kept on disk):",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                            )
                        }
                        items(libraryDocs, key = { it.id }) { doc ->
                            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Outlined.UploadFile, contentDescription = null, tint = ElectricBlue)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(doc.title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                        Text("${doc.docType} • ${doc.pageCount}p", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
                                    }
                                    Button(
                                        onClick = { viewModel.lockDocumentsToVault(listOf(doc.id)) },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                        shape = RoundedCornerShape(12.dp),
                                    ) {
                                        Text("Encrypt", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(activities, key = { it.id }) { act ->
                            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (act.isSuccess) FolderGreen else colors.danger),
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(act.action, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                        Text(act.detail, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
                                    }
                                    Text(
                                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(act.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textMuted,
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    if (prefs.secureCloudOptIn) Icons.Outlined.CloudSync else Icons.Outlined.CloudOff,
                                    contentDescription = null,
                                    tint = if (prefs.secureCloudOptIn) CyanGlow else FolderGreen,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Secure Cloud (Opt-In)", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                    Text(
                                        if (prefs.secureCloudOptIn && prefs.lastSyncedMs > 0L) {
                                            "Last synced: ${SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(prefs.lastSyncedMs))}"
                                        } else {
                                            "Disabled • Stored on this device only"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.textMuted,
                                    )
                                }
                                Switch(
                                    checked = prefs.secureCloudOptIn,
                                    onCheckedChange = { enable ->
                                        if (enable) showCloudConsent = true else viewModel.setSecureCloudOptIn(false)
                                    },
                                )
                            }
                        }

                        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Cryptographic Specification", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                Text("• Cipher: AES-256-GCM (128-bit auth tag)", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                                Text("• PIN KDF: PBKDF2WithHmacSHA256 (100k rounds)", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                                Text("• Key Storage: AndroidKeyStore Hardware Key", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                                Text("• Auto-Lock Timeout: ${if (prefs.autoLockSeconds == 0) "Manual" else "${prefs.autoLockSeconds}s"}", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }

    // PIN Keypad Modal
    if (showPinModal) {
        AlertDialog(
            onDismissRequest = { showPinModal = false; pinBuffer = "" },
            containerColor = colors.glassSurfaceElevated,
            title = { Text(strings.enterPin) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Default PIN: 1234 (changeable in Settings)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted,
                    )
                    // Dots
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(4) { idx ->
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(if (idx < pinBuffer.length) CyanGlow else colors.glassBorder),
                            )
                        }
                    }
                    vaultError?.let {
                        Text(it, color = colors.danger, style = MaterialTheme.typography.bodyMedium)
                    }
                    // Numeric Keypad
                    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("C", "0", "⌫"))
                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { key ->
                                Surface(
                                    shape = CircleShape,
                                    color = colors.glassSurface,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clickable {
                                            when (key) {
                                                "C" -> pinBuffer = ""
                                                "⌫" -> if (pinBuffer.isNotEmpty()) pinBuffer = pinBuffer.dropLast(1)
                                                else -> if (pinBuffer.length < 4) {
                                                    pinBuffer += key
                                                    if (pinBuffer.length == 4) {
                                                        val ok = viewModel.attemptPinUnlock(pinBuffer)
                                                        pinBuffer = ""
                                                        if (ok) showPinModal = false
                                                    }
                                                }
                                            }
                                        }
                                        .testTag("pin_key_$key"),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (key == "⌫") {
                                            Icon(Icons.Outlined.Backspace, contentDescription = "Backspace", tint = colors.textPrimary)
                                        } else {
                                            Text(key, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPinModal = false; pinBuffer = "" }) { Text("Cancel") }
            },
        )
    }

    if (showCloudConsent) {
        AlertDialog(
            onDismissRequest = { showCloudConsent = false },
            containerColor = colors.glassSurfaceElevated,
            title = { Text("Enable Secure Cloud Opt-In?") },
            text = {
                Text(
                    "AsifAraf is offline-first by default. Turning on Secure Cloud only prepares client-side encrypted backup metadata. Zero plaintext or unencrypted files ever leave your device.",
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.setSecureCloudOptIn(true)
                    showCloudConsent = false
                }) { Text("Opt In") }
            },
            dismissButton = {
                TextButton(onClick = { showCloudConsent = false }) { Text("Stay Offline") }
            },
        )
    }
}
