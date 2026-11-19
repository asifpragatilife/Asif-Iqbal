package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.security.BiometricAuthHelper
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowingBrandTitle
import com.example.ui.components.ObsidianBackground
import com.example.ui.components.StatusPillBadge
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.AsifTheme
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.FolderGreen
import kotlinx.coroutines.launch

private data class OnboardingSlide(
    val imageRes: Int,
    val badge: String,
    val title: String,
    val body: String,
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val colors = AsifTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showPermissions by remember { mutableStateOf(false) }
    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        cameraGranted = granted
    }

    val slides = remember(strings) {
        listOf(
            OnboardingSlide(
                imageRes = R.drawable.img_onboarding_privacy,
                badge = "ZERO TELEMETRY",
                title = strings.onboardingTitle1,
                body = strings.onboardingBody1,
            ),
            OnboardingSlide(
                imageRes = R.drawable.img_onboarding_offline,
                badge = "ON-DEVICE VISION AI",
                title = strings.onboardingTitle2,
                body = strings.onboardingBody2,
            ),
            OnboardingSlide(
                imageRes = R.drawable.img_onboarding_vault,
                badge = "AES-256-GCM",
                title = strings.onboardingTitle3,
                body = strings.onboardingBody3,
            ),
        )
    }

    ObsidianBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = showPermissions,
                label = "onboarding_phase",
                modifier = Modifier.widthIn(max = 520.dp).fillMaxSize(),
            ) { isPermissionStep ->
                if (!isPermissionStep) {
                    val pagerState = rememberPagerState(pageCount = { slides.size })
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            GlowingBrandTitle(strings.appName)
                            TextButton(
                                onClick = { showPermissions = true },
                                modifier = Modifier.testTag("onboarding_skip"),
                            ) {
                                Text(strings.skipBtn, color = colors.textMuted)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.weight(1f),
                        ) { page ->
                            val slide = slides[page]
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(270.dp),
                                    cornerRadius = 24.dp,
                                    featuredGlow = page == 2,
                                ) {
                                    Image(
                                        painter = painterResource(id = slide.imageRes),
                                        contentDescription = slide.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                StatusPillBadge(
                                    text = slide.badge,
                                    color = CyanGlow,
                                    icon = Icons.Outlined.Lock,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = slide.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = colors.textPrimary,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = slide.body,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colors.textSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                )
                            }
                        }

                        // Pager indicator dots
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            repeat(slides.size) { index ->
                                val active = pagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .height(8.dp)
                                        .width(if (active) 28.dp else 8.dp)
                                        .clip(CircleShape)
                                        .background(if (active) CyanGlow else colors.textMuted.copy(alpha = 0.35f)),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (pagerState.currentPage < slides.lastIndex) {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                } else {
                                    showPermissions = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("onboarding_next_btn"),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        ) {
                            Text(
                                text = strings.continueBtn,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                            )
                        }
                    }
                } else {
                    val biometricAvailable = remember { BiometricAuthHelper.canAuthenticate(context) }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            GlowingBrandTitle(strings.appName)
                            Text(
                                text = strings.permTitle,
                                style = MaterialTheme.typography.headlineMedium,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = strings.permSubtitle,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.textSecondary,
                            )

                            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(ElectricBlue.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Outlined.CameraAlt,
                                            contentDescription = null,
                                            tint = CyanGlow,
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Camera Access",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = colors.textPrimary,
                                        )
                                        Text(
                                            "Used exclusively for live document edge capture & QR scanning on-device.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colors.textMuted,
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (cameraGranted) {
                                        Icon(
                                            Icons.Outlined.CheckCircle,
                                            contentDescription = "Granted",
                                            tint = FolderGreen,
                                        )
                                    } else {
                                        OutlinedButton(
                                            onClick = { cameraLauncher.launch(Manifest.permission.CAMERA) },
                                            shape = RoundedCornerShape(50),
                                        ) {
                                            Text("Allow")
                                        }
                                    }
                                }
                            }

                            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(CyanGlow.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Outlined.Fingerprint,
                                            contentDescription = null,
                                            tint = CyanGlow,
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Biometrics & PIN Vault",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = colors.textPrimary,
                                        )
                                        Text(
                                            if (biometricAvailable) {
                                                "Hardware fingerprint/face unlock protects your AES-256 Vault. Default fallback PIN: 1234."
                                            } else {
                                                "PIN fallback (1234) protects your AES-256 Vault. You can customize your PIN in Settings."
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colors.textMuted,
                                        )
                                    }
                                }
                            }

                            Text(
                                text = strings.tagline,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = CyanGlow,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onFinished,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("onboarding_enter_btn"),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        ) {
                            Text(strings.getStartedBtn, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
