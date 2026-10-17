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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MachineProfile
import com.example.model.OriginCorner
import com.example.ui.theme.LaserBorder
import com.example.ui.theme.LaserDarkBg
import com.example.ui.theme.LaserPrimary
import com.example.ui.theme.LaserSurface
import com.example.ui.theme.LaserSurfaceElevated
import com.example.ui.theme.LaserWarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MachineSettingsScreen(
    modifier: Modifier = Modifier,
    profile: MachineProfile,
    onSaveProfile: (MachineProfile) -> Unit,
    onQueryGrblSettings: () -> Unit
) {
    var name by remember(profile) { mutableStateOf(profile.name) }
    var bedWidth by remember(profile) { mutableStateOf(profile.bedWidthMm.toString()) }
    var bedHeight by remember(profile) { mutableStateOf(profile.bedHeightMm.toString()) }
    var maxSpeed by remember(profile) { mutableStateOf(profile.maxSpeedMmMin.toString()) }
    var rapidSpeed by remember(profile) { mutableStateOf(profile.rapidSpeedMmMin.toString()) }
    var laserMaxPwm by remember(profile) { mutableStateOf(profile.laserMaxPwm.toString()) }
    var useM4 by remember(profile) { mutableStateOf(profile.useDynamicLaserModeM4) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LaserDarkBg)
            .padding(14.dp)
            .verticalScroll(rememberScrollState())
            .testTag("machine_settings_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Read-only safety advisory
        Card(
            colors = CardDefaults.cardColors(containerColor = LaserSurfaceElevated),
            border = androidx.compose.foundation.BorderStroke(1.dp, LaserBorder),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Info, contentDescription = null, tint = LaserPrimary)
                Text(
                    "LaserPro Studio strictly enforces workspace limits in software and never automatically overwrites hardware EEPROM controller settings ($$).",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp
                )
            }
        }

        // Machine Name
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Machine Profile Name") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LaserPrimary,
                unfocusedBorderColor = LaserBorder
            )
        )

        // Bed Dimensions
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = bedWidth,
                onValueChange = { bedWidth = it },
                label = { Text("Bed Width X (mm)") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LaserPrimary,
                    unfocusedBorderColor = LaserBorder
                )
            )
            OutlinedTextField(
                value = bedHeight,
                onValueChange = { bedHeight = it },
                label = { Text("Bed Height Y (mm)") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LaserPrimary,
                    unfocusedBorderColor = LaserBorder
                )
            )
        }

        // Speeds
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = maxSpeed,
                onValueChange = { maxSpeed = it },
                label = { Text("Max Cut Speed (mm/min)") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LaserPrimary,
                    unfocusedBorderColor = LaserBorder
                )
            )
            OutlinedTextField(
                value = rapidSpeed,
                onValueChange = { rapidSpeed = it },
                label = { Text("Rapid Speed G0 (mm/min)") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LaserPrimary,
                    unfocusedBorderColor = LaserBorder
                )
            )
        }

        // Laser Power Range & GRBL M4 Dynamic Mode
        OutlinedTextField(
            value = laserMaxPwm,
            onValueChange = { laserMaxPwm = it },
            label = { Text("Laser Max PWM (\$30 setting in GRBL, typically 1000 or 255)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LaserPrimary,
                unfocusedBorderColor = LaserBorder
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LaserSurface, RoundedCornerShape(8.dp))
                .border(1.dp, LaserBorder, RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Use M4 Dynamic Laser Mode", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Modulates laser power proportional to acceleration (\$32=1). M3 uses constant laser power.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = useM4,
                onCheckedChange = { useM4 = it },
                colors = SwitchDefaults.colors(checkedThumbColor = LaserPrimary, checkedTrackColor = LaserPrimary.copy(alpha = 0.5f))
            )
        }

        // Query Firmware $$ button
        OutlinedButton(
            onClick = onQueryGrblSettings,
            modifier = Modifier.fillMaxWidth().height(40.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Read Controller EEPROM Settings (\$\$)", fontSize = 12.sp)
        }

        // Save Button
        Button(
            onClick = {
                val newProfile = profile.copy(
                    name = name,
                    bedWidthMm = bedWidth.toFloatOrNull() ?: profile.bedWidthMm,
                    bedHeightMm = bedHeight.toFloatOrNull() ?: profile.bedHeightMm,
                    maxSpeedMmMin = maxSpeed.toFloatOrNull() ?: profile.maxSpeedMmMin,
                    rapidSpeedMmMin = rapidSpeed.toFloatOrNull() ?: profile.rapidSpeedMmMin,
                    laserMaxPwm = laserMaxPwm.toIntOrNull() ?: profile.laserMaxPwm,
                    useDynamicLaserModeM4 = useM4
                )
                onSaveProfile(newProfile)
            },
            colors = ButtonDefaults.buttonColors(containerColor = LaserPrimary, contentColor = Color.Black),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(46.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("SAVE MACHINE SETTINGS", fontWeight = FontWeight.Bold)
        }
    }
}
