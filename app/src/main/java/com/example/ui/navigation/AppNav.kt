package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ui.components.AsifBottomBar
import com.example.ui.components.ExportAndCloudSheet
import com.example.ui.components.MainTab
import com.example.ui.components.ObsidianBackground
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.screens.DocumentDetailScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.QrHubScreen
import com.example.ui.screens.RedactionScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SignatureScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.AsifTheme
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricBlue
import com.example.ui.viewmodel.MainViewModel
import kotlinx.serialization.Serializable

@Serializable object RouteOnboarding
@Serializable object RouteHome
@Serializable object RouteTools
@Serializable object RouteVault
@Serializable object RouteSettings
@Serializable object RouteScanner
@Serializable data class RouteDetail(val docId: String)
@Serializable data class RouteSignature(val docId: String? = null)
@Serializable data class RouteRedact(val docId: String? = null)
@Serializable object RouteQrHub

@Composable
fun AsifArafNavHost(viewModel: MainViewModel) {
    val prefs by viewModel.preferencesState.collectAsStateWithLifecycle()
    val libraryDocs by viewModel.libraryDocuments.collectAsStateWithLifecycle()
    val allDocs by viewModel.allDocuments.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    var exportDocId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    val startDest: Any = if (prefs.onboardingCompleted) RouteHome else RouteOnboarding

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRouteName = backStackEntry?.destination?.route.orEmpty()

    val currentTab = when {
        currentRouteName.contains("RouteTools") -> MainTab.TOOLS
        currentRouteName.contains("RouteVault") -> MainTab.VAULT
        currentRouteName.contains("RouteSettings") -> MainTab.SETTINGS
        currentRouteName.contains("RouteHome") -> MainTab.HOME
        else -> null
    }

    val showMainChrome = currentTab != null

    val onSelectTab: (MainTab) -> Unit = { tab ->
        val target: Any = when (tab) {
            MainTab.HOME -> RouteHome
            MainTab.TOOLS -> RouteTools
            MainTab.VAULT -> RouteVault
            MainTab.SETTINGS -> RouteSettings
        }
        navController.navigate(target) {
            popUpTo(RouteHome) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { _ ->
        ObsidianBackground {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWide = maxWidth >= 680.dp

                Row(modifier = Modifier.fillMaxSize()) {
                    if (isWide && showMainChrome) {
                        val strings = LocalAppStrings.current
                        NavigationRail(
                            containerColor = AsifTheme.colors.navBarBackground,
                            contentColor = CyanGlow,
                            modifier = Modifier.fillMaxHeight(),
                            header = {
                                Spacer(modifier = Modifier.height(12.dp))
                                FloatingActionButton(
                                    onClick = { navController.navigate(RouteScanner) },
                                    containerColor = ElectricBlue,
                                    contentColor = Color.White,
                                ) {
                                    Icon(Icons.Outlined.Add, contentDescription = strings.navScan)
                                }
                            },
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            NavigationRailItem(
                                selected = currentTab == MainTab.HOME,
                                onClick = { onSelectTab(MainTab.HOME) },
                                icon = { Icon(Icons.Outlined.Home, contentDescription = strings.navHome) },
                                label = { Text(strings.navHome) },
                            )
                            NavigationRailItem(
                                selected = currentTab == MainTab.TOOLS,
                                onClick = { onSelectTab(MainTab.TOOLS) },
                                icon = { Icon(Icons.Outlined.Build, contentDescription = strings.navTools) },
                                label = { Text(strings.navTools) },
                            )
                            NavigationRailItem(
                                selected = currentTab == MainTab.VAULT,
                                onClick = { onSelectTab(MainTab.VAULT) },
                                icon = { Icon(Icons.Outlined.Lock, contentDescription = strings.navVault) },
                                label = { Text(strings.navVault) },
                            )
                            NavigationRailItem(
                                selected = currentTab == MainTab.SETTINGS,
                                onClick = { onSelectTab(MainTab.SETTINGS) },
                                icon = { Icon(Icons.Outlined.Settings, contentDescription = strings.navSettings) },
                                label = { Text(strings.navSettings) },
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = startDest,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(if (isWide) Modifier.widthIn(max = 920.dp) else Modifier),
                        ) {
                            composable<RouteOnboarding> {
                                OnboardingScreen(
                                    onFinished = {
                                        viewModel.completeOnboarding()
                                        navController.navigate(RouteHome) {
                                            popUpTo(RouteOnboarding) { inclusive = true }
                                        }
                                    },
                                )
                            }

                            composable<RouteHome> {
                                LibraryScreen(
                                    documents = libraryDocs,
                                    folders = folders,
                                    onDocumentClick = { docId -> navController.navigate(RouteDetail(docId)) },
                                    onCreateFolder = { name, color -> viewModel.createFolder(name, color) },
                                    onLockToVault = { ids -> viewModel.lockDocumentsToVault(ids) },
                                    onMoveToFolder = { ids, folderId -> viewModel.moveDocumentsToFolder(ids, folderId) },
                                    onDeleteDocuments = { ids -> viewModel.deleteDocuments(ids) },
                                    onExportDocument = { docId -> exportDocId = docId },
                                    onOpenVault = { onSelectTab(MainTab.VAULT) },
                                )
                            }

                            composable<RouteTools> {
                                ToolsScreen(
                                    documents = libraryDocs,
                                    viewModel = viewModel,
                                    onOpenSignature = { docId -> navController.navigate(RouteSignature(docId)) },
                                    onOpenRedact = { docId -> navController.navigate(RouteRedact(docId)) },
                                    onOpenQrHub = { navController.navigate(RouteQrHub) },
                                    onOpenDocument = { docId -> navController.navigate(RouteDetail(docId)) },
                                )
                            }

                            composable<RouteVault> {
                                VaultScreen(
                                    viewModel = viewModel,
                                    onOpenDocument = { docId -> navController.navigate(RouteDetail(docId)) },
                                )
                            }

                            composable<RouteSettings> {
                                SettingsScreen(viewModel = viewModel)
                            }

                            composable<RouteScanner> {
                                ScannerScreen(
                                    viewModel = viewModel,
                                    onClose = { navController.popBackStack() },
                                    onSavedDocument = { docId ->
                                        navController.navigate(RouteDetail(docId)) {
                                            popUpTo(RouteScanner) { inclusive = true }
                                        }
                                    },
                                )
                            }

                            composable<RouteDetail> { entry ->
                                val route = entry.toRoute<RouteDetail>()
                                DocumentDetailScreen(
                                    docId = route.docId,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onOpenExport = { exportDocId = route.docId },
                                    onOpenSign = { navController.navigate(RouteSignature(route.docId)) },
                                    onOpenRedact = { navController.navigate(RouteRedact(route.docId)) },
                                )
                            }

                            composable<RouteSignature> { entry ->
                                val route = entry.toRoute<RouteSignature>()
                                SignatureScreen(
                                    initialDocId = route.docId,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                )
                            }

                            composable<RouteRedact> { entry ->
                                val route = entry.toRoute<RouteRedact>()
                                RedactionScreen(
                                    initialDocId = route.docId,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onDoneDocument = { newDocId ->
                                        navController.navigate(RouteDetail(newDocId)) {
                                            popUpTo(RouteRedact(route.docId)) { inclusive = true }
                                        }
                                    },
                                )
                            }

                            composable<RouteQrHub> {
                                QrHubScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                )
                            }
                        }

                        if (!isWide && showMainChrome && currentTab != null) {
                            AsifBottomBar(
                                currentTab = currentTab,
                                onSelectTab = onSelectTab,
                                onScanClick = { navController.navigate(RouteScanner) },
                                modifier = Modifier.align(Alignment.BottomCenter),
                            )
                        }
                    }
                }
            }
        }
    }

    exportDocId?.let { targetId ->
        val docTitle = allDocs.firstOrNull { it.id == targetId }?.title ?: "Document"
        ExportAndCloudSheet(
            documentTitle = docTitle,
            prefs = prefs,
            onDismiss = { exportDocId = null },
            onExportFormat = { format ->
                viewModel.exportAndShare(targetId, format, prefs.defaultScanQuality.jpegQuality)
                exportDocId = null
            },
            onToggleGoogleDrive = { viewModel.setGoogleDriveOptIn(it) },
            onToggleDropbox = { viewModel.setDropboxOptIn(it) },
        )
    }
}
