package com.example.parser

import android.graphics.Bitmap
import com.example.model.DitherMode
import com.example.model.LaserShape
import com.example.model.Point2D
import kotlin.math.max
import kotlin.math.min

object ImageProcessor {

    data class ProcessedImageResult(
        val binaryMatrix: Array<BooleanArray>, // true = burn dot, false = off
        val grayscaleMatrix: Array<FloatArray>, // 0.0 (white/off) to 1.0 (black/full power)
        val widthPx: Int,
        val heightPx: Int
    )

    fun processBitmapToMatrix(
        bitmap: Bitmap,
        ditherMode: DitherMode,
        threshold: Int = 128,
        brightness: Float = 0f,
        contrast: Float = 1f,
        inverted: Boolean = false,
        targetWidth: Int = 120
    ): ProcessedImageResult {
        val aspect = bitmap.height.toFloat() / bitmap.width.toFloat()
        val targetHeight = max(10, (targetWidth * aspect).toInt())

        val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        val w = scaled.width
        val h = scaled.height

        // 1. Convert to 2D float array 0.0..1.0
        val gray = Array(h) { FloatArray(w) }
        for (y in 0 until h) {
            for (x in 0 until w) {
                val pixel = scaled.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                // Standard luma formula
                var lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f

                // Apply brightness (-1.0 to 1.0) and contrast (0.1 to 3.0)
                lum = (lum - 0.5f) * contrast + 0.5f + (brightness / 100f)
                lum = max(0f, min(1f, lum))

                if (inverted) {
                    lum = 1f - lum
                }
                gray[y][x] = lum
            }
        }

        val binary = Array(h) { BooleanArray(w) }
        val outputGray = Array(h) { FloatArray(w) }

        when (ditherMode) {
            DitherMode.THRESHOLD -> {
                val threshNorm = threshold / 255f
                for (y in 0 until h) {
                    for (x in 0 until w) {
                        // In laser engraving, darker (lower luminance) means burn dot
                        val burn = gray[y][x] < threshNorm
                        binary[y][x] = burn
                        outputGray[y][x] = if (burn) 1.0f else 0.0f
                    }
                }
            }
            DitherMode.FLOYD_STEINBERG -> {
                // Floyd-Steinberg error diffusion
                val buffer = Array(h) { FloatArray(w) { x -> gray[it][x] } }
                for (y in 0 until h) {
                    for (x in 0 until w) {
                        val oldVal = buffer[y][x]
                        val newVal = if (oldVal < 0.5f) 0f else 1f
                        val err = oldVal - newVal
                        val burn = newVal == 0f
                        binary[y][x] = burn
                        outputGray[y][x] = if (burn) 1f else 0f

                        if (x + 1 < w) buffer[y][x + 1] += err * (7f / 16f)
                        if (y + 1 < h) {
                            if (x - 1 >= 0) buffer[y + 1][x - 1] += err * (3f / 16f)
                            buffer[y + 1][x] += err * (5f / 16f)
                            if (x + 1 < w) buffer[y + 1][x + 1] += err * (1f / 16f)
                        }
                    }
                }
            }
            DitherMode.ATKINSON -> {
                // Atkinson dithering: distributes 1/8 to 6 neighbors (preserves highlights)
                val buffer = Array(h) { FloatArray(w) { x -> gray[it][x] } }
                for (y in 0 until h) {
                    for (x in 0 until w) {
                        val oldVal = buffer[y][x]
                        val newVal = if (oldVal < 0.5f) 0f else 1f
                        val err = (oldVal - newVal) / 8f
                        val burn = newVal == 0f
                        binary[y][x] = burn
                        outputGray[y][x] = if (burn) 1f else 0f

                        if (x + 1 < w) buffer[y][x + 1] += err
                        if (x + 2 < w) buffer[y][x + 2] += err
                        if (y + 1 < h) {
                            if (x - 1 >= 0) buffer[y + 1][x - 1] += err
                            buffer[y + 1][x] += err
                            if (x + 1 < w) buffer[y + 1][x + 1] += err
                        }
                        if (y + 2 < h) {
                            buffer[y + 2][x] += err
                        }
                    }
                }
            }
            DitherMode.GRAYSCALE_PWM -> {
                for (y in 0 until h) {
                    for (x in 0 until w) {
                        val powerFactor = 1f - gray[y][x]
                        outputGray[y][x] = powerFactor
                        binary[y][x] = powerFactor > 0.05f
                    }
                }
            }
        }

        return ProcessedImageResult(binary, outputGray, w, h)
    }

    /**
     * Generates bidirectional raster scanline vectors from binary image matrix
     */
    fun generateScanlines(
        image: LaserShape.RasterImage,
        matrix: Array<BooleanArray>,
        lineIntervalMm: Float
    ): List<List<Point2D>> {
        val lines = mutableListOf<List<Point2D>>()
        val h = matrix.size
        if (h == 0) return lines
        val w = matrix[0].size
        if (w == 0) return lines

        val pixelWidthMm = image.width / w.toFloat()
        val pixelHeightMm = image.height / h.toFloat()

        for (row in 0 until h) {
            val yMm = image.y + (row * pixelHeightMm)
            var inSegment = false
            var segStart = 0

            val isLeftToRight = (row % 2 == 0)
            val rowLine = mutableListOf<Point2D>()

            if (isLeftToRight) {
                for (col in 0 until w) {
                    val burn = matrix[row][col]
                    if (burn && !inSegment) {
                        inSegment = true
                        segStart = col
                    } else if (!burn && inSegment) {
                        inSegment = false
                        val x1 = image.x + (segStart * pixelWidthMm)
                        val x2 = image.x + (col * pixelWidthMm)
                        lines.add(listOf(Point2D(x1, yMm), Point2D(x2, yMm)))
                    }
                }
                if (inSegment) {
                    val x1 = image.x + (segStart * pixelWidthMm)
                    val x2 = image.x + (w * pixelWidthMm)
                    lines.add(listOf(Point2D(x1, yMm), Point2D(x2, yMm)))
                }
            } else {
                for (col in (w - 1) downTo 0) {
                    val burn = matrix[row][col]
                    if (burn && !inSegment) {
                        inSegment = true
                        segStart = col
                    } else if (!burn && inSegment) {
                        inSegment = false
                        val x1 = image.x + ((segStart + 1) * pixelWidthMm)
                        val x2 = image.x + ((col + 1) * pixelWidthMm)
                        lines.add(listOf(Point2D(x1, yMm), Point2D(x2, yMm)))
                    }
                }
                if (inSegment) {
                    val x1 = image.x + ((segStart + 1) * pixelWidthMm)
                    val x2 = image.x
                    lines.add(listOf(Point2D(x1, yMm), Point2D(x2, yMm)))
                }
            }
        }

        return lines
    }
}
