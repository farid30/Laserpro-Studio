package com.example.ui.components

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
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WindPower
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.machine.SafetyManager
import com.example.ui.theme.LaserBorder
import com.example.ui.theme.LaserEmergencyRed
import com.example.ui.theme.LaserPrimary
import com.example.ui.theme.LaserSurface
import com.example.ui.theme.LaserSurfaceElevated
import com.example.ui.theme.LaserWarningAmber

@Composable
fun SafetyNoticeDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            color = LaserSurface,
            border = androidx.compose.foundation.BorderStroke(1.5f.dp, LaserEmergencyRed)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = LaserEmergencyRed, modifier = Modifier.size(24.dp))
                    Text(
                        "CNC Laser Operational Safety",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LaserEmergencyRed
                    )
                }

                HorizontalDivider(color = LaserBorder)

                SafetyPoint(
                    Icons.Default.LocalFireDepartment,
                    "Never Leave Machine Unattended",
                    "Laser cutting focuses immense energy into combustible materials. Fires can ignite within milliseconds. Always remain beside the machine with an extinguisher."
                )

                SafetyPoint(
                    Icons.Default.RemoveRedEye,
                    "Certified Eye Protection",
                    "Always wear OD6+ laser safety glasses rated specifically for your laser wavelength (e.g., 450nm Blue Diode, 1064nm Fiber, or 10.6um CO2). Never look directly into the beam or reflected scatter."
                )

                SafetyPoint(
                    Icons.Default.WindPower,
                    "Exhaust & Toxic Fume Extraction",
                    "Never cut materials containing PVC, vinyl, or synthetic chlorine compounds, which produce lethal chlorine gas and corrosive hydrochloric acid. Operate in well-ventilated areas."
                )

                SafetyPoint(
                    Icons.Default.Security,
                    "Hardware vs Software Limits",
                    "LaserPro Studio travel bounds and pause controls do NOT substitute for hardware emergency stop mushroom buttons, physical limit switches, or enclosure interlocks."
                )

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = LaserPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text("I Understand & Accept Safety Requirements", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SafetyPoint(icon: ImageVector, title: String, description: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = LaserSurfaceElevated),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = LaserWarningAmber, modifier = Modifier.size(22.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
