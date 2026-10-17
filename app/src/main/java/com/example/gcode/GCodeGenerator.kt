package com.example.gcode

import android.graphics.RectF
import com.example.model.GCodeJob
import com.example.model.LaserLayer
import com.example.model.LaserShape
import com.example.model.MachineProfile
import com.example.model.Point2D
import com.example.model.ToolpathSegment
import com.example.model.ToolpathType
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

object GCodeGenerator {

    fun generateJob(
        shapes: List<LaserShape>,
        layers: List<LaserLayer>,
        profile: MachineProfile
    ): GCodeJob {
        val segments = mutableListOf<ToolpathSegment>()
        val gcode = StringBuilder()

        gcode.append("; ==============================================\n")
        gcode.append("; LaserPro Studio - Precision CNC G-Code\n")
        gcode.append("; Machine: ${profile.name}\n")
        gcode.append("; Bed Size: ${profile.bedWidthMm} x ${profile.bedHeightMm} mm\n")
        gcode.append("; ==============================================\n")
        gcode.append("G21 ; Millimeter units\n")
        gcode.append("G90 ; Absolute coordinates\n")
        val laserModeCmd = if (profile.useDynamicLaserModeM4) "M4" else "M3"
        gcode.append("$laserModeCmd S0 ; Dynamic laser control initialized (OFF)\n\n")

        var currentX = 0f
        var currentY = 0f
        var totalCutDistance = 0f
        var totalRapidDistance = 0f
        var estimatedSeconds = 0f

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        val layerMap = layers.associateBy { it.id }

        // Process layers in order
        for (layer in layers) {
            if (!layer.enabled) continue
            val layerShapes = shapes.filter { it.layerId == layer.id }
            if (layerShapes.isEmpty()) continue

            gcode.append("; ----------------------------------------------\n")
            gcode.append("; Layer: ${layer.name} | Mode: ${layer.mode.displayName}\n")
            gcode.append("; Speed: ${layer.speedMmMin} mm/min | Power: ${layer.maxPowerPercent}% | Passes: ${layer.passes}\n")
            gcode.append("; ----------------------------------------------\n")

            if (layer.airAssist) {
                gcode.append("M8 ; Air assist ON\n")
            }

            val maxS = (layer.maxPowerPercent / 100f * profile.laserMaxPwm).toInt()
            val feedRate = layer.speedMmMin

            // Convert shapes to polyline toolpaths
            val rawPaths = mutableListOf<List<Point2D>>()
            for (shape in layerShapes) {
                rawPaths.addAll(shapeToPolylines(shape, layer))
            }

            // Optimize toolpath: simple nearest-neighbor TSP to minimize travel moves
            val orderedPaths = optimizePaths(rawPaths, currentX, currentY)

            for (pass in 1..layer.passes) {
                if (layer.passes > 1) {
                    gcode.append("; Pass $pass of ${layer.passes}\n")
                }

                for (path in orderedPaths) {
                    if (path.isEmpty()) continue

                    val start = path.first()

                    // Rapid move to start of shape (laser off)
                    if (kotlin.math.abs(currentX - start.x) > 0.001f || kotlin.math.abs(currentY - start.y) > 0.001f) {
                        val rapidDist = distance(currentX, currentY, start.x, start.y)
                        totalRapidDistance += rapidDist
                        estimatedSeconds += (rapidDist / profile.rapidSpeedMmMin) * 60f

                        segments.add(
                            ToolpathSegment(
                                type = ToolpathType.RAPID,
                                startX = currentX,
                                startY = currentY,
                                endX = start.x,
                                endY = start.y,
                                powerPercent = 0f,
                                feedRateMmMin = profile.rapidSpeedMmMin,
                                layerColorHex = 0xFF555555
                            )
                        )

                        gcode.append(
                            String.format(
                                Locale.US,
                                "G0 X%.3f Y%.3f F%.0f\n",
                                start.x,
                                start.y,
                                profile.rapidSpeedMmMin
                            )
                        )
                        currentX = start.x
                        currentY = start.y
                    }

                    // Cut along path
                    for (i in 1 until path.size) {
                        val pt = path[i]
                        val cutDist = distance(currentX, currentY, pt.x, pt.y)
                        totalCutDistance += cutDist
                        estimatedSeconds += (cutDist / feedRate) * 60f

                        minX = min(minX, min(currentX, pt.x))
                        minY = min(minY, min(currentY, pt.y))
                        maxX = max(maxX, max(currentX, pt.x))
                        maxY = max(maxY, max(currentY, pt.y))

                        segments.add(
                            ToolpathSegment(
                                type = if (layer.mode == com.example.model.LayerMode.ENGRAVE) ToolpathType.ENGRAVE else ToolpathType.CUT,
                                startX = currentX,
                                startY = currentY,
                                endX = pt.x,
                                endY = pt.y,
                                powerPercent = layer.maxPowerPercent,
                                feedRateMmMin = feedRate,
                                layerColorHex = layer.colorHex
                            )
                        )

                        gcode.append(
                            String.format(
                                Locale.US,
                                "G1 X%.3f Y%.3f S%d F%.0f\n",
                                pt.x,
                                pt.y,
                                maxS,
                                feedRate
                            )
                        )
                        currentX = pt.x
                        currentY = pt.y
                    }

                    // Turn laser power to 0 at end of shape
                    gcode.append("G1 S0\n")
                }
            }

            if (layer.airAssist) {
                gcode.append("M9 ; Air assist OFF\n")
            }
            gcode.append("\n")
        }

        // Return to home & program end
        gcode.append("; ----------------------------------------------\n")
        gcode.append("; Job Completion\n")
        gcode.append("M5 S0 ; Laser OFF\n")
        gcode.append("M9 ; Air assist OFF\n")
        gcode.append(String.format(Locale.US, "G0 X0.000 Y0.000 F%.0f ; Return to origin\n", profile.rapidSpeedMmMin))
        gcode.append("M2 ; End of program\n")

        val isWithinLimits = if (minX < Float.MAX_VALUE) {
            minX >= 0f && minY >= 0f && maxX <= profile.bedWidthMm && maxY <= profile.bedHeightMm
        } else true

        val warning = if (!isWithinLimits) {
            "WARNING: Toolpath exceeds machine bed! Bounds: [X: %.1f..%.1f, Y: %.1f..%.1f] vs Bed: [0..%.1f, 0..%.1f] mm"
                .format(Locale.US, minX, maxX, minY, maxY, profile.bedWidthMm, profile.bedHeightMm)
        } else null

        return GCodeJob(
            rawGCode = gcode.toString(),
            segments = segments,
            totalDistanceMm = totalCutDistance + totalRapidDistance,
            cuttingDistanceMm = totalCutDistance,
            rapidDistanceMm = totalRapidDistance,
            estimatedSeconds = max(1f, estimatedSeconds),
            minX = if (minX < Float.MAX_VALUE) minX else 0f,
            minY = if (minY < Float.MAX_VALUE) minY else 0f,
            maxX = if (maxX > -Float.MAX_VALUE) maxX else 0f,
            maxY = if (maxY > -Float.MAX_VALUE) maxY else 0f,
            isWithinLimits = isWithinLimits,
            outOfBoundsWarning = warning
        )
    }

