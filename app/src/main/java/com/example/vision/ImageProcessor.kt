package com.example.vision

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

enum class ScanFilter(val label: String) {
    ORIGINAL("Original"),
    AUTO("Auto"),
    BW("B&W"),
    GRAYSCALE("Grayscale"),
    MAGIC_COLOR("Magic Color"),
}

data class NormalizedQuad(
    val topLeft: PointF = PointF(0.08f, 0.08f),
    val topRight: PointF = PointF(0.92f, 0.08f),
    val bottomRight: PointF = PointF(0.92f, 0.92f),
    val bottomLeft: PointF = PointF(0.08f, 0.92f),
)

object ImageProcessor {

    /**
     * Lightweight on-device edge energy estimator to propose initial document boundary quad.
     */
    fun estimateDocumentQuad(bitmap: Bitmap): NormalizedQuad {
        val w = bitmap.width
        val h = bitmap.height
        if (w < 32 || h < 32) return NormalizedQuad()

        val scaledW = 96
        val scaledH = max(32, (h.toFloat() / w.toFloat() * scaledW).roundToInt())
        val small = Bitmap.createScaledBitmap(bitmap, scaledW, scaledH, true)

        var minX = scaledW
        var minY = scaledH
        var maxX = 0
        var maxY = 0
        var edgeHits = 0

        for (y in 2 until scaledH - 2) {
            for (x in 2 until scaledW - 2) {
                val c = small.getPixel(x, y)
                val r = small.getPixel(x + 1, y)
                val d = small.getPixel(x, y + 1)
                val lumC = (((c shr 16) and 0xFF) * 3 + ((c shr 8) and 0xFF) * 6 + (c and 0xFF)) / 10
                val lumR = (((r shr 16) and 0xFF) * 3 + ((r shr 8) and 0xFF) * 6 + (r and 0xFF)) / 10
                val lumD = (((d shr 16) and 0xFF) * 3 + ((d shr 8) and 0xFF) * 6 + (d and 0xFF)) / 10
                val grad = kotlin.math.abs(lumC - lumR) + kotlin.math.abs(lumC - lumD)
                if (grad > 48) {
                    edgeHits++
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        if (small !== bitmap) small.recycle()

        if (edgeHits < 25 || maxX - minX < scaledW / 4 || maxY - minY < scaledH / 4) {
            return NormalizedQuad()
        }

        val left = (minX.toFloat() / scaledW).coerceIn(0.04f, 0.35f)
        val right = (maxX.toFloat() / scaledW).coerceIn(0.65f, 0.96f)
        val top = (minY.toFloat() / scaledH).coerceIn(0.04f, 0.35f)
        val bottom = (maxY.toFloat() / scaledH).coerceIn(0.65f, 0.96f)

        return NormalizedQuad(
            topLeft = PointF(left, top),
            topRight = PointF(right, top),
            bottomRight = PointF(right, bottom),
            bottomLeft = PointF(left, bottom),
        )
    }

    /**
     * Applies 4-point perspective correction using Matrix.setPolyToPoly.
     */
    fun perspectiveWarp(bitmap: Bitmap, quad: NormalizedQuad): Bitmap {
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        val src = floatArrayOf(
            quad.topLeft.x * w, quad.topLeft.y * h,
            quad.topRight.x * w, quad.topRight.y * h,
            quad.bottomRight.x * w, quad.bottomRight.y * h,
            quad.bottomLeft.x * w, quad.bottomLeft.y * h,
        )

        val topWidth = hypot((src[2] - src[0]).toDouble(), (src[3] - src[1]).toDouble())
        val bottomWidth = hypot((src[4] - src[6]).toDouble(), (src[5] - src[7]).toDouble())
        val leftHeight = hypot((src[6] - src[0]).toDouble(), (src[7] - src[1]).toDouble())
        val rightHeight = hypot((src[4] - src[2]).toDouble(), (src[5] - src[3]).toDouble())

        val dstW = max(200, max(topWidth, bottomWidth).roundToInt())
        val dstH = max(200, max(leftHeight, rightHeight).roundToInt())

        val dst = floatArrayOf(
            0f, 0f,
            dstW.toFloat(), 0f,
            dstW.toFloat(), dstH.toFloat(),
            0f, dstH.toFloat(),
        )

        val matrix = Matrix()
        matrix.setPolyToPoly(src, 0, dst, 0, 4)

        val output = Bitmap.createBitmap(dstW, dstH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, matrix, paint)
        return output
    }

    fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        val norm = ((degrees % 360) + 360) % 360
        if (norm == 0) return bitmap
        val matrix = Matrix().apply { postRotate(norm.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun applyFilterAndAdjustments(
        source: Bitmap,
        filter: ScanFilter,
        brightness: Float = 0f, // -60f .. +60f
        contrast: Float = 1f,   // 0.6f .. 1.8f
    ): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val matrix = ColorMatrix()

        when (filter) {
            ScanFilter.ORIGINAL -> Unit
            ScanFilter.AUTO -> {
                val c = contrast * 1.18f
                val t = (-0.5f * c + 0.5f) * 255f + 12f
                matrix.postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            c, 0f, 0f, 0f, t,
                            0f, c, 0f, 0f, t,
                            0f, 0f, c, 0f, t,
                            0f, 0f, 0f, 1f, 0f,
                        ),
                    ),
                )
            }
            ScanFilter.GRAYSCALE -> {
                matrix.setSaturation(0f)
            }
            ScanFilter.BW -> {
                matrix.setSaturation(0f)
                val c = 2.6f * contrast
                val t = (-0.5f * c + 0.5f) * 255f + 24f
                matrix.postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            c, 0f, 0f, 0f, t,
                            0f, c, 0f, 0f, t,
                            0f, 0f, c, 0f, t,
                            0f, 0f, 0f, 1f, 0f,
                        ),
                    ),
                )
            }
            ScanFilter.MAGIC_COLOR -> {
                val sat = ColorMatrix().apply { setSaturation(1.38f) }
                matrix.postConcat(sat)
                val c = 1.24f * contrast
                val t = (-0.5f * c + 0.5f) * 255f + 14f
                matrix.postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            c, 0f, 0f, 0f, t,
                            0f, c, 0f, 0f, t,
                            0f, 0f, c, 0f, t,
                            0f, 0f, 0f, 1f, 0f,
                        ),
                    ),
                )
            }
        }

        if (filter == ScanFilter.ORIGINAL || filter == ScanFilter.GRAYSCALE) {
            val t = (-0.5f * contrast + 0.5f) * 255f + brightness
            matrix.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, t,
                        0f, contrast, 0f, 0f, t,
                        0f, 0f, contrast, 0f, t,
                        0f, 0f, 0f, 1f, 0f,
                    ),
                ),
            )
        } else if (brightness != 0f) {
            matrix.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, brightness,
                        0f, 1f, 0f, 0f, brightness,
                        0f, 0f, 1f, 0f, brightness,
                        0f, 0f, 0f, 1f, 0f,
                    ),
                ),
            )
        }

        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return out
    }
}
