package com.example.parser

import com.example.model.LaserShape
import com.example.model.Point2D
import java.io.InputStream
import kotlin.math.cos
import kotlin.math.sin

object DxfImporter {

    data class DxfImportResult(
        val shapes: List<LaserShape>,
        val entityCount: Int,
        val widthMm: Float,
        val heightMm: Float,
        val description: String
    )

    fun parseDxf(inputStream: InputStream, defaultLayerId: String): DxfImportResult {
        val lines = inputStream.bufferedReader().readLines()
        return parseDxfLines(lines, defaultLayerId)
    }

    fun parseDxfString(dxfContent: String, defaultLayerId: String): DxfImportResult {
        val lines = dxfContent.lines()
        return parseDxfLines(lines, defaultLayerId)
    }

    private fun parseDxfLines(rawLines: List<String>, defaultLayerId: String): DxfImportResult {
        val shapes = mutableListOf<LaserShape>()
        val lines = rawLines.map { it.trim() }

        var inEntities = false
        var i = 0

        while (i < lines.size - 1) {
            val code = lines[i].toIntOrNull() ?: -1
            val value = lines[i + 1]

            if (code == 0 && value.equals("SECTION", ignoreCase = true)) {
                if (i + 3 < lines.size && lines[i + 2] == "2" && lines[i + 3].equals("ENTITIES", ignoreCase = true)) {
                    inEntities = true
                    i += 4
                    continue
                }
            } else if (code == 0 && value.equals("ENDSEC", ignoreCase = true)) {
                inEntities = false
            }

            if (inEntities && code == 0) {
                val entityType = value.uppercase()
                i += 2

                // Collect key-value pairs until the next 0 code
                val entityData = mutableListOf<Pair<Int, String>>()
                while (i < lines.size - 1 && (lines[i].toIntOrNull() ?: -1) != 0) {
                    val c = lines[i].toIntOrNull() ?: -1
                    val v = lines[i + 1]
                    entityData.add(Pair(c, v))
                    i += 2
                }

                when (entityType) {
                    "LINE" -> {
                        var x1 = 0f
                        var y1 = 0f
                        var x2 = 0f
                        var y2 = 0f
                        for ((c, v) in entityData) {
                            when (c) {
                                10 -> x1 = v.toFloatOrNull() ?: 0f
                                20 -> y1 = v.toFloatOrNull() ?: 0f
                                11 -> x2 = v.toFloatOrNull() ?: 0f
                                21 -> y2 = v.toFloatOrNull() ?: 0f
                            }
                        }
                        shapes.add(LaserShape.Line(layerId = defaultLayerId, name = "DXF Line", x1 = x1, y1 = y1, x2 = x2, y2 = y2))
                    }
                    "CIRCLE" -> {
                        var cx = 0f
                        var cy = 0f
                        var r = 0f
                        for ((c, v) in entityData) {
                            when (c) {
                                10 -> cx = v.toFloatOrNull() ?: 0f
                                20 -> cy = v.toFloatOrNull() ?: 0f
                                40 -> r = v.toFloatOrNull() ?: 0f
                            }
                        }
                        if (r > 0f) {
                            shapes.add(LaserShape.Circle(layerId = defaultLayerId, name = "DXF Circle", centerX = cx, centerY = cy, radius = r))
                        }
                    }
                    "ARC" -> {
                        var cx = 0f
                        var cy = 0f
                        var r = 0f
                        var startAngle = 0f
                        var endAngle = 360f
                        for ((c, v) in entityData) {
                            when (c) {
                                10 -> cx = v.toFloatOrNull() ?: 0f
                                20 -> cy = v.toFloatOrNull() ?: 0f
                                40 -> r = v.toFloatOrNull() ?: 0f
                                50 -> startAngle = v.toFloatOrNull() ?: 0f
                                51 -> endAngle = v.toFloatOrNull() ?: 360f
                            }
                        }
                        if (r > 0f) {
                            // Convert arc to polygon segments
                            var sweep = endAngle - startAngle
                            if (sweep < 0) sweep += 360f
                            val steps = maxOf(4, (sweep / 15f).toInt())
                            val arcPts = mutableListOf<Point2D>()
                            for (s in 0..steps) {
                                val deg = startAngle + (sweep * (s.toFloat() / steps))
                                val rad = Math.toRadians(deg.toDouble())
                                arcPts.add(Point2D((cx + r * cos(rad)).toFloat(), (cy + r * sin(rad)).toFloat()))
                            }
                            shapes.add(LaserShape.Polygon(layerId = defaultLayerId, name = "DXF Arc", points = arcPts, isClosed = false))
                        }
                    }
                    "LWPOLYLINE" -> {
                        var isClosed = false
                        val points = mutableListOf<Point2D>()
                        var currentX: Float? = null
                        for ((c, v) in entityData) {
                            when (c) {
                                70 -> {
                                    val flags = v.toIntOrNull() ?: 0
                                    isClosed = (flags and 1) != 0
                                }
                                10 -> currentX = v.toFloatOrNull()
                                20 -> {
                                    val y = v.toFloatOrNull() ?: 0f
                                    if (currentX != null) {
                                        points.add(Point2D(currentX, y))
                                        currentX = null
                                    }
                                }
                            }
                        }
                        if (points.size >= 2) {
                            shapes.add(LaserShape.Polygon(layerId = defaultLayerId, name = "DXF Polyline", points = points, isClosed = isClosed))
                        }
                    }
                    "TEXT", "MTEXT" -> {
                        var x = 0f
                        var y = 0f
                        var text = ""
                        var height = 10f
                        for ((c, v) in entityData) {
                            when (c) {
                                10 -> x = v.toFloatOrNull() ?: 0f
                                20 -> y = v.toFloatOrNull() ?: 0f
                                40 -> height = v.toFloatOrNull() ?: 10f
                                1 -> text = v
                            }
                        }
                        if (text.isNotEmpty()) {
                            shapes.add(LaserShape.Text(layerId = defaultLayerId, name = "DXF Text", text = text, x = x, y = y, heightMm = height))
                        }
                    }
                }
                continue
            }
            i += 2
        }

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (s in shapes) {
            val b = s.bounds()
            if (b.left < minX) minX = b.left
            if (b.top < minY) minY = b.top
            if (b.right > maxX) maxX = b.right
            if (b.bottom > maxY) maxY = b.bottom
        }
        val w = if (maxX > minX) maxX - minX else 0f
        val h = if (maxY > minY) maxY - minY else 0f

        return DxfImportResult(
            shapes = shapes,
            entityCount = shapes.size,
            widthMm = w,
            heightMm = h,
            description = "Imported ${shapes.size} DXF CAD entities (${String.format("%.1f", w)} x ${String.format("%.1f", h)} mm)"
        )
    }
}
