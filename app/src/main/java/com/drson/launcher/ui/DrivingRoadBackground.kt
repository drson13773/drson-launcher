package com.drson.launcher.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp

private val SKY_TOP = Color(0xFF14120F)
private val SKY_BOTTOM = Color(0xFF2C251C)
private val ROAD_DARK = Color(0xFF1E1C1A)
private val ROAD_EDGE = Color(0xFFD4AF37).copy(alpha = 0.5f)
private val ROAD_LANE = Color(0xFFFFF0B8).copy(alpha = 0.8f)

// Bảng màu Mazda CX-5 Soul Red Crystal đa tầng
private val RED_HIGHLIGHT = Color(0xFFE5242D)
private val RED_MAIN = Color(0xFFB8141B)
private val RED_DARK = Color(0xFF6B0B10)
private val RED_SHADOW = Color(0xFF420508)
private val BUMPER_BLACK = Color(0xFF181716)
private val GLASS_GRADIENT_TOP = Color(0xFF0D1217)
private val GLASS_GRADIENT_BOTTOM = Color(0xFF1D2833)
private val TAIL_LIGHT_ON = Color(0xFFFF1B25)
private val TAIL_LIGHT_DARK = Color(0xFF7A050A)
private val CHROME_GOLD = Color(0xFFFFF0B8)

@Composable
fun DrivingRoadBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "road_anim")
    val laneOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "lane_offset"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SKY_TOP, SKY_BOTTOM)))
    ) {
        // 1. Phối cảnh đường chạy 3D
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val horizonY = h * 0.44f

            val roadPath = Path().apply {
                moveTo(w * 0.45f, horizonY)
                lineTo(w * 0.55f, horizonY)
                lineTo(w * 0.82f, h)
                lineTo(w * 0.18f, h)
                close()
            }
            drawPath(
                path = roadPath,
                brush = Brush.verticalGradient(
                    colors = listOf(ROAD_DARK.copy(alpha = 0.85f), ROAD_DARK),
                    startY = horizonY,
                    endY = h
                )
            )

            drawLine(
                color = ROAD_EDGE,
                start = Offset(w * 0.45f, horizonY),
                end = Offset(w * 0.18f, h),
                strokeWidth = 3f
            )
            drawLine(
                color = ROAD_EDGE,
                start = Offset(w * 0.55f, horizonY),
                end = Offset(w * 0.82f, h),
                strokeWidth = 3f
            )

            val laneCount = 5
            for (i in 0..laneCount) {
                val progress = (i.toFloat() / laneCount + laneOffset * (1f / laneCount)) % 1f
                val startY = horizonY + (h - horizonY) * (progress * progress)
                val endY = startY + 28f * (progress + 0.3f)
                if (endY <= h) {
                    drawLine(
                        color = ROAD_LANE,
                        start = Offset(w * 0.5f, startY),
                        end = Offset(w * 0.5f, endY),
                        strokeWidth = 4f + progress * 6f
                    )
                }
            }
        }

        // 2. Mazda CX-5 Vector chi tiết cao (khoảng cách cân đối, sắc nét)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 110.dp)
                .width(360.dp)
                .height(230.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawDetailedMazdaCX5()
            }
        }
    }
}

