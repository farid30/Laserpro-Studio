package com.example.model

enum class ToolpathType {
    RAPID,   // G0: Laser off rapid positioning
    CUT,     // G1/G2/G3: Laser active cutting/marking
    ENGRAVE  // G1: Laser raster scanline
}

data class ToolpathSegment(
    val type: ToolpathType,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val powerPercent: Float,
    val feedRateMmMin: Float,
    val layerColorHex: Long = 0xFF00E5FF
) {
    val lengthMm: Float
        get() {
            val dx = endX - startX
            val dy = endY - startY
            return kotlin.math.sqrt(dx * dx + dy * dy)
        }
}

data class GCodeJob(
    val rawGCode: String,
    val segments: List<ToolpathSegment>,
    val totalDistanceMm: Float,
    val cuttingDistanceMm: Float,
    val rapidDistanceMm: Float,
    val estimatedSeconds: Float,
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float,
    val isWithinLimits: Boolean,
    val outOfBoundsWarning: String? = null
) {
    val estimatedTimeFormatted: String
        get() {
            val totalSec = estimatedSeconds.toInt()
            val minutes = totalSec / 60
            val seconds = totalSec % 60
            return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
        }
}
