package com.example.parser

import android.graphics.RectF
import com.example.model.LaserShape
import com.example.model.Point2D
import java.io.InputStream
import java.util.regex.Pattern
import kotlin.math.cos
import kotlin.math.sin

object SvgImporter {

    data class SvgImportResult(
        val shapes: List<LaserShape>,
        val widthMm: Float,
        val heightMm: Float,
        val description: String
    )

    fun parseSvg(inputStream: InputStream, defaultLayerId: String): SvgImportResult {
        val content = inputStream.bufferedReader().use { it.readText() }
        return parseSvgString(content, defaultLayerId)
    }

    fun parseSvgString(svgContent: String, defaultLayerId: String): SvgImportResult {
        val shapes = mutableListOf<LaserShape>()

        // 1. Determine viewBox and dimension units
        var viewBoxWidth = 100f
        var viewBoxHeight = 100f

        val vbMatch = Regex("""viewBox=["']\s*([-\d.]+)\s+([-\d.]+)\s+([-\d.]+)\s+([-\d.]+)\s*["']""").find(svgContent)
        if (vbMatch != null) {
            viewBoxWidth = vbMatch.groupValues[3].toFloatOrNull() ?: 100f
            viewBoxHeight = vbMatch.groupValues[4].toFloatOrNull() ?: 100f
        } else {
            val wMatch = Regex("""width=["']([\d.]+)(mm|px|pt|in)?["']""").find(svgContent)
            val hMatch = Regex("""height=["']([\d.]+)(mm|px|pt|in)?["']""").find(svgContent)
            if (wMatch != null) viewBoxWidth = wMatch.groupValues[1].toFloatOrNull() ?: 100f
            if (hMatch != null) viewBoxHeight = hMatch.groupValues[1].toFloatOrNull() ?: 100f
        }

        // Scale factor to convert pixels or points to millimeters (assuming 96 DPI: 1 inch = 25.4mm, 1 px = 0.264583 mm)
        // If viewBox is large (> 200), apply 0.264583f scaling factor if units are pixels
        val unitScale = if (svgContent.contains("mm")) 1.0f else if (viewBoxWidth > 1000f) 0.1f else 0.264583f
        val scale = if (viewBoxWidth > 400f && !svgContent.contains("mm")) 0.264583f else 1.0f

        // 2. Parse Rectangles
        val rectRegex = Regex("""<rect\s+([^>]+)/>""", RegexOption.IGNORE_CASE)
        for (match in rectRegex.findAll(svgContent)) {
            val attrs = match.groupValues[1]
            val x = extractAttr(attrs, "x") * scale
            val y = extractAttr(attrs, "y") * scale
            val w = extractAttr(attrs, "width") * scale
            val h = extractAttr(attrs, "height") * scale
            val rx = extractAttr(attrs, "rx") * scale
            if (w > 0 && h > 0) {
                shapes.add(
                    LaserShape.Rectangle(
                        layerId = defaultLayerId,
                        name = "SVG Rect",
                        x = x,
                        y = y,
                        width = w,
                        height = h,
                        cornerRadius = rx
                    )
                )
            }
        }

        // 3. Parse Circles
        val circleRegex = Regex("""<circle\s+([^>]+)/>""", RegexOption.IGNORE_CASE)
        for (match in circleRegex.findAll(svgContent)) {
            val attrs = match.groupValues[1]
            val cx = extractAttr(attrs, "cx") * scale
            val cy = extractAttr(attrs, "cy") * scale
            val r = extractAttr(attrs, "r") * scale
            if (r > 0) {
                shapes.add(
                    LaserShape.Circle(
                        layerId = defaultLayerId,
                        name = "SVG Circle",
                        centerX = cx,
                        centerY = cy,
                        radius = r
                    )
                )
            }
        }

        // 4. Parse Ellipses
        val ellipseRegex = Regex("""<ellipse\s+([^>]+)/>""", RegexOption.IGNORE_CASE)
        for (match in ellipseRegex.findAll(svgContent)) {
            val attrs = match.groupValues[1]
            val cx = extractAttr(attrs, "cx") * scale
            val cy = extractAttr(attrs, "cy") * scale
            val rx = extractAttr(attrs, "rx") * scale
            val ry = extractAttr(attrs, "ry") * scale
            if (rx > 0 && ry > 0) {
                shapes.add(
                    LaserShape.Ellipse(
                        layerId = defaultLayerId,
                        name = "SVG Ellipse",
                        centerX = cx,
                        centerY = cy,
                        radiusX = rx,
                        radiusY = ry
                    )
                )
            }
        }

        // 5. Parse Lines
        val lineRegex = Regex("""<line\s+([^>]+)/>""", RegexOption.IGNORE_CASE)
        for (match in lineRegex.findAll(svgContent)) {
            val attrs = match.groupValues[1]
            val x1 = extractAttr(attrs, "x1") * scale
            val y1 = extractAttr(attrs, "y1") * scale
            val x2 = extractAttr(attrs, "x2") * scale
            val y2 = extractAttr(attrs, "y2") * scale
            shapes.add(
                LaserShape.Line(
                    layerId = defaultLayerId,
                    name = "SVG Line",
                    x1 = x1,
                    y1 = y1,
                    x2 = x2,
                    y2 = y2
                )
            )
        }

        // 6. Parse Polygons & Polylines
        val polyRegex = Regex("""<(polygon|polyline)\s+([^>]+)/>""", RegexOption.IGNORE_CASE)
        for (match in polyRegex.findAll(svgContent)) {
            val tag = match.groupValues[1].lowercase()
            val attrs = match.groupValues[2]
            val pointsStr = extractStringAttr(attrs, "points")
            if (pointsStr.isNotEmpty()) {
                val pts = parsePointsList(pointsStr, scale)
                if (pts.size >= 2) {
                    shapes.add(
                        LaserShape.Polygon(
                            layerId = defaultLayerId,
                            name = if (tag == "polygon") "SVG Polygon" else "SVG Polyline",
                            points = pts,
                            isClosed = (tag == "polygon")
                        )
                    )
                }
            }
        }

        // 7. Parse Paths (<path d="...">)
        val pathRegex = Regex("""<path\s+([^>]+)/>""", RegexOption.IGNORE_CASE)
        for (match in pathRegex.findAll(svgContent)) {
            val attrs = match.groupValues[1]
            val d = extractStringAttr(attrs, "d")
            if (d.isNotEmpty()) {
                val parsedSubPaths = parseSvgPathD(d, scale)
                if (parsedSubPaths.isNotEmpty()) {
                    shapes.add(
                        LaserShape.VectorPath(
                            layerId = defaultLayerId,
                            name = "SVG Path",
                            subPaths = parsedSubPaths,
                            isClosed = d.contains("z", ignoreCase = true)
                        )
                    )
                }
            }
        }

        // Calculate bounding box and reposition to sensible origin (e.g. 20, 20 mm) if needed
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (shape in shapes) {
            val b = shape.bounds()
            if (b.left < minX) minX = b.left
            if (b.top < minY) minY = b.top
            if (b.right > maxX) maxX = b.right
            if (b.bottom > maxY) maxY = b.bottom
        }

        val totalW = if (maxX > minX) maxX - minX else viewBoxWidth * scale
        val totalH = if (maxY > minY) maxY - minY else viewBoxHeight * scale

        return SvgImportResult(
            shapes = shapes,
            widthMm = totalW,
            heightMm = totalH,
            description = "Imported ${shapes.size} SVG vector elements (${String.format("%.1f", totalW)} x ${String.format("%.1f", totalH)} mm)"
        )
    }