    private fun shapeToPolylines(shape: LaserShape, layer: LaserLayer): List<List<Point2D>> {
        val paths = mutableListOf<List<Point2D>>()
        when (shape) {
            is LaserShape.Rectangle -> {
                val pts = if (shape.cornerRadius <= 0.1f) {
                    listOf(
                        Point2D(shape.x, shape.y),
                        Point2D(shape.x + shape.width, shape.y),
                        Point2D(shape.x + shape.width, shape.y + shape.height),
                        Point2D(shape.x, shape.y + shape.height),
                        Point2D(shape.x, shape.y) // closed
                    )
                } else {
                    generateRoundedRectPoints(shape)
                }
                paths.add(pts)
            }
            is LaserShape.Circle -> {
                val pts = mutableListOf<Point2D>()
                val segments = maxOf(24, (shape.radius * 2).toInt())
                for (s in 0..segments) {
                    val angle = (s.toFloat() / segments) * 2.0 * Math.PI
                    val x = shape.centerX + (shape.radius * cos(angle)).toFloat()
                    val y = shape.centerY + (shape.radius * sin(angle)).toFloat()
                    pts.add(Point2D(x, y))
                }
                paths.add(pts)
            }
            is LaserShape.Ellipse -> {
                val pts = mutableListOf<Point2D>()
                val segments = maxOf(24, (maxOf(shape.radiusX, shape.radiusY) * 2).toInt())
                for (s in 0..segments) {
                    val angle = (s.toFloat() / segments) * 2.0 * Math.PI
                    val x = shape.centerX + (shape.radiusX * cos(angle)).toFloat()
                    val y = shape.centerY + (shape.radiusY * sin(angle)).toFloat()
                    pts.add(Point2D(x, y))
                }
                paths.add(pts)
            }
            is LaserShape.Line -> {
                paths.add(listOf(Point2D(shape.x1, shape.y1), Point2D(shape.x2, shape.y2)))
            }
            is LaserShape.Polygon -> {
                if (shape.points.size >= 2) {
                    val pts = shape.points.toMutableList()
                    if (shape.isClosed && pts.first() != pts.last()) {
                        pts.add(pts.first())
                    }
                    paths.add(pts)
                }
            }
            is LaserShape.VectorPath -> {
                for (sub in shape.subPaths) {
                    if (sub.size >= 2) {
                        val pts = sub.toMutableList()
                        if (shape.isClosed && pts.first() != pts.last()) {
                            pts.add(pts.first())
                        }
                        paths.add(pts)
                    }
                }
            }
            is LaserShape.Text -> {
                // Vector stroke approximation for text characters
                paths.addAll(generateTextStrokePolylines(shape))
            }
            is LaserShape.RasterImage -> {
                // Generate horizontal scanlines with line interval
                val interval = if (layer.lineIntervalMm > 0.02f) layer.lineIntervalMm else 0.1f
                val scanlineCount = max(2, (shape.height / interval).toInt())
                for (i in 0 until scanlineCount) {
                    val y = shape.y + (i * interval)
                    if (i % 2 == 0) {
                        paths.add(listOf(Point2D(shape.x, y), Point2D(shape.x + shape.width, y)))
                    } else {
                        paths.add(listOf(Point2D(shape.x + shape.width, y), Point2D(shape.x, y)))
                    }
                }
            }
        }
        return paths
    }

