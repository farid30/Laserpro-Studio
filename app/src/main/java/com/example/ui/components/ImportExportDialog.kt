package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.GCodeJob
import com.example.model.LaserLayer
import com.example.model.LaserShape
import com.example.model.MachineProfile
import com.example.model.ProjectFileManager
import com.example.model.SampleProjects
import com.example.ui.theme.LaserBorder
import com.example.ui.theme.LaserDarkBg
import com.example.ui.theme.LaserPrimary
import com.example.ui.theme.LaserSurface
import com.example.ui.theme.LaserSurfaceElevated

@Composable
fun ImportExportDialog(
    shapes: List<LaserShape>,
    layers: List<LaserLayer>,
    profile: MachineProfile,
    job: GCodeJob?,
    onImportSvg: (String) -> Unit,
    onImportDxf: (String) -> Unit,
    onImportRaster: (Bitmap) -> Unit,
    onLoadSample: (SampleProjects.SampleProject) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var textInput by remember { mutableStateOf("") }
    var copyNotice by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp),
            shape = RoundedCornerShape(12.dp),
            color = LaserSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, LaserBorder)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Import & Export CAD/CAM Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = onDismiss,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Close", fontSize = 11.sp)
                    }
                }

                // Tabs: 0 = Import, 1 = Export, 2 = Sample Projects
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = LaserSurfaceElevated,
                    contentColor = LaserPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = LaserPrimary
                        )
                    }
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Import", fontSize = 12.sp) })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Export", fontSize = 12.sp) })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Sample Designs", fontSize = 12.sp) })
                }

                // Tab Content
                when (selectedTab) {
                    0 -> {
                        // Import tab
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Paste SVG or DXF Code:", style = MaterialTheme.typography.labelSmall)
                            OutlinedTextField(
                                value = textInput,
                                onValueChange = { textInput = it },
                                placeholder = { Text("<svg xmlns=\"...\"> or DXF 0\\nSECTION...", fontSize = 11.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LaserPrimary,
                                    unfocusedBorderColor = LaserBorder,
                                    focusedContainerColor = LaserDarkBg,
                                    unfocusedContainerColor = LaserDarkBg
                                )
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        if (textInput.isNotBlank()) {
                                            onImportSvg(textInput)
                                            onDismiss()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = LaserPrimary, contentColor = Color.Black),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Import SVG", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = {
                                        if (textInput.isNotBlank()) {
                                            onImportDxf(textInput)
                                            onDismiss()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = LaserPrimary, contentColor = Color.Black),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Import DXF", fontSize = 12.sp)
                                }
                            }

                            HorizontalDivider(color = LaserBorder, modifier = Modifier.padding(vertical = 4.dp))

                            // Test Raster Image Generation Button
                            OutlinedButton(
                                onClick = {
                                    // Generate a sample graphic for raster engraving test
                                    val bmp = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
                                    val c = Canvas(bmp)
                                    c.drawColor(AndroidColor.WHITE)
                                    val p = Paint().apply {
                                        color = AndroidColor.BLACK
                                        textSize = 24f
                                        isAntiAlias = true
                                    }
                                    c.drawCircle(60f, 60f, 45f, p)
                                    p.color = AndroidColor.WHITE
                                    c.drawText("CNC", 35f, 68f, p)
                                    onImportRaster(bmp)
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Generate Sample Raster Engrave Image", fontSize = 12.sp)
                            }
                        }
                    }
                    1 -> {
                        // Export tab
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExportItem(
                                title = "Export GRBL G-Code (.nc / .gcode)",
                                desc = "${job?.segments?.size ?: 0} movements ready for machine",
                                onCopy = {
                                    copyToClipboard(context, job?.rawGCode ?: "; No G-code generated", "G-Code")
                                    copyNotice = "Copied GRBL G-Code to clipboard!"
                                }
                            )

                            ExportItem(
                                title = "Export Vector SVG (.svg)",
                                desc = "${shapes.size} elements with millimeter scaling",
                                onCopy = {
                                    val svg = ProjectFileManager.exportToSvg(shapes, layers, profile.bedWidthMm, profile.bedHeightMm)
                                    copyToClipboard(context, svg, "SVG")
                                    copyNotice = "Copied SVG to clipboard!"
                                }
                            )

                            ExportItem(
                                title = "Export AutoCAD DXF (.dxf)",
                                desc = "Standard 2D CNC CAM exchange format",
                                onCopy = {
                                    val dxf = ProjectFileManager.exportToDxf(shapes)
                                    copyToClipboard(context, dxf, "DXF")
                                    copyNotice = "Copied DXF to clipboard!"
                                }
                            )

                            ExportItem(
                                title = "Export Native Project (.lpro)",
                                desc = "All shapes, layers, settings, and machine profile",
                                onCopy = {
                                    val json = ProjectFileManager.serializeProject("Laser Project", shapes, layers, profile)
                                    copyToClipboard(context, json, "LaserPro Project")
                                    copyNotice = "Copied Native Project JSON to clipboard!"
                                }
                            )

                            copyNotice?.let {
                                Text(it, color = LaserPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    2 -> {
                        // Sample designs tab
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SampleProjects.getSampleProjects().forEach { sample ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = LaserSurfaceElevated),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(sample.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(sample.subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Button(
                                            onClick = {
                                                onLoadSample(sample)
                                                onDismiss()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = LaserPrimary, contentColor = Color.Black),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Load", fontSize = 11.sp)
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

@Composable
private fun ExportItem(title: String, desc: String, onCopy: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = LaserSurfaceElevated),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(
                onClick = onCopy,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("Copy", fontSize = 11.sp)
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String, label: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}
