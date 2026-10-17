package com.example.machine

import android.graphics.RectF
import com.example.model.MachineProfile

object SafetyManager {

    data class SafetyCheckResult(
        val isSafeToRun: Boolean,
        val issues: List<String>,
        val warnings: List<String>
    )

    fun validateJobSafety(
        bounds: RectF,
        profile: MachineProfile,
        isLaserCoverClosed: Boolean = true
    ): SafetyCheckResult {
        val issues = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // 1. Boundary checking against machine bed
        if (bounds.left < 0f) {
            issues.add("X minimum (%.1f mm) is outside the bed limit (0.0 mm)".format(bounds.left))
        }
        if (bounds.top < 0f) {
            issues.add("Y minimum (%.1f mm) is outside the bed limit (0.0 mm)".format(bounds.top))
        }
        if (bounds.right > profile.bedWidthMm) {
            issues.add("X maximum (%.1f mm) exceeds machine bed width (%.1f mm)".format(bounds.right, profile.bedWidthMm))
        }
        if (bounds.bottom > profile.bedHeightMm) {
            issues.add("Y maximum (%.1f mm) exceeds machine bed height (%.1f mm)".format(bounds.bottom, profile.bedHeightMm))
        }

        // 2. Unattended operation warning
        warnings.add("FIRE HAZARD: Never operate laser unattended. Keep a certified Class ABC or CO2 fire extinguisher nearby.")

        // 3. Eye protection and ventilation
        warnings.add("EYE PROTECTION: Wear laser safety goggles certified for your laser's specific optical wavelength.")
        warnings.add("VENTILATION: Ensure active exhaust ventilation or fume extraction is operational before burning.")

        return SafetyCheckResult(
            isSafeToRun = issues.isEmpty(),
            issues = issues,
            warnings = warnings
        )
    }

    val SAFETY_DISCLAIMER = """
        IMPORTANT SAFETY NOTICE:
        Laser cutting and engraving devices emit intense optical radiation capable of causing permanent blindness and fires.
        
        Software interlocks and limit checks DO NOT replace hardware emergency stop switches, physical flame sensors, or protective enclosures.
        
        - Never leave a running laser machine unattended.
        - Ensure your workspace is properly vented outside or equipped with an active HEPA/carbon filtration system.
        - Do not cut polyvinyl chloride (PVC), vinyl, or chlorinated plastics due to toxic chlorine gas release.
    """.trimIndent()
}
