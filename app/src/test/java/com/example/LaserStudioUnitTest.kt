package com.example

import com.example.gcode.GCodeGenerator
import com.example.machine.SafetyManager
import com.example.model.LaserLayer
import com.example.model.LaserShape
import com.example.model.MachineProfile
import com.example.model.Point2D
import com.example.model.ProjectFileManager
import com.example.parser.DxfImporter
import com.example.parser.SvgImporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LaserStudioUnitTest {

    @Test
    fun testGCodeGeneration_headerAndCommands() {
        val profile = MachineProfile(bedWidthMm = 400f, bedHeightMm = 400f, useDynamicLaserModeM4 = true)
        val layer = LaserLayer(
            id = "test_layer",
            name = "Cut",
            colorHex = 0xFFFF0000,
            speedMmMin = 500f,
            maxPowerPercent = 80f,
            passes = 1,
            airAssist = true
        )
        val rect = LaserShape.Rectangle(
            layerId = "test_layer",
            name = "TestBox",
            x = 10f,
            y = 10f,
            width = 50f,
            height = 30f
        )

        val job = GCodeGenerator.generateJob(listOf(rect), listOf(layer), profile)

        assertTrue("G-code should contain G21 millimeter mode", job.rawGCode.contains("G21"))
        assertTrue("G-code should contain G90 absolute mode", job.rawGCode.contains("G90"))
        assertTrue("G-code should contain M4 dynamic laser mode", job.rawGCode.contains("M4 S0"))
        assertTrue("G-code should contain air assist M8", job.rawGCode.contains("M8"))
        assertTrue("G-code should contain G1 cutting move", job.rawGCode.contains("G1"))
        assertTrue("G-code should contain M5 laser off at end", job.rawGCode.contains("M5 S0"))
        assertTrue("Job should be within machine bed limits", job.isWithinLimits)
        assertTrue("Job total distance should be greater than 0", job.totalDistanceMm > 0f)
    }

    @Test
    fun testGCodeGeneration_detectsOutOfBounds() {
        val profile = MachineProfile(bedWidthMm = 200f, bedHeightMm = 200f)
        val layer = LaserLayer(id = "layer1", name = "Cut", colorHex = 0xFFFF0000)
        // Rect extends past bed width: 180 + 50 = 230mm > 200mm
        val rect = LaserShape.Rectangle(
            layerId = "layer1",
            name = "OversizedBox",
            x = 180f,
            y = 10f,
            width = 50f,
            height = 20f
        )

        val job = GCodeGenerator.generateJob(listOf(rect), listOf(layer), profile)
        assertFalse("Job should flag out of bounds when geometry extends past bed width", job.isWithinLimits)
        assertNotNull("Warning message should be present", job.outOfBoundsWarning)
    }

    @Test
    fun testSvgImporter_parsesElements() {
        val svgXml = """
            <svg xmlns="http://www.w3.org/2000/svg" width="100mm" height="100mm" viewBox="0 0 100 100">
              <rect x="10" y="10" width="40" height="30" />
              <circle cx="50" cy="50" r="15" />
              <line x1="5" y1="5" x2="25" y2="25" />
              <path d="M 10 10 L 20 20 L 30 10 Z" />
            </svg>
        """.trimIndent()

        val result = SvgImporter.parseSvgString(svgXml, "default_layer")
        assertTrue("Should parse at least 4 shapes from SVG", result.shapes.size >= 4)
        assertTrue("Should contain a rectangle", result.shapes.any { it is LaserShape.Rectangle })
        assertTrue("Should contain a circle", result.shapes.any { it is LaserShape.Circle })
        assertTrue("Should contain a line", result.shapes.any { it is LaserShape.Line })
    }

    @Test
    fun testDxfImporter_parsesCadLinesAndCircles() {
        val dxfData = """
            0
            SECTION
            2
            ENTITIES
            0
            LINE
            10
            10.0
            20
            10.0
            11
            60.0
            21
            10.0
            0
            CIRCLE
            10
            50.0
            20
            50.0
            40
            12.5
            0
            ENDSEC
            0
            EOF
        """.trimIndent()

        val result = DxfImporter.parseDxfString(dxfData, "default_layer")
        assertEquals(2, result.entityCount)
        assertTrue("First element should be a line", result.shapes[0] is LaserShape.Line)
        val circle = result.shapes[1] as LaserShape.Circle
        assertEquals(12.5f, circle.radius, 0.01f)
    }

    @Test
    fun testProjectFileManager_serializeAndDeserialize() {
        val profile = MachineProfile(bedWidthMm = 300f, bedHeightMm = 300f)
        val layer = LaserLayer(id = "c01", name = "Cut Layer", colorHex = 0xFFFF3D00, speedMmMin = 400f)
        val shapes = listOf(
            LaserShape.Rectangle(id = "r1", layerId = "c01", name = "TestRect", x = 15f, y = 25f, width = 60f, height = 40f),
            LaserShape.Circle(id = "c1", layerId = "c01", name = "TestCircle", centerX = 50f, centerY = 50f, radius = 20f)
        )

        val json = ProjectFileManager.serializeProject("Test Project", shapes, listOf(layer), profile)
        assertNotNull(json)

        val loaded = ProjectFileManager.deserializeProject(json)
        assertEquals("Test Project", loaded.name)
        assertEquals(2, loaded.shapes.size)
        assertTrue(loaded.shapes.any { it is LaserShape.Rectangle })
        assertTrue(loaded.shapes.any { it is LaserShape.Circle })
    }

    @Test
    fun testSafetyManager_travelLimitChecking() {
        val profile = MachineProfile(bedWidthMm = 400f, bedHeightMm = 400f)
        val safeBounds = android.graphics.RectF(10f, 10f, 350f, 350f)
        val resultSafe = SafetyManager.validateJobSafety(safeBounds, profile)
        assertTrue("Safe bounds should pass", resultSafe.isSafeToRun)

        val unsafeBounds = android.graphics.RectF(-5f, 10f, 450f, 200f)
        val resultUnsafe = SafetyManager.validateJobSafety(unsafeBounds, profile)
        assertFalse("Out of bounds should fail safety check", resultUnsafe.isSafeToRun)
        assertTrue(resultUnsafe.issues.isNotEmpty())
    }
}
