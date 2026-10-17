package com.example.ui.components

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LaserLayer
import com.example.model.LaserShape
import com.example.model.MachineProfile
import com.example.model.Point2D
import com.example.ui.theme.LaserBedBorder
import com.example.ui.theme.LaserBedGridMajor
import com.example.ui.theme.LaserBedGridMinor
import com.example.ui.theme.LaserDarkBg
import com.example.ui.theme.LaserPrimary
import com.example.viewmodel.DrawingTool
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun CadCanvas(
    modifier: Modifier = Modifier,
    shapes: List<LaserShape>,
    layers: List<LaserLayer>,
    profile: MachineProfile,
    selectedShapeIds: Set<String>,
    activeTool: DrawingTool,
    activeLayerId: String,
    zoomScale: Float,
    panOffsetX: Float,
    panOffsetY: Float,
    onTransform: (zoomDelta: Float, panDx: Float, panDy: Float) -> Unit,
    onShapeSelect: (shapeId: String?, addToSelection: Boolean) -> Unit,
    onShapeTranslate: (dxMm: Float, dyMm: Float) -> Unit,
    onTransformationCommit: () -> Unit,
    onShapeCreate: (LaserShape) -> Unit
) {
    val layerMap = remember(layers) { layers.associateBy { it.id } }

    var cursorCoordsMm by remember { mutableStateOf(Pair(0f, 0f)) }
    var isDraggingShape by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(LaserDarkBg)
            .testTag("cad_canvas_container")
    ) {
        val rulerThicknessPx = 28.dp.value * 2f // Top and Left ruler thickness

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("cad_drawing_canvas")
                .pointerInput(activeTool, zoomScale, panOffsetX, panOffsetY, selectedShapeIds) {
                    // Tap gestures for selection and shape placement
                    detectTapGestures(
                        onTap = { offset ->
                            val mmX = (offset.x - panOffsetX) / zoomScale
                            val mmY = (offset.y - panOffsetY) / zoomScale
                            cursorCoordsMm = Pair(mmX, mmY)

                            if (activeTool == DrawingTool.SELECT) {
                                // Hit test from top to bottom
                                val hit = shapes.reversed().find { s ->
                                    val b = s.bounds()
                                    mmX >= b.left && mmX <= b.right && mmY >= b.top && mmY <= b.bottom
                                }
                                onShapeSelect(hit?.id, false)
                            } else {
                                // Tool creation on tap
                                val snappedX = (mmX / 5f).roundToInt() * 5f
                                val snappedY = (mmY / 5f).roundToInt() * 5f

                                when (activeTool) {
                                    DrawingTool.RECTANGLE -> {
                                        onShapeCreate(
                                            LaserShape.Rectangle(
                                                layerId = activeLayerId,
                                                name = "Rectangle",
                                                x = snappedX,
                                                y = snappedY,
                                                width = 30f,
                                                height = 20f
                                            )
                                        )
                                    }
                                    DrawingTool.CIRCLE -> {
                                        onShapeCreate(
                                            LaserShape.Circle(
                                                layerId = activeLayerId,
                                                name = "Circle",
                                                centerX = snappedX,
                                                centerY = snappedY,
                                                radius = 15f
                                            )
                                        )
                                    }
                                    DrawingTool.LINE -> {
                                        onShapeCreate(
                                            LaserShape.Line(
                                                layerId = activeLayerId,
                                                name = "Line",
                                                x1 = snappedX,
                                                y1 = snappedY,
                                                x2 = snappedX + 30f,
                                                y2 = snappedY
                                            )
                                        )
                                    }
                                    DrawingTool.POLYGON -> {
                                        // 5-sided polygon
                                        val pts = mutableListOf<Point2D>()
                                        val r = 18f
                                        for (i in 0 until 5) {
                                            val a = (i * 2.0 * Math.PI / 5) - Math.PI / 2
                                            pts.add(Point2D((snappedX + r * kotlin.math.cos(a)).toFloat(), (snappedY + r * kotlin.math.sin(a)).toFloat()))
                                        }
                                        onShapeCreate(
                                            LaserShape.Polygon(
                                                layerId = activeLayerId,
                                                name = "Pentagon",
                                                points = pts,
                                                isClosed = true
                                            )
                                        )
                                    }
                                    DrawingTool.TEXT -> {
                                        onShapeCreate(
                                            LaserShape.Text(
                                                layerId = activeLayerId,
                                                name = "Text",
                                                text = "LASER",
                                                x = snappedX,
                                                y = snappedY,
                                                heightMm = 12f
                                            )
                                        )
                                    }
                                    DrawingTool.MEASURE -> {}
                                    DrawingTool.RASTER_IMAGE -> {}
                                    DrawingTool.SELECT -> {}
                                }
                            }
                        }
                    )
                }
                .pointerInput(activeTool, zoomScale, selectedShapeIds) {
                    // Multi-touch pinch-to-zoom and pan
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        if (activeTool == DrawingTool.SELECT && selectedShapeIds.isNotEmpty() && zoom == 1f) {
                            // Dragging selected shapes
                            val dxMm = pan.x / zoomScale
                            val dyMm = pan.y / zoomScale
                            onShapeTranslate(dxMm, dyMm)
                            isDraggingShape = true
                        } else {
                            // Panning or zooming canvas
                            onTransform(zoom, pan.x, pan.y)
                        }
                    }
                }
        ) {
            // 1. Draw Machine Bed Grid
            drawMachineBed(
                profile = profile,
                zoomScale = zoomScale,
                panOffsetX = panOffsetX,
                panOffsetY = panOffsetY
            )

            // 2. Draw Shapes
            for (shape in shapes) {
                val layer = layerMap[shape.layerId] ?: LaserLayer.DEFAULT_LAYERS.first()
                if (!layer.enabled) continue

                val color = layer.composeColor
                val isSelected = selectedShapeIds.contains(shape.id)

                drawShape(
                    shape = shape,
                    color = color,
                    zoomScale = zoomScale,
                    panOffsetX = panOffsetX,
                    panOffsetY = panOffsetY,
                    isSelected = isSelected
                )
            }

            // 3. Draw Selection Bounding Box & Handles
            val selectedShapes = shapes.filter { selectedShapeIds.contains(it.id) }
            if (selectedShapes.isNotEmpty()) {
                var minX = Float.MAX_VALUE
                var minY = Float.MAX_VALUE
                var maxX = -Float.MAX_VALUE
                var maxY = -Float.MAX_VALUE
                for (s in selectedShapes) {
                    val b = s.bounds()
                    if (b.left < minX) minX = b.left
                    if (b.top < minY) minY = b.top
                    if (b.right > maxX) maxX = b.right
                    if (b.bottom > maxY) maxY = b.bottom
                }
                drawSelectionBox(minX, minY, maxX, maxY, zoomScale, panOffsetX, panOffsetY)
            }

            // 4. Draw Millimeter Rulers along Top and Left
            drawRulers(
                canvasWidth = size.width,
                canvasHeight = size.height,
                zoomScale = zoomScale,
                panOffsetX = panOffsetX,
                panOffsetY = panOffsetY,
                profile = profile
            )
        }

        // Overlay: Coords & Zoom Status Pill
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            shadowElevation = 4.dp
        ) {
            Text(
                text = String.format(
                    Locale.US,
                    "X: %.1f mm  |  Y: %.1f mm  |  Zoom: %d%%",
                    cursorCoordsMm.first,
                    cursorCoordsMm.second,
                    (zoomScale * 66.6f).toInt()
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

private fun DrawScope.drawMachineBed(
    profile: MachineProfile,
    zoomScale: Float,
    panOffsetX: Float,
    panOffsetY: Float
) {
    val bedW = profile.bedWidthMm * zoomScale
    val bedH = profile.bedHeightMm * zoomScale
    val startX = panOffsetX
    val startY = panOffsetY

    // Bed background fill
    drawRect(
        color = Color(0xFF141920),
        topLeft = Offset(startX, startY),
        size = Size(bedW, bedH)
    )

    // Minor Grid Lines (every 10mm)
    var mmX = 0f
    while (mmX <= profile.bedWidthMm) {
        val px = startX + (mmX * zoomScale)
        val isMajor = (mmX % 50f) < 0.1f
        drawLine(
            color = if (isMajor) LaserBedGridMajor else LaserBedGridMinor,
            start = Offset(px, startY),
            end = Offset(px, startY + bedH),
            strokeWidth = if (isMajor) 1.5f else 0.8f
        )
        mmX += 10f
    }

    var mmY = 0f
    while (mmY <= profile.bedHeightMm) {
        val py = startY + (mmY * zoomScale)
        val isMajor = (mmY % 50f) < 0.1f
        drawLine(
            color = if (isMajor) LaserBedGridMajor else LaserBedGridMinor,
            start = Offset(startX, py),
            end = Offset(startX + bedW, py),
            strokeWidth = if (isMajor) 1.5f else 0.8f
        )
        mmY += 10f
    }

    // Outer Bed Limit Border
    drawRect(
        color = LaserBedBorder,
        topLeft = Offset(startX, startY),
        size = Size(bedW, bedH),
        style = Stroke(width = 2.5f)
    )

    // Origin Indicator (0, 0)
    drawCircle(
        color = Color(0xFFFF3D00),
        center = Offset(startX, startY),
        radius = 5f
    )
    drawLine(
        color = Color(0xFFFF3D00),
        start = Offset(startX - 10f, startY),
        end = Offset(startX + 18f, startY),
        strokeWidth = 2f
    )
    drawLine(
        color = Color(0xFFFF3D00),
        start = Offset(startX, startY - 10f),
        end = Offset(startX, startY + 18f),
        strokeWidth = 2f
    )
}

private fun DrawScope.drawShape(
    shape: LaserShape,
    color: Color,
    zoomScale: Float,
    panOffsetX: Float,
    panOffsetY: Float,
    isSelected: Boolean
) {
    val strokeWidth = if (isSelected) 2.5f else 1.5f
    val displayColor = if (isSelected) Color(0xFF00E5FF) else color

    when (shape) {
        is LaserShape.Rectangle -> {
            val px = panOffsetX + shape.x * zoomScale
            val py = panOffsetY + shape.y * zoomScale
            val pw = shape.width * zoomScale
            val ph = shape.height * zoomScale
            val pr = shape.cornerRadius * zoomScale

            drawRoundRect(
                color = displayColor,
                topLeft = Offset(px, py),
                size = Size(pw, ph),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(pr, pr),
                style = Stroke(width = strokeWidth)
            )
        }
        is LaserShape.Circle -> {
            val pcx = panOffsetX + shape.centerX * zoomScale
            val pcy = panOffsetY + shape.centerY * zoomScale
            val pr = shape.radius * zoomScale

            drawCircle(
                color = displayColor,
                center = Offset(pcx, pcy),
                radius = pr,
                style = Stroke(width = strokeWidth)
            )
        }
        is LaserShape.Ellipse -> {
            val pcx = panOffsetX + shape.centerX * zoomScale
            val pcy = panOffsetY + shape.centerY * zoomScale
            val prx = shape.radiusX * zoomScale
            val pry = shape.radiusY * zoomScale

            drawOval(
                color = displayColor,
                topLeft = Offset(pcx - prx, pcy - pry),
                size = Size(prx * 2f, pry * 2f),
                style = Stroke(width = strokeWidth)
            )
        }
        is LaserShape.Line -> {
            val px1 = panOffsetX + shape.x1 * zoomScale
            val py1 = panOffsetY + shape.y1 * zoomScale
            val px2 = panOffsetX + shape.x2 * zoomScale
            val py2 = panOffsetY + shape.y2 * zoomScale

            drawLine(
                color = displayColor,
                start = Offset(px1, py1),
                end = Offset(px2, py2),
                strokeWidth = strokeWidth
            )
        }
        is LaserShape.Polygon -> {
            if (shape.points.size >= 2) {
                val path = Path()
                val first = shape.points.first()
                path.moveTo(panOffsetX + first.x * zoomScale, panOffsetY + first.y * zoomScale)
                for (i in 1 until shape.points.size) {
                    val p = shape.points[i]
                    path.lineTo(panOffsetX + p.x * zoomScale, panOffsetY + p.y * zoomScale)
                }
                if (shape.isClosed) path.close()
                drawPath(path, displayColor, style = Stroke(width = strokeWidth))
            }
        }
        is LaserShape.VectorPath -> {
            for (sub in shape.subPaths) {
                if (sub.size >= 2) {
                    val path = Path()
                    val first = sub.first()
                    path.moveTo(panOffsetX + first.x * zoomScale, panOffsetY + first.y * zoomScale)
                    for (i in 1 until sub.size) {
                        val p = sub[i]
                        path.lineTo(panOffsetX + p.x * zoomScale, panOffsetY + p.y * zoomScale)
                    }
                    if (shape.isClosed) path.close()
                    drawPath(path, displayColor, style = Stroke(width = strokeWidth))
                }
            }
        }
        is LaserShape.Text -> {
            val px = panOffsetX + shape.x * zoomScale
            val py = panOffsetY + (shape.y + shape.heightMm) * zoomScale
            val fontSizePx = shape.heightMm * zoomScale

            drawContext.canvas.nativeCanvas.apply {
                val paint = Paint().apply {
                    this.color = displayColor.hashCode()
                    this.textSize = fontSizePx
                    this.isAntiAlias = true
                    this.style = Paint.Style.FILL
                }
                drawText(shape.text, px, py, paint)
            }
        }
        is LaserShape.RasterImage -> {
            val px = panOffsetX + shape.x * zoomScale
            val py = panOffsetY + shape.y * zoomScale
            val pw = shape.width * zoomScale
            val ph = shape.height * zoomScale

            // Draw dashed container and raster lines
            drawRect(
                color = displayColor,
                topLeft = Offset(px, py),
                size = Size(pw, ph),
                style = Stroke(
                    width = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                )
            )
            // Hatching indicator
            val linesCount = 8
            for (i in 1..linesCount) {
                val ly = py + (ph * i / (linesCount + 1))
                drawLine(
                    color = displayColor.copy(alpha = 0.4f),
                    start = Offset(px, ly),
                    end = Offset(px + pw, ly),
                    strokeWidth = 1f
                )
            }
        }
    }
}

private fun DrawScope.drawSelectionBox(
    minX: Float,
    minY: Float,
    maxX: Float,
    maxY: Float,
    zoomScale: Float,
    panOffsetX: Float,
    panOffsetY: Float
) {
    val padding = 4f
    val px = panOffsetX + (minX - padding) * zoomScale
    val py = panOffsetY + (minY - padding) * zoomScale
    val pw = (maxX - minX + padding * 2f) * zoomScale
    val ph = (maxY - minY + padding * 2f) * zoomScale

    // Bounding Box Rectangle (dashed cyan)
    drawRect(
        color = Color(0xFF00E5FF),
        topLeft = Offset(px, py),
        size = Size(pw, ph),
        style = Stroke(
            width = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
        )
    )

    // Corner Handles
    val handleRadius = 6f
    val handleColor = Color(0xFF00E5FF)
    val corners = listOf(
        Offset(px, py),
        Offset(px + pw / 2, py),
        Offset(px + pw, py),
        Offset(px + pw, py + ph / 2),
        Offset(px + pw, py + ph),
        Offset(px + pw / 2, py + ph),
        Offset(px, py + ph),
        Offset(px, py + ph / 2)
    )
    for (c in corners) {
        drawCircle(color = Color.White, center = c, radius = handleRadius)
        drawCircle(color = handleColor, center = c, radius = handleRadius, style = Stroke(width = 2f))
    }
}

private fun DrawScope.drawRulers(
    canvasWidth: Float,
    canvasHeight: Float,
    zoomScale: Float,
    panOffsetX: Float,
    panOffsetY: Float,
    profile: MachineProfile
) {
    val rulerColor = Color(0xFF1B222C)
    val tickColor = Color(0xFF8B949E)
    val rulerThick = 20f

    // Top Ruler Background
    drawRect(color = rulerColor, topLeft = Offset(0f, 0f), size = Size(canvasWidth, rulerThick))
    // Left Ruler Background
    drawRect(color = rulerColor, topLeft = Offset(0f, 0f), size = Size(rulerThick, canvasHeight))
    // Origin intersection
    drawRect(color = Color(0xFF28303C), topLeft = Offset(0f, 0f), size = Size(rulerThick, rulerThick))

    // Top X ticks
    var mmX = 0f
    while (mmX <= profile.bedWidthMm) {
        val px = panOffsetX + (mmX * zoomScale)
        if (px >= rulerThick && px <= canvasWidth) {
            val isMajor = (mmX % 50f) < 0.1f
            val isMedium = (mmX % 10f) < 0.1f
            val tickLen = if (isMajor) 14f else if (isMedium) 8f else 4f
            drawLine(
                color = tickColor,
                start = Offset(px, rulerThick - tickLen),
                end = Offset(px, rulerThick),
                strokeWidth = 1f
            )
            if (isMajor) {
                drawContext.canvas.nativeCanvas.apply {
                    val paint = Paint().apply {
                        this.color = 0xFF8B949E.toInt()
                        this.textSize = 10.sp.toPx()
                        this.isAntiAlias = true
                    }
                    drawText("${mmX.toInt()}", px + 2f, rulerThick - 4f, paint)
                }
            }
        }
        mmX += 10f
    }

    // Left Y ticks
    var mmY = 0f
    while (mmY <= profile.bedHeightMm) {
        val py = panOffsetY + (mmY * zoomScale)
        if (py >= rulerThick && py <= canvasHeight) {
            val isMajor = (mmY % 50f) < 0.1f
            val isMedium = (mmY % 10f) < 0.1f
            val tickLen = if (isMajor) 14f else if (isMedium) 8f else 4f
            drawLine(
                color = tickColor,
                start = Offset(rulerThick - tickLen, py),
                end = Offset(rulerThick, py),
                strokeWidth = 1f
            )
            if (isMajor) {
                drawContext.canvas.nativeCanvas.apply {
                    val paint = Paint().apply {
                        this.color = 0xFF8B949E.toInt()
                        this.textSize = 10.sp.toPx()
                        this.isAntiAlias = true
                    }
                    drawText("${mmY.toInt()}", 2f, py - 2f, paint)
                }
            }
        }
        mmY += 10f
    }
}
