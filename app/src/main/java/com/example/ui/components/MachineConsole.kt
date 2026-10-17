package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LaserBorder
import com.example.ui.theme.LaserDarkBg
import com.example.ui.theme.LaserEmergencyRed
import com.example.ui.theme.LaserPrimary
import com.example.ui.theme.LaserSuccessGreen
import com.example.ui.theme.LaserSurface
import com.example.ui.theme.LaserSurfaceElevated
import com.example.ui.theme.LaserWarningAmber

@Composable
fun MachineConsole(
    modifier: Modifier = Modifier,
    logs: List<String>,
    onSendCommand: (String) -> Unit,
    onClearLogs: () -> Unit
) {
    var cmdText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to latest log
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .background(LaserSurface)
            .border(1.dp, LaserBorder)
            .padding(8.dp)
            .testTag("machine_console"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Console Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Terminal, contentDescription = null, tint = LaserPrimary, modifier = Modifier.size(16.dp))
                Text("Controller Serial Console", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onClearLogs, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.ClearAll, contentDescription = "Clear logs", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }

        // Quick Macro Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("?", "\$\$", "\$H", "\$X", "G0 X0 Y0", "G92 X0 Y0", "M5", "M3 S10").forEach { macro ->
                FilterChip(
                    selected = false,
                    onClick = { onSendCommand(macro) },
                    label = { Text(macro, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = FilterChipDefaults.filterChipColors(containerColor = LaserSurfaceElevated),
                    modifier = Modifier.height(26.dp)
                )
            }
        }

        // Terminal Log Area
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = LaserDarkBg,
            shape = RoundedCornerShape(4.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, LaserBorder)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (logs.isEmpty()) {
                    item {
                        Text(
                            "Console initialized. Connect USB controller or mock machine to stream G-code...",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                items(logs) { line ->
                    val color = when {
                        line.startsWith(">") -> LaserPrimary
                        line.startsWith("[ERROR]") || line.startsWith("[EMERGENCY") || line.contains("ALARM:") -> LaserEmergencyRed
                        line.startsWith("[WARN]") -> LaserWarningAmber
                        line.startsWith("ok") || line.contains("Grbl") -> LaserSuccessGreen
                        else -> Color(0xFFC9D1D9)
                    }
                    Text(
                        text = line,
                        color = color,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        // Command Input Field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = cmdText,
                onValueChange = { cmdText = it },
                placeholder = { Text("Enter G-code command (e.g. G0 X10 Y10)...", fontSize = 12.sp) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LaserPrimary,
                    unfocusedBorderColor = LaserBorder,
                    focusedContainerColor = LaserDarkBg,
                    unfocusedContainerColor = LaserDarkBg
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (cmdText.isNotBlank()) {
                        onSendCommand(cmdText)
                        cmdText = ""
                    }
                }),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            )
            Button(
                onClick = {
                    if (cmdText.isNotBlank()) {
                        onSendCommand(cmdText)
                        cmdText = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LaserPrimary, contentColor = Color.Black),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(44.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
            }
        }
    }
}
