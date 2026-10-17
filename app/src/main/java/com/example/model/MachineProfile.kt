package com.example.model

enum class OriginCorner(val displayName: String) {
    BOTTOM_LEFT("Bottom-Left (Standard GRBL)"),
    TOP_LEFT("Top-Left"),
    BOTTOM_RIGHT("Bottom-Right"),
    TOP_RIGHT("Top-Right"),
    CENTER("Center")
}

data class MachineProfile(
    val name: String = "Standard 4040 Laser Engraver",
    val bedWidthMm: Float = 400f,
    val bedHeightMm: Float = 400f,
    val maxSpeedMmMin: Float = 10000f,
    val rapidSpeedMmMin: Float = 8000f,
    val accelerationMmSec2: Float = 500f,
    val laserMaxPwm: Int = 1000, // $30 setting in GRBL
    val laserMinPwm: Int = 0,
    val useDynamicLaserModeM4: Boolean = true, // M4 for GRBL laser mode ($32=1)
    val originCorner: OriginCorner = OriginCorner.BOTTOM_LEFT,
    val baudRate: Int = 115200,
    val invertX: Boolean = false,
    val invertY: Boolean = false,
    val softLimitsEnabled: Boolean = true
) {
    companion object {
        val DEFAULT = MachineProfile()
        val DIODE_COMPACT = MachineProfile(
            name = "Compact Diode 3018 (300x180mm)",
            bedWidthMm = 300f,
            bedHeightMm = 180f,
            maxSpeedMmMin = 4000f,
            rapidSpeedMmMin = 4000f,
            laserMaxPwm = 1000
        )
        val CO2_INDUSTRIAL = MachineProfile(
            name = "CO2 Pro Flatbed (600x400mm)",
            bedWidthMm = 600f,
            bedHeightMm = 400f,
            maxSpeedMmMin = 24000f,
            rapidSpeedMmMin = 15000f,
            laserMaxPwm = 1000,
            useDynamicLaserModeM4 = true
        )
    }
}
