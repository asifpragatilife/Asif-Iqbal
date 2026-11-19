package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.AsifTheme
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.PlusJakartaSans

@Composable
fun ObsidianBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = AsifTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(colors.gradientTop, colors.gradientBottom),
                ),
            )
            .drawBehind {
                // Ambient radial glow in top-left and bottom-right
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ElectricBlue.copy(alpha = if (colors.isDark) 0.14f else 0.06f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.2f, size.height * 0.08f),
                        radius = size.width * 0.75f,
                    ),
                    radius = size.width * 0.75f,
                    center = Offset(size.width * 0.2f, size.height * 0.08f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CyanGlow.copy(alpha = if (colors.isDark) 0.09f else 0.04f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.85f),
                        radius = size.width * 0.6f,
                    ),
                    radius = size.width * 0.6f,
                    center = Offset(size.width * 0.85f, size.height * 0.85f),
                )
            },
        content = content,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    featuredGlow: Boolean = false,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = AsifTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.975f else 1f,
        animationSpec = spring(stiffness = 500f),
        label = "glass_press",
    )

    val borderBrush = when {
        selected -> Brush.linearGradient(listOf(CyanGlow, ElectricBlue))
        featuredGlow -> Brush.linearGradient(
            listOf(ElectricBlue.copy(alpha = 0.95f), CyanGlow.copy(alpha = 0.85f)),
        )
        else -> Brush.linearGradient(
            listOf(colors.glassBorder, colors.glassBorder.copy(alpha = 0.5f)),
        )
    }

    val shape = RoundedCornerShape(cornerRadius)
    val clickableModifier = when {
        onClick != null && onLongClick != null -> Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
            onLongClick = onLongClick,
        )
        onClick != null -> Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            role = Role.Button,
            onClick = onClick,
        )
        else -> Modifier
    }

    Box(
        modifier = modifier
            .scale(scale)
            .then(
                if (featuredGlow || selected) {
                    Modifier.shadow(
                        elevation = 16.dp,
                        shape = shape,
                        ambientColor = ElectricBlue.copy(alpha = 0.45f),
                        spotColor = CyanGlow.copy(alpha = 0.45f),
                    )
                } else Modifier,
            )
            .clip(shape)
            .background(if (featuredGlow) colors.glassSurfaceElevated else colors.glassSurface)
            .border(
                width = if (featuredGlow || selected) 1.5.dp else 1.dp,
                brush = borderBrush,
                shape = shape,
            )
            .then(clickableModifier),
        content = content,
    )
}

@Composable
fun GlowingBrandTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        modifier = modifier,
        style = TextStyle(
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            letterSpacing = (-0.5).sp,
            brush = Brush.horizontalGradient(listOf(ElectricBlue, CyanGlow)),
        ),
    )
}

@Composable
fun StatusPillBadge(
    text: String,
    color: Color = CyanGlow,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
            }
            Text(
                text = text.uppercase(),
                style = TextStyle(
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                    color = color,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

enum class MainTab {
    HOME, TOOLS, VAULT, SETTINGS
}

@Composable
fun AsifBottomBar(
    currentTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AsifTheme.colors
    val strings = LocalAppStrings.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp),
            shape = RoundedCornerShape(26.dp),
            color = colors.navBarBackground,
            border = BorderStroke(1.dp, colors.glassBorder),
            shadowElevation = 12.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                BottomNavItem(
                    label = strings.navHome,
                    selected = currentTab == MainTab.HOME,
                    selectedIcon = Icons.Filled.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    tag = "nav_home",
                    onClick = { onSelectTab(MainTab.HOME) },
                    modifier = Modifier.weight(1f),
                )
                BottomNavItem(
                    label = strings.navTools,
                    selected = currentTab == MainTab.TOOLS,
                    selectedIcon = Icons.Filled.Build,
                    unselectedIcon = Icons.Outlined.Build,
                    tag = "nav_tools",
                    onClick = { onSelectTab(MainTab.TOOLS) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(68.dp))
                BottomNavItem(
                    label = strings.navVault,
                    selected = currentTab == MainTab.VAULT,
                    selectedIcon = Icons.Filled.Lock,
                    unselectedIcon = Icons.Outlined.Lock,
                    tag = "nav_vault",
                    onClick = { onSelectTab(MainTab.VAULT) },
                    modifier = Modifier.weight(1f),
                )
                BottomNavItem(
                    label = strings.navSettings,
                    selected = currentTab == MainTab.SETTINGS,
                    selectedIcon = Icons.Filled.Settings,
                    unselectedIcon = Icons.Outlined.Settings,
                    tag = "nav_settings",
                    onClick = { onSelectTab(MainTab.SETTINGS) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Center Floating Glowing "+" Scanner Button
        Box(
            modifier = Modifier
                .offset(y = (-14).dp)
                .size(62.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = CircleShape,
                    ambientColor = ElectricBlue,
                    spotColor = CyanGlow,
                )
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(ElectricBlue, CyanGlow)))
                .border(2.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                .clickable(role = Role.Button, onClick = onScanClick)
                .testTag("nav_scan_fab"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = strings.navScan,
                tint = Color.White,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AsifTheme.colors
    val tint = if (selected) CyanGlow else colors.textMuted

    Column(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Tab, onClick = onClick)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
