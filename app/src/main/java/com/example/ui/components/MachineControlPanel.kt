package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.machine.ConnectionType
import com.example.machine.GrblStatus
import com.example.machine.MachineState
import com.example.ui.theme.LaserBorder
import com.example.ui.theme.LaserEmergencyRed
import com.example.ui.theme.LaserPrimary
import com.example.ui.theme.LaserSuccessGreen
import com.example.ui.theme.LaserSurface
import com.example.ui.theme.LaserSurfaceElevated
import com.example.ui.theme.LaserWarningAmber
import java.util.Locale

@Composable
fun MachineControlPanel(
    modifier: Modifier = Modifier,
    status: GrblStatus,
    onConnectUsb: () -> Unit,
    onConnectMock: () -> Unit,
    onDisconnect: () -> Unit,
    onJog: (dx: Float, dy: Float) -> Unit,
    onHome: () -> Unit,
    onZeroOrigin: () -> Unit,
    onMoveToOrigin: () -> Unit,
    onUnlock: () -> Unit,
    onFrame: () -> Unit,
    onStartJob: () -> Unit,
    onPauseJob: () -> Unit,
    onResumeJob: () -> Unit,
    onStopJob: () -> Unit,
    onEmergencyStop: () -> Unit
) {
    var jogStepMm by remember { mutableFloatStateOf(10f) }

    Column(
        modifier = modifier
            .background(LaserSurface)
            .border(1.dp, LaserBorder)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
            .testTag("machine_control_panel"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Status & Connection Card
        Card(
            colors = CardDefaults.cardColors(containerColor = LaserSurfaceElevated),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Connection State LED
                        val ledColor = when {
                            !status.isConnected -> Color.Gray
                            status.state == MachineState.ALARM -> LaserEmergencyRed
                            status.state == MachineState.RUN -> LaserWarningAmber
                            else -> LaserSuccessGreen
                        }
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(ledColor)
                        )
                        Text(
                            text = status.state.displayName,
                            fontWeight = FontWeight.Bold,
                            color = ledColor,
                            fontSize = 13.sp
                        )
                    }

                    if (status.isConnected) {
                        OutlinedButton(
                            onClick = onDisconnect,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.PowerOff, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Disconnect", fontSize = 11.sp)
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilledTonalButton(
                                onClick = onConnectMock,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Mock Machine", fontSize = 11.sp)
                            }
                            Button(
                                onClick = onConnectUsb,
                                colors = ButtonDefaults.buttonColors(containerColor = LaserPrimary, contentColor = Color.Black),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.Power, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("USB Connect", fontSize = 11.sp)
                            }
                        }
                    }
                }

                Text(
                    text = if (status.isConnected) status.deviceName else "No controller connected. Connect USB OTG or use Mock Mode.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Coordinate Readout Display (Digital DRO)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0D1117), RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("X AXIS", fontSize = 10.sp, color = LaserPrimary, fontWeight = FontWeight.Bold)
                        Text(
                            String.format(Locale.US, "%.2f mm", status.wposX),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Y AXIS", fontSize = 10.sp, color = LaserPrimary, fontWeight = FontWeight.Bold)
                        Text(
                            String.format(Locale.US, "%.2f mm", status.wposY),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LASER PWM", fontSize = 10.sp, color = Color(0xFFFF3D00), fontWeight = FontWeight.Bold)
                        Text(
                            "${status.spindleSpeed}",
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Streaming Job Progress Bar
                if (status.isStreamingJob) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Job Streaming:", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${(status.jobProgress * 100).toInt()}% (${status.linesProcessed}/${status.totalLinesInJob})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = LaserPrimary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { status.jobProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = LaserPrimary,
                            trackColor = LaserBorder
                        )
                    }
                }
            }
        }

        // Job Control Buttons: START, PAUSE, STOP, EMERGENCY STOP
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!status.isStreamingJob) {
                Button(
                    onClick = onStartJob,
                    enabled = status.isConnected,
                    colors = ButtonDefaults.buttonColors(containerColor = LaserSuccessGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("START JOB", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            } else {
                if (status.state == MachineState.HOLD) {
                    Button(
                        onClick = onResumeJob,
                        colors = ButtonDefaults.buttonColors(containerColor = LaserSuccessGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("RESUME", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onPauseJob,
                        colors = ButtonDefaults.buttonColors(containerColor = LaserWarningAmber, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("PAUSE", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onStopJob,
                    colors = ButtonDefaults.buttonColors(containerColor = LaserEmergencyRed, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("STOP", fontWeight = FontWeight.Bold)
                }
            }

            // Frame Button
            OutlinedButton(
                onClick = onFrame,
                enabled = status.isConnected && !status.isStreamingJob,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(42.dp)
            ) {
                Icon(Icons.Default.CropFree, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("FRAME", fontSize = 12.sp)
            }
        }

        // EMERGENCY STOP (Prominent red alert button)
        Button(
            onClick = onEmergencyStop,
            colors = ButtonDefaults.buttonColors(containerColor = LaserEmergencyRed, contentColor = Color.White),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("emergency_stop_button")
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("EMERGENCY STOP (E-STOP)", fontWeight = FontWeight.Black, fontSize = 13.sp)
        }

        HorizontalDivider(color = LaserBorder, modifier = Modifier.padding(vertical = 4.dp))

        // Jog Distance Steps
        Text("Jog Step Size:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(0.1f, 1f, 10f, 50f, 100f).forEach { step ->
                val isSelected = jogStepMm == step
                FilterChip(
                    selected = isSelected,
                    onClick = { jogStepMm = step },
                    label = { Text(if (step < 1f) "0.1mm" else "${step.toInt()}mm", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LaserPrimary,
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.height(28.dp)
                )
            }
        }

        // Jogging Directional Keypad
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Y+
                FilledIconButton(
                    onClick = { onJog(0f, jogStepMm) },
                    enabled = status.isConnected && !status.isStreamingJob,
                    shape = RoundedCornerShape(8.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = LaserSurfaceElevated),
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Jog Y+", modifier = Modifier.size(28.dp))
                }

                // X- , Center (Go to Origin) , X+
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FilledIconButton(
                        onClick = { onJog(-jogStepMm, 0f) },
                        enabled = status.isConnected && !status.isStreamingJob,
                        shape = RoundedCornerShape(8.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = LaserSurfaceElevated),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Jog X-", modifier = Modifier.size(28.dp))
                    }

                    FilledIconButton(
                        onClick = onMoveToOrigin,
                        enabled = status.isConnected && !status.isStreamingJob,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = LaserPrimary, contentColor = Color.Black),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Go to Work Origin (0,0)", modifier = Modifier.size(22.dp))
                    }

                    FilledIconButton(
                        onClick = { onJog(jogStepMm, 0f) },
                        enabled = status.isConnected && !status.isStreamingJob,
                        shape = RoundedCornerShape(8.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = LaserSurfaceElevated),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Jog X+", modifier = Modifier.size(28.dp))
                    }
                }

                // Y-
                FilledIconButton(
                    onClick = { onJog(0f, -jogStepMm) },
                    enabled = status.isConnected && !status.isStreamingJob,
                    shape = RoundedCornerShape(8.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = LaserSurfaceElevated),
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Jog Y-", modifier = Modifier.size(28.dp))
                }
            }
        }

        // CNC Macros: Set Origin, Home ($H), Unlock Alarm ($X)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
                onClick = onZeroOrigin,
                enabled = status.isConnected,
                modifier = Modifier.weight(1f).height(36.dp)
            ) {
                Text("Zero (0,0)", fontSize = 11.sp)
            }
            OutlinedButton(
                onClick = onHome,
                enabled = status.isConnected,
                modifier = Modifier.weight(1f).height(36.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Home", fontSize = 11.sp)
            }
            OutlinedButton(
                onClick = onUnlock,
                enabled = status.isConnected,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LaserWarningAmber),
                modifier = Modifier.weight(1f).height(36.dp)
            ) {
                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Unlock", fontSize = 11.sp)
            }
        }
    }
}