private fun DrawScope.drawDetailedMazdaCX5() {
    val w = size.width
    val h = size.height

    // Đổ bóng thực tế gầm xe
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent),
            center = Offset(w * 0.5f, h * 0.88f),
            radius = w * 0.48f
        ),
        topLeft = Offset(w * 0.05f, h * 0.78f),
        size = Size(w * 0.90f, h * 0.20f)
    )

    // Bánh lốp sau (Gai lốp thể thao rộng)
    drawRoundRect(
        color = Color(0xFF111111),
        topLeft = Offset(w * 0.12f, h * 0.62f),
        size = Size(w * 0.14f, h * 0.28f),
        cornerRadius = CornerRadius(12f, 12f)
    )
    drawRoundRect(
        color = Color(0xFF111111),
        topLeft = Offset(w * 0.74f, h * 0.62f),
        size = Size(w * 0.14f, h * 0.28f),
        cornerRadius = CornerRadius(12f, 12f)
    )

    // Cản sau & ốp gầm nhựa sần thể thao (Rear Diffuser)
    val diffuserPath = Path().apply {
        moveTo(w * 0.14f, h * 0.78f)
        lineTo(w * 0.18f, h * 0.65f)
        lineTo(w * 0.82f, h * 0.65f)
        lineTo(w * 0.86f, h * 0.78f)
        lineTo(w * 0.82f, h * 0.86f)
        lineTo(w * 0.18f, h * 0.86f)
        close()
    }
    drawPath(diffuserPath, color = BUMPER_BLACK)

    // Ống xả đôi Chrome đối xứng hai bên
    drawCircle(color = Color(0xFFAAAAAA), radius = 8f, center = Offset(w * 0.23f, h * 0.82f))
    drawCircle(color = Color(0xFF000000), radius = 5.5f, center = Offset(w * 0.23f, h * 0.82f))

    drawCircle(color = Color(0xFFAAAAAA), radius = 8f, center = Offset(w * 0.77f, h * 0.82f))
    drawCircle(color = Color(0xFF000000), radius = 5.5f, center = Offset(w * 0.77f, h * 0.82f))

    // Thân xe chính (Soul Red Crystal Dynamic Body)
    val bodyPath = Path().apply {
        moveTo(w * 0.15f, h * 0.66f)
        cubicTo(w * 0.11f, h * 0.50f, w * 0.13f, h * 0.38f, w * 0.24f, h * 0.34f)
        lineTo(w * 0.76f, h * 0.34f)
        cubicTo(w * 0.87f, h * 0.38f, w * 0.89f, h * 0.50f, w * 0.85f, h * 0.66f)
        lineTo(w * 0.82f, h * 0.76f)
        lineTo(w * 0.18f, h * 0.76f)
        close()
    }
    drawPath(
        path = bodyPath,
        brush = Brush.verticalGradient(
            listOf(RED_HIGHLIGHT, RED_MAIN, RED_DARK, RED_SHADOW)
        )
    )

    // Cột C & Kính sau khí động học (Aerodynamic Rear Window)
    val glassPath = Path().apply {
        moveTo(w * 0.27f, h * 0.34f)
        cubicTo(w * 0.30f, h * 0.20f, w * 0.33f, h * 0.14f, w * 0.37f, h * 0.13f)
        lineTo(w * 0.63f, h * 0.13f)
        cubicTo(w * 0.67f, h * 0.20f, w * 0.70f, h * 0.20f, w * 0.73f, h * 0.34f)
        close()
    }
    drawPath(
        path = glassPath,
        brush = Brush.verticalGradient(listOf(GLASS_GRADIENT_TOP, GLASS_GRADIENT_BOTTOM))
    )

    // Cánh gió mui xe (Roof Spoiler) + Đèn phanh trên cao
    val spoilerPath = Path().apply {
        moveTo(w * 0.34f, h * 0.12f)
        lineTo(w * 0.66f, h * 0.12f)
        lineTo(w * 0.64f, h * 0.15f)
        lineTo(w * 0.36f, h * 0.15f)
        close()
    }
    drawPath(spoilerPath, color = RED_DARK)
    drawLine(
        color = Color(0xFFFF2222),
        start = Offset(w * 0.45f, h * 0.14f),
        end = Offset(w * 0.55f, h * 0.14f),
        strokeWidth = 3f
    )

    // Gương chiếu hậu 2 bên (Side Mirrors)
    drawRoundRect(
        brush = Brush.horizontalGradient(listOf(RED_DARK, RED_MAIN)),
        topLeft = Offset(w * 0.08f, h * 0.38f),
        size = Size(w * 0.11f, h * 0.08f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawRoundRect(
        brush = Brush.horizontalGradient(listOf(RED_MAIN, RED_DARK)),
        topLeft = Offset(w * 0.81f, h * 0.38f),
        size = Size(w * 0.11f, h * 0.08f),
        cornerRadius = CornerRadius(8f, 8f)
    )

    // Cụm đèn hậu LED KODO sắc lẹm (Mazda KODO Signature Taillights)
    val leftTaillight = Path().apply {
        moveTo(w * 0.14f, h * 0.44f)
        lineTo(w * 0.37f, h * 0.46f)
        lineTo(w * 0.35f, h * 0.52f)
        lineTo(w * 0.18f, h * 0.50f)
        close()
    }
    drawPath(
        path = leftTaillight,
        brush = Brush.horizontalGradient(listOf(TAIL_LIGHT_ON, TAIL_LIGHT_DARK))
    )

    val rightTaillight = Path().apply {
        moveTo(w * 0.86f, h * 0.44f)
        lineTo(w * 0.63f, h * 0.46f)
        lineTo(w * 0.65f, h * 0.52f)
        lineTo(w * 0.82f, h * 0.50f)
        close()
    }
    drawPath(
        path = rightTaillight,
        brush = Brush.horizontalGradient(listOf(TAIL_LIGHT_DARK, TAIL_LIGHT_ON))
    )

    // Dải Chrome nối cốp & Logo Mazda mạ Chrome Vàng/Bạc
    drawCircle(color = CHROME_GOLD, radius = 13f, center = Offset(w * 0.5f, h * 0.46f))
    drawCircle(color = RED_MAIN, radius = 9.5f, center = Offset(w * 0.5f, h * 0.46f))

    // Hốc gắn biển số cách điệu
    val plateHolderPath = Path().apply {
        moveTo(w * 0.36f, h * 0.56f)
        lineTo(w * 0.64f, h * 0.56f)
        lineTo(w * 0.62f, h * 0.70f)
        lineTo(w * 0.38f, h * 0.70f)
        close()
    }
    drawPath(plateHolderPath, color = RED_SHADOW)

    // Biển số nền trắng viền đen
    drawRoundRect(
        color = Color(0xFFF8F8F8),
        topLeft = Offset(w * 0.39f, h * 0.58f),
        size = Size(w * 0.22f, h * 0.10f),
        cornerRadius = CornerRadius(5f, 5f)
    )
    drawRoundRect(
        color = Color(0xFF1E1E1E),
        topLeft = Offset(w * 0.39f, h * 0.58f),
        size = Size(w * 0.22f, h * 0.10f),
        cornerRadius = CornerRadius(5f, 5f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
    )

    // Dập nổi số xe: 79A - 137.73
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 21f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.nativeCanvas.drawText("79A - 137.73", w * 0.5f, h * 0.655f, paint)
    }

    // Đèn phản quang đỏ phía dưới cản
    drawRoundRect(color = Color(0xFFFF2222), topLeft = Offset(w * 0.20f, h * 0.72f), size = Size(w * 0.08f, 6f), cornerRadius = CornerRadius(3f, 3f))
    drawRoundRect(color = Color(0xFFFF2222), topLeft = Offset(w * 0.72f, h * 0.72f), size = Size(w * 0.08f, 6f), cornerRadius = CornerRadius(3f, 3f))
}
