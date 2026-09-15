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

// Bảng màu xe Mazda Soul Red Crystal & Chi tiết Batman / Carbon
private val CAR_RED_BODY = Color(0xFFC0151D)
private val CAR_RED_DARK = Color(0xFF7A090E)
private val CAR_RED_HIGHLIGHT = Color(0xFFE82830)
private val CAR_GLASS = Color(0xFF1B232A)
private val CAR_TIRE = Color(0xFF151515)
private val CAR_TAILLIGHT = Color(0xFFFF1E27)
private val GOLD_EMBLEM = Color(0xFFFFF0B8)

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
        // 1. Mặt đường 3D chuyển động
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

            // Viền mép đường màu vàng ánh kim
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

            // Vạch đứt tim đường
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

        // 2. Vẽ Xe Mazda CX-5 Thể thao siêu nét (Vị trí cách Dock 115dp - rút ngắn 1/2 khoảng cách)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 115.dp)
                .width(320.dp)
                .height(210.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawSharpMazdaCar()
            }
        }
    }
}

// Hàm vẽ xe Mazda CX-5 vector siêu nét
private fun DrawScope.drawSharpMazdaCar() {
    val w = size.width
    val h = size.height

    // Bóng đổ gầm xe
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent),
            center = Offset(w * 0.5f, h * 0.92f),
            radius = w * 0.46f
        ),
        topLeft = Offset(w * 0.08f, h * 0.82f),
        size = Size(w * 0.84f, h * 0.18f)
    )

    // 2 Bánh lốp xe
    drawRoundRect(
        color = CAR_TIRE,
        topLeft = Offset(w * 0.18f, h * 0.68f),
        size = Size(w * 0.13f, h * 0.26f),
        cornerRadius = CornerRadius(10f, 10f)
    )
    drawRoundRect(
        color = CAR_TIRE,
        topLeft = Offset(w * 0.69f, h * 0.68f),
        size = Size(w * 0.13f, h * 0.26f),
        cornerRadius = CornerRadius(10f, 10f)
    )

    // Khung thân dưới xe (Body)
    val bodyPath = Path().apply {
        moveTo(w * 0.18f, h * 0.72f)
        cubicTo(w * 0.12f, h * 0.52f, w * 0.14f, h * 0.40f, w * 0.24f, h * 0.35f)
        lineTo(w * 0.76f, h * 0.35f)
        cubicTo(w * 0.86f, h * 0.40f, w * 0.88f, h * 0.52f, w * 0.82f, h * 0.72f)
        lineTo(w * 0.76f, h * 0.82f)
        lineTo(w * 0.24f, h * 0.82f)
        close()
    }
    drawPath(
        path = bodyPath,
        brush = Brush.verticalGradient(listOf(CAR_RED_HIGHLIGHT, CAR_RED_BODY, CAR_RED_DARK))
    )

    // Kính hậu phía sau (Cabin Window)
    val windowPath = Path().apply {
        moveTo(w * 0.28f, h * 0.35f)
        lineTo(w * 0.34f, h * 0.16f)
        lineTo(w * 0.66f, h * 0.16f)
        lineTo(w * 0.72f, h * 0.35f)
        close()
    }
    drawPath(
        path = windowPath,
        brush = Brush.verticalGradient(listOf(Color(0xFF0F1418), CAR_GLASS))
    )

    // Cánh gió đuôi thể thao & Cần gạt nước
    drawLine(
        color = Color(0xFF0C0B0A),
        start = Offset(w * 0.32f, h * 0.15f),
        end = Offset(w * 0.68f, h * 0.15f),
        strokeWidth = 6f
    )

    // Cụm đèn hậu LED Mazda góc cạnh sắc sảo
    val leftLight = Path().apply {
        moveTo(w * 0.15f, h * 0.43f)
        lineTo(w * 0.36f, h * 0.45f)
        lineTo(w * 0.34f, h * 0.49f)
        lineTo(w * 0.17f, h * 0.47f)
        close()
    }
    drawPath(leftLight, color = CAR_TAILLIGHT)

    val rightLight = Path().apply {
        moveTo(w * 0.85f, h * 0.43f)
        lineTo(w * 0.64f, h * 0.45f)
        lineTo(w * 0.66f, h * 0.49f)
        lineTo(w * 0.83f, h * 0.47f)
        close()
    }
    drawPath(rightLight, color = CAR_TAILLIGHT)

    // Biểu tượng Logo Mazda cánh chim trung tâm
    drawCircle(
        color = GOLD_EMBLEM,
        radius = 12f,
        center = Offset(w * 0.5f, h * 0.46f)
    )
    drawCircle(
        color = CAR_RED_BODY,
        radius = 8f,
        center = Offset(w * 0.5f, h * 0.46f)
    )

    // Biển số xe chữ nhật trắng viền đen
    drawRoundRect(
        color = Color(0xFFF2F2F2),
        topLeft = Offset(w * 0.38f, h * 0.54f),
        size = Size(w * 0.24f, h * 0.13f),
        cornerRadius = CornerRadius(6f, 6f)
    )

    // Viền đen quanh biển số
    drawRoundRect(
        color = Color(0xFF1E1E1E),
        topLeft = Offset(w * 0.38f, h * 0.54f),
        size = Size(w * 0.24f, h * 0.13f),
        cornerRadius = CornerRadius(6f, 6f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
    )

    // Chữ biển số: 79A - 137.73 in đậm sắc nét
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 24f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.nativeCanvas.drawText("79A - 137.73", w * 0.5f, h * 0.625f, paint)
    }

    // Ống xả đôi thể thao mạ kim loại
    drawCircle(color = Color(0xFF888888), radius = 7f, center = Offset(w * 0.26f, h * 0.82f))
    drawCircle(color = Color(0xFF111111), radius = 4.5f, center = Offset(w * 0.26f, h * 0.82f))

    drawCircle(color = Color(0xFF888888), radius = 7f, center = Offset(w * 0.74f, h * 0.82f))
    drawCircle(color = Color(0xFF111111), radius = 4.5f, center = Offset(w * 0.74f, h * 0.82f))
}
