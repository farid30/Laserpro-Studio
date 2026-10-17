package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.machine.ConnectionType
import com.example.machine.MachineState
import com.example.ui.components.CadCanvas
import com.example.ui.components.DrawingToolbar
import com.example.ui.components.GCodePreviewScreen
import com.example.ui.components.ImportExportDialog
import com.example.ui.components.LayerSettingsPanel
import com.example.ui.components.MachineConsole
import com.example.ui.components.MachineControlPanel
import com.example.ui.components.MachineSettingsScreen
import com.example.ui.components.MaterialLibraryScreen
import com.example.ui.components.SafetyNoticeDialog
import com.example.ui.theme.LaserBorder
import com.example.ui.theme.LaserDarkBg
import com.example.ui.theme.LaserEmergencyRed
import com.example.ui.theme.LaserPrimary
import com.example.ui.theme.LaserSuccessGreen
import com.example.ui.theme.LaserSurface
import com.example.ui.theme.LaserSurfaceElevated
import com.example.ui.theme.LaserWarningAmber
import com.example.viewmodel.LaserStudioViewModel
import com.example.viewmodel.StudioScreen
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaserStudioApp(
    viewModel: LaserStudioViewModel
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val shapes by viewModel.shapes.collectAsState()
    val layers by viewModel.layers.collectAsState()
    val activeLayerId by viewModel.activeLayerId.collectAsState()
    val selectedShapeIds by viewModel.selectedShapeIds.collectAsState()
    val activeTool by viewModel.activeTool.collectAsState()
    val machineProfile by viewModel.machineProfile.collectAsState()
    val materialPresets by viewModel.materialPresets.collectAsState()
    val generatedJob by viewModel.generatedJob.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val zoomScale by viewModel.zoomScale.collectAsState()
    val panOffsetX by viewModel.panOffsetX.collectAsState()
    val panOffsetY by viewModel.panOffsetY.collectAsState()

    val machineStatus by viewModel.machineController.status.collectAsState()
    val consoleLogs by viewModel.machineController.consoleLogs.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var showImportExportDialog by remember { mutableStateOf(false) }
    var showSafetyDialog by remember { mutableStateOf(false) }
    var showLayersBottomSheet by remember { mutableStateOf(false) }
    var showConsoleBottomSheet by remember { mutableStateOf(false) }

    // User message toasts / snackbar
    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    // Predictive Back Handling
    BackHandler(enabled = currentScreen != StudioScreen.WORKSPACE) {
        viewModel.setScreen(StudioScreen.WORKSPACE)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = LaserDarkBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopStudioBar(
                projectName = viewModel.projectName.collectAsState().value,
                currentScreen = currentScreen,
                machineStatus = machineStatus,
                onSelectScreen = { viewModel.setScreen(it) },
                onOpenImportExport = { showImportExportDialog = true },
                onOpenSafety = { showSafetyDialog = true },
                onEmergencyStop = { viewModel.machineController.emergencyStop() },
                onToggleMachineConnect = {
                    if (machineStatus.isConnected) {
                        viewModel.machineController.disconnect()
                    } else {
                        // Attempt USB device list or connect mock
                        val usbDevices = viewModel.machineController.usbSerialManager.listDevices()
                        if (usbDevices.isNotEmpty()) {
                            viewModel.machineController.connectUsb(usbDevices.first().device)
                        } else {
                            viewModel.machineController.connectMock()
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                StudioScreen.WORKSPACE -> {
                    // CAD Design Workspace
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Drawing Toolbar (Top strip)
                        DrawingToolbar(
                            isVertical = false,
                            activeTool = activeTool,
                            onToolSelected = { viewModel.setTool(it) },
                            canUndo = canUndo,
                            canRedo = canRedo,
                            hasSelection = selectedShapeIds.isNotEmpty(),
                            onUndo = { viewModel.undo() },
                            onRedo = { viewModel.redo() },
                            onDuplicate = { viewModel.duplicateSelected() },
                            onDelete = { viewModel.deleteSelected() },
                            onAlign = { viewModel.alignSelected(it) },
                            onZoomIn = { viewModel.updateZoomPan(1.25f, 0f, 0f) },
                            onZoomOut = { viewModel.updateZoomPan(0.8f, 0f, 0f) },
                            onResetZoom = { viewModel.resetView() }
                        )

                        // Center Canvas with Quick Float Buttons
                        Box(modifier = Modifier.weight(1f)) {
                            CadCanvas(
                                shapes = shapes,
                                layers = layers,
                                profile = machineProfile,
                                selectedShapeIds = selectedShapeIds,
                                activeTool = activeTool,
                                activeLayerId = activeLayerId,
                                zoomScale = zoomScale,
                                panOffsetX = panOffsetX,
                                panOffsetY = panOffsetY,
                                onTransform = { z, px, py -> viewModel.updateZoomPan(z, px, py) },
                                onShapeSelect = { id, add -> viewModel.selectShape(id, add) },
                                onShapeTranslate = { dx, dy -> viewModel.translateSelected(dx, dy) },
                                onTransformationCommit = { viewModel.commitTransformation() },
                                onShapeCreate = { viewModel.addShape(it) }
                            )

                            // Floating Quick Control Pills
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Layer Panel Trigger
                                Button(
                                    onClick = { showLayersBottomSheet = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = LaserSurfaceElevated, contentColor = LaserPrimary),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Layers (${layers.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                // Quick Console Trigger
                                OutlinedButton(
                                    onClick = { showConsoleBottomSheet = true },
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Console", fontSize = 12.sp)
                                }

                                // Frame Design Trigger
                                OutlinedButton(
                                    onClick = { viewModel.frameSelectedOrAll() },
                                    enabled = machineStatus.isConnected,
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.Default.CropFree, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Frame", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                StudioScreen.PREVIEW -> {
                    GCodePreviewScreen(
                        job = generatedJob,
                        profile = machineProfile,
                        onStartMachineJob = {
                            generatedJob?.let {
                                viewModel.machineController.startJob(it)
                                viewModel.setScreen(StudioScreen.CONTROL)
                            }
                        },
                        onExportGCode = {
                            showImportExportDialog = true
                        }
                    )
                }

                StudioScreen.CONTROL -> {
                    // Machine Controller + Console
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MachineControlPanel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1.3f),
                            status = machineStatus,
                            onConnectUsb = {
                                val usbDevices = viewModel.machineController.usbSerialManager.listDevices()
                                if (usbDevices.isNotEmpty()) {
                                    viewModel.machineController.connectUsb(usbDevices.first().device)
                                } else {
                                    viewModel.machineController.connectMock()
                                }
                            },
                            onConnectMock = { viewModel.machineController.connectMock() },
                            onDisconnect = { viewModel.machineController.disconnect() },
                            onJog = { dx, dy -> viewModel.machineController.jog(dx, dy) },
                            onHome = { viewModel.machineController.homeMachine() },
                            onZeroOrigin = { viewModel.machineController.setWorkOriginZero() },
                            onMoveToOrigin = { viewModel.machineController.moveToOrigin() },
                            onUnlock = { viewModel.machineController.unlockAlarm() },
                            onFrame = { viewModel.frameSelectedOrAll() },
                            onStartJob = {
                                val job = viewModel.generateGCodeJob()
                                viewModel.machineController.startJob(job)
                            },
                            onPauseJob = { viewModel.machineController.pauseJob() },
                            onResumeJob = { viewModel.machineController.resumeJob() },
                            onStopJob = { viewModel.machineController.stopStreamingJob() },
                            onEmergencyStop = { viewModel.machineController.emergencyStop() }
                        )

                        MachineConsole(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.9f),
                            logs = consoleLogs,
                            onSendCommand = { viewModel.machineController.sendCommand(it) },
                            onClearLogs = { viewModel.machineController.clearLogs() }
                        )
                    }
                }

                StudioScreen.MATERIALS -> {
                    MaterialLibraryScreen(
                        presets = materialPresets,
                        onApplyPreset = {
                            viewModel.applyMaterialPresetToActiveLayer(it)
                            viewModel.setScreen(StudioScreen.WORKSPACE)
                        }
                    )
                }

                StudioScreen.SETTINGS -> {
                    MachineSettingsScreen(
                        profile = machineProfile,
                        onSaveProfile = { viewModel.updateMachineProfile(it) },
                        onQueryGrblSettings = {
                            viewModel.machineController.sendCommand("$$")
                            showConsoleBottomSheet = true
                        }
                    )
                }
            }
        }
    }

    // Modal Sheet for Layers in CAD Workspace
    if (showLayersBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLayersBottomSheet = false },
            containerColor = LaserSurface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = LaserBorder) }
        ) {
            Box(modifier = Modifier.padding(bottom = 24.dp)) {
                LayerSettingsPanel(
                    layers = layers,
                    activeLayerId = activeLayerId,
                    onSelectActiveLayer = { viewModel.setActiveLayer(it) },
                    onToggleLayerEnabled = { viewModel.toggleLayerEnabled(it) },
                    onUpdateLayer = { viewModel.updateLayer(it) },
                    onOpenMaterialLibrary = {
                        showLayersBottomSheet = false
                        viewModel.setScreen(StudioScreen.MATERIALS)
                    }
                )
            }
        }
    }

    // Modal Sheet for Real-time Console in CAD Workspace
    if (showConsoleBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showConsoleBottomSheet = false },
            containerColor = LaserSurface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = LaserBorder) }
        ) {
            Box(modifier = Modifier.height(340.dp).padding(bottom = 24.dp)) {
                MachineConsole(
                    logs = consoleLogs,
                    onSendCommand = { viewModel.machineController.sendCommand(it) },
                    onClearLogs = { viewModel.machineController.clearLogs() }
                )
            }
        }
    }

    // Import / Export Dialog
    if (showImportExportDialog) {
        ImportExportDialog(
            shapes = shapes,
            layers = layers,
            profile = machineProfile,
            job = generatedJob,
            onImportSvg = { viewModel.importSvg(it) },
            onImportDxf = { viewModel.importDxf(it) },
            onImportRaster = { viewModel.importRasterBitmap(it) },
            onLoadSample = { viewModel.loadSampleProject(it) },
            onDismiss = { showImportExportDialog = false }
        )
    }

    // Safety Dialog
    if (showSafetyDialog) {
        SafetyNoticeDialog(
            onDismiss = { showSafetyDialog = false }
        )
    }
}

