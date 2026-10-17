package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LaserLayer
import com.example.model.LayerMode
import com.example.ui.theme.LaserBorder
import com.example.ui.theme.LaserPrimary
import com.example.ui.theme.LaserSurface
import com.example.ui.theme.LaserSurfaceElevated
import java.util.Locale

@Composable
fun LayerSettingsPanel(
    modifier: Modifier = Modifier,
    layers: List<LaserLayer>,
    activeLayerId: String,
    onSelectActiveLayer: (String) -> Unit,
    onToggleLayerEnabled: (String) -> Unit,
    onUpdateLayer: (LaserLayer) -> Unit,
    onOpenMaterialLibrary: () -> Unit
) {
    val activeLayer = layers.find { it.id == activeLayerId } ?: layers.firstOrNull()

    Column(
        modifier = modifier
            .background(LaserSurface)
            .border(1.dp, LaserBorder)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
            .testTag("layer_settings_panel"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Layers, contentDescription = null, tint = LaserPrimary, modifier = Modifier.size(18.dp))
                Text("Layers & Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onOpenMaterialLibrary,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text("Presets", fontSize = 12.sp)
            }
        }

        // Layer List Cards
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (layer in layers) {
                val isSelected = layer.id == activeLayerId
                OutlinedCard(
                    onClick = { onSelectActiveLayer(layer.id) },
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (isSelected) LaserSurfaceElevated else Color.Transparent
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 1.5f.dp else 1.dp,
                        color = if (isSelected) LaserPrimary else LaserBorder
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Layer Color Swatch
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(layer.composeColor)
                            )
                            Column {
                                Text(
                                    text = layer.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = "${layer.mode.displayName} • ${layer.speedMmMin.toInt()} mm/min • ${layer.maxPowerPercent.toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Enable / Disable Eye Toggle
                        IconButton(
                            onClick = { onToggleLayerEnabled(layer.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (layer.enabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Layer Output",
                                tint = if (layer.enabled) LaserPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = LaserBorder, modifier = Modifier.padding(vertical = 4.dp))

        // Active Layer Detailed Properties
        if (activeLayer != null) {
            Text(
                "Edit ${activeLayer.name} Parameters",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = LaserPrimary
            )

            // Mode Selector: Cut / Engrave / Mark
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LayerMode.values().forEach { mode ->
                    val selected = activeLayer.mode == mode
                    FilterChip(
                        selected = selected,
                        onClick = { onUpdateLayer(activeLayer.copy(mode = mode)) },
                        label = { Text(mode.displayName, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LaserPrimary,
                            selectedLabelColor = Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Speed Slider (mm/min)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Cutting Speed:", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${activeLayer.speedMmMin.toInt()} mm/min",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = LaserPrimary
                    )
                }
                Slider(
                    value = activeLayer.speedMmMin,
                    onValueChange = { onUpdateLayer(activeLayer.copy(speedMmMin = it)) },
                    valueRange = 50f..12000f,
                    colors = SliderDefaults.colors(
                        thumbColor = LaserPrimary,
                        activeTrackColor = LaserPrimary
                    )
                )
            }

            // Max Power Slider (%)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Laser Power (Max):", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${activeLayer.maxPowerPercent.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF3D00)
                    )
                }
                Slider(
                    value = activeLayer.maxPowerPercent,
                    onValueChange = { onUpdateLayer(activeLayer.copy(maxPowerPercent = it)) },
                    valueRange = 1f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFF3D00),
                        activeTrackColor = Color(0xFFFF3D00)
                    )
                )
            }

            // Passes Selector & Air Assist Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Passes
                Column {
                    Text("Passes: ${activeLayer.passes}", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(1, 2, 3, 4, 5).forEach { p ->
                            val isP = activeLayer.passes == p
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isP) LaserPrimary else LaserSurfaceElevated)
                                    .clickable { onUpdateLayer(activeLayer.copy(passes = p)) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$p",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isP) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }

                // Air Assist
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Air, contentDescription = null, tint = LaserPrimary, modifier = Modifier.size(16.dp))
                        Text("Air Assist (M8)", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = activeLayer.airAssist,
                        onCheckedChange = { onUpdateLayer(activeLayer.copy(airAssist = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = LaserPrimary,
                            checkedTrackColor = LaserPrimary.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // Line interval for raster engraving
            if (activeLayer.mode == LayerMode.ENGRAVE) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Line Interval:", style = MaterialTheme.typography.bodySmall)
                        Text(
                            String.format(Locale.US, "%.2f mm (%.0f DPI)", activeLayer.lineIntervalMm, 25.4f / activeLayer.lineIntervalMm),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = LaserPrimary
                        )
                    }
                    Slider(
                        value = activeLayer.lineIntervalMm,
                        onValueChange = { onUpdateLayer(activeLayer.copy(lineIntervalMm = it)) },
                        valueRange = 0.05f..0.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = LaserPrimary,
                            activeTrackColor = LaserPrimary
                        )
                    )
                }
            }
        }
    }
}