    private fun extractAttr(attrs: String, name: String): Float {
        val pattern = Pattern.compile("""$name=["']([-\d.]+)""", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(attrs)
        return if (matcher.find()) {
            matcher.group(1)?.toFloatOrNull() ?: 0f
        } else 0f
    }

    private fun extractStringAttr(attrs: String, name: String): String {
        val pattern = Pattern.compile("""$name=["']([^"']+)["']""", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(attrs)
        return if (matcher.find()) {
            matcher.group(1) ?: ""
        } else ""
    }

    private fun parsePointsList(pointsStr: String, scale: Float): List<Point2D> {
        val numbers = pointsStr.trim().split(Regex("""[\s,]+""")).mapNotNull { it.toFloatOrNull() }
        val points = mutableListOf<Point2D>()
        var i = 0
        while (i + 1 < numbers.size) {
            points.add(Point2D(numbers[i] * scale, numbers[i + 1] * scale))
            i += 2
        }
        return points
    }

    /**
     * Parses SVG Path 'd' attribute commands into discrete millimeter polyline points.
     * Evaluates lines, relative offsets, and cubic bezier curves using de Casteljau interpolation.
     */
    fun parseSvgPathD(d: String, scale: Float): List<List<Point2D>> {
        val subPaths = mutableListOf<List<Point2D>>()
        var currentSubPath = mutableListOf<Point2D>()

        var currentX = 0f
        var currentY = 0f
        var startX = 0f
        var startY = 0f

        val tokens = tokenizePathData(d)
        var i = 0

        while (i < tokens.size) {
            val token = tokens[i]
            val cmd = token[0]

            when (cmd) {
                'M', 'm' -> {
                    if (currentSubPath.size >= 2) {
                        subPaths.add(currentSubPath)
                    }
                    currentSubPath = mutableListOf()
                    val isRel = cmd == 'm'
                    val x = tokens.getOrNull(i + 1)?.toFloatOrNull() ?: 0f
                    val y = tokens.getOrNull(i + 2)?.toFloatOrNull() ?: 0f
                    i += 2

                    currentX = if (isRel) currentX + x * scale else x * scale
                    currentY = if (isRel) currentY + y * scale else y * scale
                    startX = currentX
                    startY = currentY
                    currentSubPath.add(Point2D(currentX, currentY))

                    // Subsequent pairs are treated as implicit L/l
                    while (i + 2 < tokens.size && tokens[i + 1][0].isDigitOrSign()) {
                        val lx = tokens[i + 1].toFloatOrNull() ?: 0f
                        val ly = tokens[i + 2].toFloatOrNull() ?: 0f
                        i += 2
                        currentX = if (isRel) currentX + lx * scale else lx * scale
                        currentY = if (isRel) currentY + ly * scale else ly * scale
                        currentSubPath.add(Point2D(currentX, currentY))
                    }
                }
                'L', 'l' -> {
                    val isRel = cmd == 'l'
                    while (i + 2 < tokens.size && tokens[i + 1][0].isDigitOrSign()) {
                        val x = tokens[i + 1].toFloatOrNull() ?: 0f
                        val y = tokens[i + 2].toFloatOrNull() ?: 0f
                        i += 2
                        currentX = if (isRel) currentX + x * scale else x * scale
                        currentY = if (isRel) currentY + y * scale else y * scale
                        currentSubPath.add(Point2D(currentX, currentY))
                    }
                }
                'H', 'h' -> {
                    val isRel = cmd == 'h'
                    while (i + 1 < tokens.size && tokens[i + 1][0].isDigitOrSign()) {
                        val x = tokens[i + 1].toFloatOrNull() ?: 0f
                        i += 1
                        currentX = if (isRel) currentX + x * scale else x * scale
                        currentSubPath.add(Point2D(currentX, currentY))
                    }
                }
                'V', 'v' -> {
                    val isRel = cmd == 'v'
                    while (i + 1 < tokens.size && tokens[i + 1][0].isDigitOrSign()) {
                        val y = tokens[i + 1].toFloatOrNull() ?: 0f
                        i += 1
                        currentY = if (isRel) currentY + y * scale else y * scale
                        currentSubPath.add(Point2D(currentX, currentY))
                    }
                }
                'C', 'c' -> {
                    val isRel = cmd == 'c'
                    while (i + 6 < tokens.size && tokens[i + 1][0].isDigitOrSign()) {
                        val x1 = (tokens[i + 1].toFloatOrNull() ?: 0f) * scale
                        val y1 = (tokens[i + 2].toFloatOrNull() ?: 0f) * scale
                        val x2 = (tokens[i + 3].toFloatOrNull() ?: 0f) * scale
                        val y2 = (tokens[i + 4].toFloatOrNull() ?: 0f) * scale
                        val x = (tokens[i + 5].toFloatOrNull() ?: 0f) * scale
                        val y = (tokens[i + 6].toFloatOrNull() ?: 0f) * scale
                        i += 6

                        val cp1x = if (isRel) currentX + x1 else x1
                        val cp1y = if (isRel) currentY + y1 else y1
                        val cp2x = if (isRel) currentX + x2 else x2
                        val cp2y = if (isRel) currentY + y2 else y2
                        val endX = if (isRel) currentX + x else x
                        val endY = if (isRel) currentY + y else y

                        // Discretize cubic bezier into 8 line segments for precision cutting
                        for (step in 1..8) {
                            val t = step / 8f
                            val bx = bezierCubic(currentX, cp1x, cp2x, endX, t)
                            val by = bezierCubic(currentY, cp1y, cp2y, endY, t)
                            currentSubPath.add(Point2D(bx, by))
                        }
                        currentX = endX
                        currentY = endY
                    }
                }
                'Z', 'z' -> {
                    if (currentSubPath.isNotEmpty()) {
                        currentSubPath.add(Point2D(startX, startY))
                        subPaths.add(currentSubPath)
                        currentSubPath = mutableListOf()
                        currentX = startX
                        currentY = startY
                    }
                }
            }
            i++
        }

        if (currentSubPath.size >= 2) {
            subPaths.add(currentSubPath)
        }

        return subPaths
    }

    private fun bezierCubic(p0: Float, p1: Float, p2: Float, p3: Float, t: Float): Float {
        val u = 1f - t
        return u * u * u * p0 + 3f * u * u * t * p1 + 3f * u * t * t * p2 + t * t * t * p3
    }

    private fun Char.isDigitOrSign(): Boolean =
        this.isDigit() || this == '-' || this == '+' || this == '.'

    private fun tokenizePathData(d: String): List<String> {
        val tokens = mutableListOf<String>()
        val regex = Regex("""([a-df-z]|[-+]?(?:\d*\.\d+|\d+)(?:[eE][-+]?\d+)?)""", RegexOption.IGNORE_CASE)
        for (match in regex.findAll(d)) {
            tokens.add(match.value)
        }
        return tokens
    }
}
