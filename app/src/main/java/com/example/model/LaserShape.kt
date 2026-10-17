package com.example.model

import android.graphics.Path
import android.graphics.RectF
import java.util.UUID

enum class DitherMode(val displayName: String) {
    THRESHOLD("Threshold (Black & White)"),
    FLOYD_STEINBERG("Floyd-Steinberg Dither"),
    ATKINSON("Atkinson Dither"),
    GRAYSCALE_PWM("Grayscale (Laser Power Modulation)")
}

data class Point2D(val x: Float, val y: Float)

sealed class LaserShape {
    abstract val id: String
    abstract val layerId: String
    abstract val name: String
    abstract val rotationDeg: Float
    abstract fun bounds(): RectF
    abstract fun translate(dx: Float, dy: Float): LaserShape
    abstract fun scale(scaleX: Float, scaleY: Float, originX: Float, originY: Float): LaserShape
    abstract fun copyWithLayer(newLayerId: String): LaserShape

    data class Rectangle(
        override val id: String = UUID.randomUUID().toString(),
        override val layerId: String,
        override val name: String = "Rectangle",
        override val rotationDeg: Float = 0f,
        val x: Float, // mm (top-left)
        val y: Float, // mm (top-left)
        val width: Float, // mm
        val height: Float, // mm
        val cornerRadius: Float = 0f // mm
    ) : LaserShape() {
        override fun bounds(): RectF = RectF(x, y, x + width, y + height)
        override fun translate(dx: Float, dy: Float): Rectangle =
            copy(x = x + dx, y = y + dy)
        override fun scale(scaleX: Float, scaleY: Float, originX: Float, originY: Float): Rectangle {
            val newX = originX + (x - originX) * scaleX
            val newY = originY + (y - originY) * scaleY
            val newW = width * scaleX
            val newH = height * scaleY
            return copy(
                x = minOf(newX, newX + newW),
                y = minOf(newY, newY + newH),
                width = kotlin.math.abs(newW),
                height = kotlin.math.abs(newH)
            )
        }
        override fun copyWithLayer(newLayerId: String): Rectangle = copy(layerId = newLayerId)
    }

    data class Circle(
        override val id: String = UUID.randomUUID().toString(),
        override val layerId: String,
        override val name: String = "Circle",
        override val rotationDeg: Float = 0f,
        val centerX: Float, // mm
        val centerY: Float, // mm
        val radius: Float // mm
    ) : LaserShape() {
        override fun bounds(): RectF = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
        override fun translate(dx: Float, dy: Float): Circle =
            copy(centerX = centerX + dx, centerY = centerY + dy)
        override fun scale(scaleX: Float, scaleY: Float, originX: Float, originY: Float): Circle {
            val newCx = originX + (centerX - originX) * scaleX
            val newCy = originY + (centerY - originY) * scaleY
            val factor = maxOf(kotlin.math.abs(scaleX), kotlin.math.abs(scaleY))
            return copy(centerX = newCx, centerY = newCy, radius = radius * factor)
        }
        override fun copyWithLayer(newLayerId: String): Circle = copy(layerId = newLayerId)
    }

    data class Ellipse(
        override val id: String = UUID.randomUUID().toString(),
        override val layerId: String,
        override val name: String = "Ellipse",
        override val rotationDeg: Float = 0f,
        val centerX: Float,
        val centerY: Float,
        val radiusX: Float,
        val radiusY: Float
    ) : LaserShape() {
        override fun bounds(): RectF = RectF(
            centerX - radiusX,
            centerY - radiusY,
            centerX + radiusX,
            centerY + radiusY
        )
        override fun translate(dx: Float, dy: Float): Ellipse =
            copy(centerX = centerX + dx, centerY = centerY + dy)
        override fun scale(scaleX: Float, scaleY: Float, originX: Float, originY: Float): Ellipse {
            val newCx = originX + (centerX - originX) * scaleX
            val newCy = originY + (centerY - originY) * scaleY
            return copy(
                centerX = newCx,
                centerY = newCy,
                radiusX = radiusX * kotlin.math.abs(scaleX),
                radiusY = radiusY * kotlin.math.abs(scaleY)
            )
        }
        override fun copyWithLayer(newLayerId: String): Ellipse = copy(layerId = newLayerId)
    }

    data class Line(
        override val id: String = UUID.randomUUID().toString(),
        override val layerId: String,
        override val name: String = "Line",
        override val rotationDeg: Float = 0f,
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float
    ) : LaserShape() {
        override fun bounds(): RectF = RectF(
            minOf(x1, x2),
            minOf(y1, y2),
            maxOf(x1, x2),
            maxOf(y1, y2)
        )
        override fun translate(dx: Float, dy: Float): Line =
            copy(x1 = x1 + dx, y1 = y1 + dy, x2 = x2 + dx, y2 = y2 + dy)
        override fun scale(scaleX: Float, scaleY: Float, originX: Float, originY: Float): Line =
            copy(
                x1 = originX + (x1 - originX) * scaleX,
                y1 = originY + (y1 - originY) * scaleY,
                x2 = originX + (x2 - originX) * scaleX,
                y2 = originY + (y2 - originY) * scaleY
            )
        override fun copyWithLayer(newLayerId: String): Line = copy(layerId = newLayerId)
    }

