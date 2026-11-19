package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.preferences.UserPreferencesState
import com.example.pdf.ExportFormat
import com.example.ui.theme.AsifTheme
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.FolderGreen
import com.example.ui.theme.FolderOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportAndCloudSheet(
    documentTitle: String,
    prefs: UserPreferencesState,
    onDismiss: () -> Unit,
    onExportFormat: (ExportFormat) -> Unit,
    onToggleGoogleDrive: (Boolean) -> Unit,
    onToggleDropbox: (Boolean) -> Unit,
) {
    val colors = AsifTheme.colors
    var pendingCloudProvider by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.glassSurfaceElevated,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Export & Share",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = documentTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted,
                    )
                }
                StatusPillBadge(
                    text = "Local Generation",
                    color = FolderGreen,
                    icon = Icons.Outlined.CloudOff,
                )
            }

            val formats = listOf(
                Triple(ExportFormat.PDF, Icons.Outlined.PictureAsPdf, ElectricBlue),
                Triple(ExportFormat.JPG, Icons.Outlined.Image, CyanGlow),
                Triple(ExportFormat.TXT, Icons.Outlined.TextSnippet, colors.textSecondary),
                Triple(ExportFormat.DOCX, Icons.Outlined.Description, ElectricBlue),
                Triple(ExportFormat.XLSX, Icons.Outlined.TableChart, FolderGreen),
            )

            formats.forEach { (format, icon, tint) ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_${format.extension}"),
                    cornerRadius = 16.dp,
                    onClick = { onExportFormat(format) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = format.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = "Generated offline on this device • .${format.extension}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textMuted,
                            )
                        }
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = "Share ${format.label}",
                            tint = colors.textMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "OPTIONAL CLOUD CONNECTORS (OPT-IN ONLY)",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted,
            )

            CloudConnectorRow(
                name = "Google Drive",
                enabled = prefs.googleDriveOptIn,
                onToggleRequest = { wantEnable ->
                    if (wantEnable) pendingCloudProvider = "Google Drive" else onToggleGoogleDrive(false)
                },
            )
            CloudConnectorRow(
                name = "Dropbox",
                enabled = prefs.dropboxOptIn,
                onToggleRequest = { wantEnable ->
                    if (wantEnable) pendingCloudProvider = "Dropbox" else onToggleDropbox(false)
                },
            )
        }
    }

    pendingCloudProvider?.let { provider ->
        AlertDialog(
            onDismissRequest = { pendingCloudProvider = null },
            containerColor = colors.glassSurfaceElevated,
            title = { Text("Enable $provider Opt-In?") },
            text = {
                Text(
                    "AsifAraf never uploads documents automatically. Enabling $provider marks it available inside your local Share Sheet when you explicitly choose to export.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (provider == "Google Drive") onToggleGoogleDrive(true) else onToggleDropbox(true)
                        pendingCloudProvider = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                ) {
                    Text("I Understand, Enable")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCloudProvider = null }) {
                    Text("Keep Offline")
                }
            },
        )
    }
}

@Composable
private fun CloudConnectorRow(
    name: String,
    enabled: Boolean,
    onToggleRequest: (Boolean) -> Unit,
) {
    val colors = AsifTheme.colors
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text(
                    text = if (enabled) "Opt-in export target ready" else "Off by default • Local-only",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) FolderOrange else colors.textMuted,
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggleRequest)
        }
    }
}