@Composable
private fun TopStudioBar(
    projectName: String,
    currentScreen: StudioScreen,
    machineStatus: com.example.machine.GrblStatus,
    onSelectScreen: (StudioScreen) -> Unit,
    onOpenImportExport: () -> Unit,
    onOpenSafety: () -> Unit,
    onEmergencyStop: () -> Unit,
    onToggleMachineConnect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("top_studio_bar"),
        color = LaserSurface,
        tonalElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, LaserBorder)
    ) {
        Column {
            // Main Top Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // App Emblem & Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            "LaserPro Studio",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            projectName,
                            style = MaterialTheme.typography.labelSmall,
                            color = LaserPrimary,
                            maxLines = 1
                        )
                    }
                }

                // Right Actions: Machine Status Pill, Import/Export, Safety Warning, E-STOP
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Machine Status Chip
                    val isConnected = machineStatus.isConnected
                    val statusText = if (!isConnected) "Offline" else if (machineStatus.connectionType == ConnectionType.MOCK_SIMULATOR) "Mock CNC" else "USB Online"
                    val statusColor = if (!isConnected) Color.Gray else if (machineStatus.state == MachineState.ALARM) LaserEmergencyRed else LaserSuccessGreen

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onToggleMachineConnect() }
                            .testTag("machine_status_pill"),
                        color = LaserSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(statusColor))
                            Text(statusText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
                        }
                    }

                    // Import / Export
                    IconButton(
                        onClick = onOpenImportExport,
                        modifier = Modifier.size(32.dp).testTag("import_export_button")
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Import or Export", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    // Safety Advisory
                    IconButton(
                        onClick = onOpenSafety,
                        modifier = Modifier.size(32.dp).testTag("safety_notice_button")
                    ) {
                        Icon(Icons.Default.Security, contentDescription = "Laser Safety Notice", tint = LaserWarningAmber, modifier = Modifier.size(19.dp))
                    }

                    // Fast E-Stop
                    Button(
                        onClick = onEmergencyStop,
                        colors = ButtonDefaults.buttonColors(containerColor = LaserEmergencyRed, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp).testTag("quick_estop_button")
                    ) {
                        Text("E-STOP", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            // Screen Selector Navigation Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LaserSurfaceElevated)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StudioScreen.values().forEach { screen ->
                    val isSelected = currentScreen == screen
                    val icon = when (screen) {
                        StudioScreen.WORKSPACE -> Icons.Default.DesignServices
                        StudioScreen.PREVIEW -> Icons.Default.Visibility
                        StudioScreen.CONTROL -> Icons.Default.SettingsRemote
                        StudioScreen.MATERIALS -> Icons.Default.LibraryBooks
                        StudioScreen.SETTINGS -> Icons.Default.Tune
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectScreen(screen) },
                        leadingIcon = {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        label = { Text(screen.title, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LaserPrimary,
                            selectedLabelColor = Color.Black,
                            selectedLeadingIconColor = Color.Black
                        ),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("nav_${screen.name.lowercase()}")
                    )
                }
            }
        }
    }
}