    private fun generateRoundedRectPoints(rect: LaserShape.Rectangle): List<Point2D> {
        val r = min(rect.cornerRadius, min(rect.width / 2f, rect.height / 2f))
        val pts = mutableListOf<Point2D>()
        val steps = 6

        // Top edge
        pts.add(Point2D(rect.x + r, rect.y))
        pts.add(Point2D(rect.x + rect.width - r, rect.y))

        // Top-right corner
        for (i in 1..steps) {
            val a = -Math.PI / 2 + (Math.PI / 2) * (i.toDouble() / steps)
            pts.add(Point2D(rect.x + rect.width - r + (r * cos(a)).toFloat(), rect.y + r + (r * sin(a)).toFloat()))
        }

        // Right edge
        pts.add(Point2D(rect.x + rect.width, rect.y + rect.height - r))

        // Bottom-right corner
        for (i in 1..steps) {
            val a = 0.0 + (Math.PI / 2) * (i.toDouble() / steps)
            pts.add(Point2D(rect.x + rect.width - r + (r * cos(a)).toFloat(), rect.y + rect.height - r + (r * sin(a)).toFloat()))
        }

        // Bottom edge
        pts.add(Point2D(rect.x + r, rect.y + rect.height))

        // Bottom-left corner
        for (i in 1..steps) {
            val a = Math.PI / 2 + (Math.PI / 2) * (i.toDouble() / steps)
            pts.add(Point2D(rect.x + r + (r * cos(a)).toFloat(), rect.y + rect.height - r + (r * sin(a)).toFloat()))
        }

        // Left edge
        pts.add(Point2D(rect.x, rect.y + r))

        // Top-left corner
        for (i in 1..steps) {
            val a = Math.PI + (Math.PI / 2) * (i.toDouble() / steps)
            pts.add(Point2D(rect.x + r + (r * cos(a)).toFloat(), rect.y + r + (r * sin(a)).toFloat()))
        }

        pts.add(Point2D(rect.x + r, rect.y)) // Close
        return pts
    }

