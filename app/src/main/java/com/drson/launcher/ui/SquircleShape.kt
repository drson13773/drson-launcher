package com.drson.launcher.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.pow

/**
 * "Squircle" - hình dạng bo góc đặc trưng của iPadOS/iOS (superellipse),
 * mượt hơn RoundedCornerShape thông thường.
 *
 * smoothing: 0f..1f, giá trị càng cao càng gần với continuous-corner của Apple (~0.6 là hợp lý).
 */
class SquircleShape(private val smoothing: Float = 0.6f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val w = size.width
        val h = size.height
        val n = 2.0 + smoothing * 3.0 // exponent của superellipse, ~2 (oval) .. 5 (gần vuông)

        val steps = 90
        for (i in 0..steps) {
            val t = (i.toDouble() / steps) * (2 * Math.PI)
            val cosT = Math.cos(t)
            val sinT = Math.sin(t)
            val x = (w / 2) * signedPow(cosT, n)
            val y = (h / 2) * signedPow(sinT, n)
            val px = (w / 2 + x).toFloat()
            val py = (h / 2 + y).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        return Outline.Generic(path)
    }

    private fun signedPow(value: Double, exponent: Double): Double {
        val sign = if (value < 0) -1.0 else 1.0
        return sign * Math.abs(value).pow(2.0 / exponent)
    }
}