    data class Polygon(
        override val id: String = UUID.randomUUID().toString(),
        override val layerId: String,
        override val name: String = "Polygon",
        override val rotationDeg: Float = 0f,
        val points: List<Point2D>,
        val isClosed: Boolean = true
    ) : LaserShape() {
        override fun bounds(): RectF {
            if (points.isEmpty()) return RectF(0f, 0f, 0f, 0f)
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE
            for (p in points) {
                if (p.x < minX) minX = p.x
                if (p.y < minY) minY = p.y
                if (p.x > maxX) maxX = p.x
                if (p.y > maxY) maxY = p.y
            }
            return RectF(minX, minY, maxX, maxY)
        }
        override fun translate(dx: Float, dy: Float): Polygon =
            copy(points = points.map { Point2D(it.x + dx, it.y + dy) })
        override fun scale(scaleX: Float, scaleY: Float, originX: Float, originY: Float): Polygon =
            copy(points = points.map {
                Point2D(
                    originX + (it.x - originX) * scaleX,
                    originY + (it.y - originY) * scaleY
                )
            })
        override fun copyWithLayer(newLayerId: String): Polygon = copy(layerId = newLayerId)
    }

    data class VectorPath(
        override val id: String = UUID.randomUUID().toString(),
        override val layerId: String,
        override val name: String = "Vector Path",
        override val rotationDeg: Float = 0f,
        val subPaths: List<List<Point2D>>, // List of continuous polylines
        val isClosed: Boolean = true,
        val cachedBounds: RectF = RectF()
    ) : LaserShape() {
        override fun bounds(): RectF {
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE
            var count = 0
            for (sub in subPaths) {
                for (p in sub) {
                    count++
                    if (p.x < minX) minX = p.x
                    if (p.y < minY) minY = p.y
                    if (p.x > maxX) maxX = p.x
                    if (p.y > maxY) maxY = p.y
                }
            }
            return if (count == 0) RectF(0f, 0f, 0f, 0f) else RectF(minX, minY, maxX, maxY)
        }
        override fun translate(dx: Float, dy: Float): VectorPath =
            copy(subPaths = subPaths.map { sub -> sub.map { Point2D(it.x + dx, it.y + dy) } })
        override fun scale(scaleX: Float, scaleY: Float, originX: Float, originY: Float): VectorPath =
            copy(subPaths = subPaths.map { sub ->
                sub.map {
                    Point2D(
                        originX + (it.x - originX) * scaleX,
                        originY + (it.y - originY) * scaleY
                    )
                }
            })
        override fun copyWithLayer(newLayerId: String): VectorPath = copy(layerId = newLayerId)
    }

    data class Text(
        override val id: String = UUID.randomUUID().toString(),
        override val layerId: String,
        override val name: String = "Text",
        override val rotationDeg: Float = 0f,
        val text: String,
        val x: Float, // mm
        val y: Float, // mm
        val heightMm: Float = 15f,
        val fontFamily: String = "SansSerif"
    ) : LaserShape() {
        // Approximate width based on character count and height ratio
        val estimatedWidth: Float
            get() = text.length * heightMm * 0.65f

        override fun bounds(): RectF =
            RectF(x, y, x + estimatedWidth, y + heightMm)
        override fun translate(dx: Float, dy: Float): Text =
            copy(x = x + dx, y = y + dy)
        override fun scale(scaleX: Float, scaleY: Float, originX: Float, originY: Float): Text =
            copy(
                x = originX + (x - originX) * scaleX,
                y = originY + (y - originY) * scaleY,
                heightMm = heightMm * kotlin.math.abs(scaleY)
            )
        override fun copyWithLayer(newLayerId: String): Text = copy(layerId = newLayerId)
    }

    data class RasterImage(
        override val id: String = UUID.randomUUID().toString(),
        override val layerId: String,
        override val name: String = "Raster Image",
        override val rotationDeg: Float = 0f,
        val x: Float, // mm
        val y: Float, // mm
        val width: Float, // mm
        val height: Float, // mm
        val ditherMode: DitherMode = DitherMode.FLOYD_STEINBERG,
        val threshold: Int = 128, // 0..255
        val brightness: Float = 0f, // -100..100
        val contrast: Float = 1f, // 0.1..3.0
        val isInverted: Boolean = false,
        val pixelWidth: Int = 100,
        val pixelHeight: Int = 100,
        val grayscalePixels: ByteArray? = null // Row-major 8-bit grayscale
    ) : LaserShape() {
        override fun bounds(): RectF = RectF(x, y, x + width, y + height)
        override fun translate(dx: Float, dy: Float): RasterImage =
            copy(x = x + dx, y = y + dy)
        override fun scale(scaleX: Float, scaleY: Float, originX: Float, originY: Float): RasterImage =
            copy(
                x = originX + (x - originX) * scaleX,
                y = originY + (y - originY) * scaleY,
                width = width * kotlin.math.abs(scaleX),
                height = height * kotlin.math.abs(scaleY)
            )
        override fun copyWithLayer(newLayerId: String): RasterImage = copy(layerId = newLayerId)
    }
}