    private fun generateTextStrokePolylines(text: LaserShape.Text): List<List<Point2D>> {
        val strokes = mutableListOf<List<Point2D>>()
        val charW = text.heightMm * 0.6f
        val h = text.heightMm
        val gap = text.heightMm * 0.2f

        var curX = text.x
        for (ch in text.text) {
            val cy = text.y
            when (ch.uppercaseChar()) {
                'A' -> {
                    strokes.add(listOf(Point2D(curX, cy + h), Point2D(curX + charW / 2, cy), Point2D(curX + charW, cy + h)))
                    strokes.add(listOf(Point2D(curX + charW * 0.25f, cy + h * 0.5f), Point2D(curX + charW * 0.75f, cy + h * 0.5f)))
                }
                'B' -> {
                    strokes.add(listOf(Point2D(curX, cy), Point2D(curX, cy + h)))
                    strokes.add(listOf(Point2D(curX, cy), Point2D(curX + charW * 0.7f, cy), Point2D(curX + charW, cy + h * 0.25f), Point2D(curX, cy + h * 0.5f)))
                    strokes.add(listOf(Point2D(curX, cy + h * 0.5f), Point2D(curX + charW * 0.7f, cy + h * 0.5f), Point2D(curX + charW, cy + h * 0.75f), Point2D(curX, cy + h)))
                }
                'C' -> {
                    strokes.add(listOf(Point2D(curX + charW, cy), Point2D(curX, cy), Point2D(curX, cy + h), Point2D(curX + charW, cy + h)))
                }
                'D' -> {
                    strokes.add(listOf(Point2D(curX, cy), Point2D(curX, cy + h)))
                    strokes.add(listOf(Point2D(curX, cy), Point2D(curX + charW * 0.7f, cy), Point2D(curX + charW, cy + h * 0.5f), Point2D(curX + charW * 0.7f, cy + h), Point2D(curX, cy + h)))
                }
                'E' -> {
                    strokes.add(listOf(Point2D(curX + charW, cy), Point2D(curX, cy), Point2D(curX, cy + h), Point2D(curX + charW, cy + h)))
                    strokes.add(listOf(Point2D(curX, cy + h * 0.5f), Point2D(curX + charW * 0.6f, cy + h * 0.5f)))
                }
                'H' -> {
                    strokes.add(listOf(Point2D(curX, cy), Point2D(curX, cy + h)))
                    strokes.add(listOf(Point2D(curX + charW, cy), Point2D(curX + charW, cy + h)))
                    strokes.add(listOf(Point2D(curX, cy + h * 0.5f), Point2D(curX + charW, cy + h * 0.5f)))
                }
                'L' -> {
                    strokes.add(listOf(Point2D(curX, cy), Point2D(curX, cy + h), Point2D(curX + charW, cy + h)))
                }
                'O', '0' -> {
                    strokes.add(listOf(Point2D(curX, cy), Point2D(curX + charW, cy), Point2D(curX + charW, cy + h), Point2D(curX, cy + h), Point2D(curX, cy)))
                }
                'P' -> {
                    strokes.add(listOf(Point2D(curX, cy + h), Point2D(curX, cy), Point2D(curX + charW, cy), Point2D(curX + charW, cy + h * 0.5f), Point2D(curX, cy + h * 0.5f)))
                }
                'R' -> {
                    strokes.add(listOf(Point2D(curX, cy + h), Point2D(curX, cy), Point2D(curX + charW, cy), Point2D(curX + charW, cy + h * 0.5f), Point2D(curX, cy + h * 0.5f), Point2D(curX + charW, cy + h)))
                }
                'S' -> {
                    strokes.add(listOf(Point2D(curX + charW, cy), Point2D(curX, cy), Point2D(curX, cy + h * 0.5f), Point2D(curX + charW, cy + h * 0.5f), Point2D(curX + charW, cy + h), Point2D(curX, cy + h)))
                }
                'T' -> {
                    strokes.add(listOf(Point2D(curX, cy), Point2D(curX + charW, cy)))
                    strokes.add(listOf(Point2D(curX + charW / 2, cy), Point2D(curX + charW / 2, cy + h)))
                }
                'X' -> {
                    strokes.add(listOf(Point2D(curX, cy), Point2D(curX + charW, cy + h)))
                    strokes.add(listOf(Point2D(curX + charW, cy), Point2D(curX, cy + h)))
                }
                else -> {
                    // Default fallback stroke: clean rectangular outline
                    strokes.add(listOf(Point2D(curX, cy), Point2D(curX + charW, cy), Point2D(curX + charW, cy + h), Point2D(curX, cy + h), Point2D(curX, cy)))
                }
            }
            curX += charW + gap
        }
        return strokes
    }

    private fun optimizePaths(paths: List<List<Point2D>>, startX: Float, startY: Float): List<List<Point2D>> {
        if (paths.size <= 1) return paths
        val remaining = paths.toMutableList()
        val ordered = mutableListOf<List<Point2D>>()
        var curX = startX
        var curY = startY

        while (remaining.isNotEmpty()) {
            var bestIdx = 0
            var bestDist = Float.MAX_VALUE
            var shouldReverse = false

            for (i in remaining.indices) {
                val p = remaining[i]
                if (p.isEmpty()) continue
                val dStart = distance(curX, curY, p.first().x, p.first().y)
                val dEnd = distance(curX, curY, p.last().x, p.last().y)

                if (dStart < bestDist) {
                    bestDist = dStart
                    bestIdx = i
                    shouldReverse = false
                }
                if (dEnd < bestDist) {
                    bestDist = dEnd
                    bestIdx = i
                    shouldReverse = true
                }
            }

            val chosen = remaining.removeAt(bestIdx)
            val finalPath = if (shouldReverse) chosen.reversed() else chosen
            ordered.add(finalPath)
            curX = finalPath.last().x
            curY = finalPath.last().y
        }

        return ordered
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }
}
