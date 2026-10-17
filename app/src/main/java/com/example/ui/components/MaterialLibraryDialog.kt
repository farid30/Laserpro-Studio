package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MaterialPreset
import com.example.ui.theme.LaserBorder
import com.example.ui.theme.LaserDarkBg
import com.example.ui.theme.LaserPrimary
import com.example.ui.theme.LaserSurface
import com.example.ui.theme.LaserSurfaceElevated
import com.example.ui.theme.LaserWarningAmber

@Composable
fun MaterialLibraryScreen(
    modifier: Modifier = Modifier,
    presets: List<MaterialPreset>,
    onApplyPreset: (MaterialPreset) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = remember(presets) {
        listOf("All") + presets.map { it.category }.distinct()
    }

    val filteredPresets = if (selectedCategory == "All") {
        presets
    } else {
        presets.filter { it.category == selectedCategory }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LaserDarkBg)
            .padding(14.dp)
            .testTag("material_library_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Caution Banner (Requirement: "Material presets must be starting points only. Warn users to verify settings for their specific laser, material, and machine.")
        Card(
            colors = CardDefaults.cardColors(containerColor = LaserWarningAmber.copy(alpha = 0.15f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, LaserWarningAmber),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = LaserWarningAmber)
                Text(
                    text = "DISCLAIMER: Material presets are baseline starting points only. Always run a small test cut matrix on scrap material for your specific laser wattage, optical focal lens, and workpiece thickness.",
                    color = LaserWarningAmber,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp
                )
            }
        }

        // Category Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LaserPrimary,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        // Presets List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredPresets) { preset ->
                PresetCard(preset = preset, onApply = { onApplyPreset(preset) })
            }
        }
    }
}

@Composable
private fun PresetCard(preset: MaterialPreset, onApply: () -> Unit) {
    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(containerColor = LaserSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, LaserBorder),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(preset.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("${preset.category} • ${preset.thicknessMm} mm thick", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = onApply,
                    colors = ButtonDefaults.buttonColors(containerColor = LaserPrimary, contentColor = Color.Black),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Apply to Layer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Specs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LaserSurfaceElevated, RoundedCornerShape(6.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("MODE", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(preset.recommendedMode.displayName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LaserPrimary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SPEED", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${preset.speedMmMin.toInt()} mm/min", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("POWER", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${preset.maxPowerPercent.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF3D00))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PASSES", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${preset.passes} pass", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("AIR ASSIST", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (preset.airAssist) "ON (M8)" else "OFF", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (preset.airAssist) LaserPrimary else Color.Gray)
                }
            }

            // Material Notes
            Text(
                preset.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}
