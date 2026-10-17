package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.GCodeJob
import com.example.model.MachineProfile
import com.example.model.ToolpathType
import com.example.ui.theme.LaserBorder
import com.example.ui.theme.LaserDarkBg
import com.example.ui.theme.LaserEmergencyRed
import com.example.ui.theme.LaserPrimary
import com.example.ui.theme.LaserSuccessGreen
import com.example.ui.theme.LaserSurface
import com.example.ui.theme.LaserSurfaceElevated
import com.example.ui.theme.LaserWarningAmber
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun GCodePreviewScreen(
    modifier: Modifier = Modifier,
    job: GCodeJob?,
    profile: MachineProfile,
    onStartMachineJob: () -> Unit,
    onExportGCode: () -> Unit
) {
    var simulationProgress by remember { mutableFloatStateOf(1.0f) }
    var isSimulating by remember { mutableStateOf(false) }
    var showGCodeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isSimulating) {
        while (isSimulating) {
            delay(30)
            simulationProgress += 0.008f
            if (simulationProgress >= 1f) {
                simulationProgress = 1f
                isSimulating = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LaserDarkBg)
            .padding(12.dp)
            .testTag("gcode_preview_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Out of Bounds Warning Banner
        if (job?.isWithinLimits == false) {
            Card(
                colors = CardDefaults.cardColors(containerColor = LaserEmergencyRed.copy(alpha = 0.2f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, LaserEmergencyRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = LaserEmergencyRed)
                    Text(
                        job.outOfBoundsWarning ?: "Warning: Toolpath extends outside configured machine bed!",
                        color = LaserEmergencyRed,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Stats Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard("Estimated Time", job?.estimatedTimeFormatted ?: "0s", LaserPrimary, Modifier.weight(1f))
            StatCard("Cut Distance", String.format(Locale.US, "%.1f mm", job?.cuttingDistanceMm ?: 0f), Color(0xFFFF3D00), Modifier.weight(1f))
            StatCard("Rapid Moves", String.format(Locale.US, "%.1f mm", job?.rapidDistanceMm ?: 0f), Color(0xFFFFB300), Modifier.weight(1f))
            StatCard("Total Moves", "${job?.segments?.size ?: 0} segs", Color.White, Modifier.weight(1f))
        }

        // Simulation Toolpath Canvas
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = Color(0xFF141920),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, LaserBorder)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val canvasH = size.height

                // Scale to fit bed in canvas with padding
                val margin = 20f
                val scaleX = (canvasW - margin * 2f) / profile.bedWidthMm
                val scaleY = (canvasH - margin * 2f) / profile.bedHeightMm
                val scale = minOf(scaleX, scaleY)

                val bedScreenW = profile.bedWidthMm * scale
                val bedScreenH = profile.bedHeightMm * scale
                val offsetX = margin + (canvasW - margin * 2f - bedScreenW) / 2f
                val offsetY = margin + (canvasH - margin * 2f - bedScreenH) / 2f

                // Draw Machine Bed Outline
                drawRect(
                    color = Color(0xFF1C2430),
                    topLeft = Offset(offsetX, offsetY),
                    size = Size(bedScreenW, bedScreenH)
                )
                drawRect(
                    color = LaserBorder,
                    topLeft = Offset(offsetX, offsetY),
                    size = Size(bedScreenW, bedScreenH),
                    style = Stroke(width = 1.5f)
                )

                // Draw Grid (50mm major)
                var mx = 0f
                while (mx <= profile.bedWidthMm) {
                    val px = offsetX + mx * scale
                    drawLine(Color(0xFF283242), Offset(px, offsetY), Offset(px, offsetY + bedScreenH), 1f)
                    mx += 50f
                }
                var my = 0f
                while (my <= profile.bedHeightMm) {
                    val py = offsetY + my * scale
                    drawLine(Color(0xFF283242), Offset(offsetX, py), Offset(offsetX + bedScreenW, py), 1f)
                    my += 50f
                }

                // Draw Toolpath Segments up to simulationProgress
                val segments = job?.segments ?: emptyList()
                val activeCount = (segments.size * simulationProgress).toInt().coerceIn(0, segments.size)

                var laserHeadPos: Offset? = null

                for (i in 0 until activeCount) {
                    val seg = segments[i]
                    val p1 = Offset(offsetX + seg.startX * scale, offsetY + seg.startY * scale)
                    val p2 = Offset(offsetX + seg.endX * scale, offsetY + seg.endY * scale)

                    if (seg.type == ToolpathType.RAPID) {
                        // Rapid move: dashed yellow
                        drawLine(
                            color = Color(0xFFFFB300).copy(alpha = 0.5f),
                            start = p1,
                            end = p2,
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )
                    } else {
                        // Cut or Engrave move: solid colored line
                        drawLine(
                            color = Color(seg.layerColorHex),
                            start = p1,
                            end = p2,
                            strokeWidth = 2f
                        )
                    }

                    if (i == activeCount - 1) {
                        laserHeadPos = p2
                    }
                }

                // Draw Animated Laser Nozzle Head Marker
                laserHeadPos?.let { pos ->
                    drawCircle(color = Color(0xFF00E5FF), center = pos, radius = 6f)
                    drawCircle(color = Color.White, center = pos, radius = 3f)
                    drawCircle(
                        color = Color(0xFFFF3D00).copy(alpha = 0.4f),
                        center = pos,
                        radius = 12f,
                        style = Stroke(width = 1.5f)
                    )
                }
            }
        }

        // Simulation Playback Scrubber Controls
        Card(
            colors = CardDefaults.cardColors(containerColor = LaserSurfaceElevated),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledIconButton(
                    onClick = {
                        if (simulationProgress >= 1f) simulationProgress = 0f
                        isSimulating = !isSimulating
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = LaserPrimary,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isSimulating) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Simulate"
                    )
                }

                FilledIconButton(
                    onClick = {
                        simulationProgress = 0f
                        isSimulating = false
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = LaserSurface),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Replay, contentDescription = "Restart simulation", modifier = Modifier.size(18.dp))
                }

                Slider(
                    value = simulationProgress,
                    onValueChange = {
                        simulationProgress = it
                        isSimulating = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = LaserPrimary,
                        activeTrackColor = LaserPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )

                Text(
                    "${(simulationProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = LaserPrimary
                )
            }
        }

        // Action Buttons: Send to Machine, View Raw G-Code, Export
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { showGCodeDialog = true },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(42.dp)
            ) {
                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("View G-Code", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onExportGCode,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(42.dp)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Export .nc", fontSize = 12.sp)
            }

            Button(
                onClick = onStartMachineJob,
                colors = ButtonDefaults.buttonColors(containerColor = LaserSuccessGreen, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1.2f).height(42.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("RUN ON CNC", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }

    // Raw G-Code Modal Dialog
    if (showGCodeDialog) {
        Dialog(onDismissRequest = { showGCodeDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp),
                shape = RoundedCornerShape(12.dp),
                color = LaserSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, LaserBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Generated GRBL G-Code", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = { showGCodeDialog = false },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Close", fontSize = 12.sp)
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        color = LaserDarkBg,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LaserBorder)
                    ) {
                        val lines = job?.rawGCode?.lines() ?: emptyList()
                        LazyColumn(modifier = Modifier.padding(8.dp)) {
                            itemsIndexed(lines) { idx, line ->
                                Row {
                                    Text(
                                        "${idx + 1}".padStart(4, ' '),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        modifier = Modifier.width(36.dp)
                                    )
                                    Text(
                                        line,
                                        color = if (line.startsWith(";")) Color(0xFF8B949E) else if (line.startsWith("G0")) Color(0xFFFFB300) else if (line.startsWith("G1")) LaserPrimary else Color.White,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
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
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = LaserSurfaceElevated,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, LaserBorder)
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
        }
    }
}
