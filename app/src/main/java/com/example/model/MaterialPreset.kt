package com.example.model

import java.util.UUID

data class MaterialPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String, // "Wood", "Plastics", "Leather", "Paper/Card", "Metal"
    val thicknessMm: Float,
    val recommendedMode: LayerMode,
    val speedMmMin: Float,
    val minPowerPercent: Float,
    val maxPowerPercent: Float,
    val passes: Int,
    val airAssist: Boolean,
    val lineIntervalMm: Float = 0.1f,
    val notes: String
) {
    companion object {
        val DEFAULT_PRESETS = listOf(
            MaterialPreset(
                name = "3mm Basswood Plywood - Vector Cut",
                category = "Wood",
                thicknessMm = 3.0f,
                recommendedMode = LayerMode.CUT,
                speedMmMin = 250f,
                minPowerPercent = 0f,
                maxPowerPercent = 90f,
                passes = 2,
                airAssist = true,
                notes = "High-pressure air assist prevents charring. Two passes ensure clean edge cut."
            ),
            MaterialPreset(
                name = "3mm Basswood Plywood - Engrave",
                category = "Wood",
                thicknessMm = 3.0f,
                recommendedMode = LayerMode.ENGRAVE,
                speedMmMin = 3000f,
                minPowerPercent = 5f,
                maxPowerPercent = 40f,
                passes = 1,
                airAssist = false,
                lineIntervalMm = 0.08f,
                notes = "Low air assist prevents smoke blowout across wood grain."
            ),
            MaterialPreset(
                name = "3mm MDF Board - Vector Cut",
                category = "Wood",
                thicknessMm = 3.0f,
                recommendedMode = LayerMode.CUT,
                speedMmMin = 220f,
                minPowerPercent = 0f,
                maxPowerPercent = 95f,
                passes = 2,
                airAssist = true,
                notes = "Dense resin requires slower cutting feed and strong air assist."
            ),
            MaterialPreset(
                name = "3mm Cast Acrylic - Vector Cut",
                category = "Plastics",
                thicknessMm = 3.0f,
                recommendedMode = LayerMode.CUT,
                speedMmMin = 180f,
                minPowerPercent = 0f,
                maxPowerPercent = 85f,
                passes = 1,
                airAssist = true,
                notes = "Smooth polished edge. Leave protective film on underside."
            ),
            MaterialPreset(
                name = "3mm Cast Acrylic - Frosted Engrave",
                category = "Plastics",
                thicknessMm = 3.0f,
                recommendedMode = LayerMode.ENGRAVE,
                speedMmMin = 3500f,
                minPowerPercent = 0f,
                maxPowerPercent = 30f,
                passes = 1,
                airAssist = false,
                lineIntervalMm = 0.1f,
                notes = "Yields a crisp frosted white appearance on clear or black acrylic."
            ),
            MaterialPreset(
                name = "1.5mm Veg-Tan Leather - Cut",
                category = "Leather",
                thicknessMm = 1.5f,
                recommendedMode = LayerMode.CUT,
                speedMmMin = 450f,
                minPowerPercent = 0f,
                maxPowerPercent = 75f,
                passes = 1,
                airAssist = true,
                notes = "Use honey-comb bed pins to prevent curling from moisture loss."
            ),
            MaterialPreset(
                name = "1.5mm Veg-Tan Leather - Detailed Branding",
                category = "Leather",
                thicknessMm = 1.5f,
                recommendedMode = LayerMode.ENGRAVE,
                speedMmMin = 2800f,
                minPowerPercent = 0f,
                maxPowerPercent = 35f,
                passes = 1,
                airAssist = false,
                lineIntervalMm = 0.1f,
                notes = "Rich caramel burn mark. Clean soot with damp cloth."
            ),
            MaterialPreset(
                name = "Heavy Kraft Cardstock - Precision Cut",
                category = "Paper/Card",
                thicknessMm = 0.5f,
                recommendedMode = LayerMode.CUT,
                speedMmMin = 1200f,
                minPowerPercent = 0f,
                maxPowerPercent = 35f,
                passes = 1,
                airAssist = true,
                notes = "Fast cut to avoid burning delicate paper edges."
            ),
            MaterialPreset(
                name = "Corrugated Cardboard - Cut",
                category = "Paper/Card",
                thicknessMm = 4.0f,
                recommendedMode = LayerMode.CUT,
                speedMmMin = 600f,
                minPowerPercent = 0f,
                maxPowerPercent = 60f,
                passes = 1,
                airAssist = true,
                notes = "Never leave unattended! Flammable material requires continuous supervision."
            ),
            MaterialPreset(
                name = "Anodized Aluminum - Surface Marking",
                category = "Metal",
                thicknessMm = 1.0f,
                recommendedMode = LayerMode.MARK,
                speedMmMin = 1000f,
                minPowerPercent = 0f,
                maxPowerPercent = 90f,
                passes = 1,
                airAssist = false,
                lineIntervalMm = 0.05f,
                notes = "Bleaches anodic dye layer to bright white. Does not vaporize bulk aluminum."
            )
        )
    }
}
