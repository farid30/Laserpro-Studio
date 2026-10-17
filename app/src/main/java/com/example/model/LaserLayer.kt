package com.example.model

import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class LayerMode(val displayName: String) {
    CUT("Vector Cut"),
    ENGRAVE("Raster Engrave"),
    MARK("Vector Mark / Score")
}

data class LaserLayer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorHex: Long, // e.g. 0xFF00E5FF
    val mode: LayerMode = LayerMode.CUT,
    val speedMmMin: Float = 600f,
    val minPowerPercent: Float = 0f,
    val maxPowerPercent: Float = 80f,
    val passes: Int = 1,
    val lineIntervalMm: Float = 0.1f, // For raster engraving
    val airAssist: Boolean = true,
    val overscanMm: Float = 2.5f, // For raster engrave acceleration/deceleration
    val zStepMm: Float = 0f,
    val enabled: Boolean = true,
    val locked: Boolean = false
) {
    val composeColor: Color
        get() = Color(colorHex)

    companion object {
        val DEFAULT_LAYERS = listOf(
            LaserLayer(
                id = "C00",
                name = "C00 - Cut",
                colorHex = 0xFFFF3D00, // Vibrant Red-Orange
                mode = LayerMode.CUT,
                speedMmMin = 300f,
                maxPowerPercent = 90f,
                passes = 2,
                airAssist = true
            ),
            LaserLayer(
                id = "C01",
                name = "C01 - Engrave",
                colorHex = 0xFF00E5FF, // Cyan
                mode = LayerMode.ENGRAVE,
                speedMmMin = 3000f,
                maxPowerPercent = 45f,
                passes = 1,
                lineIntervalMm = 0.1f,
                airAssist = false
            ),
            LaserLayer(
                id = "C02",
                name = "C02 - Mark",
                colorHex = 0xFF76FF03, // Lime Green
                mode = LayerMode.MARK,
                speedMmMin = 1500f,
                maxPowerPercent = 30f,
                passes = 1,
                airAssist = false
            ),
            LaserLayer(
                id = "C03",
                name = "C03 - Detail",
                colorHex = 0xFFE040FB, // Magenta
                mode = LayerMode.CUT,
                speedMmMin = 500f,
                maxPowerPercent = 70f,
                passes = 1,
                airAssist = true
            )
        )
    }
}
