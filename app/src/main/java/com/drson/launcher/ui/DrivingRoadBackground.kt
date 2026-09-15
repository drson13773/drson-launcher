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

private val SKY_TOP = Color(0xFF100E0C)
private val SKY_BOTTOM = Color(0xFF262018)
private val ROAD_COLOR = Color(0xFF1C1A17)
private val ROAD_EDGE = Color(0xFFD4AF37).copy(alpha = 0.5f)
private val ROAD_LANE = Color(0xFFFFF0B8).copy(alpha = 0.85f)

// Màu sắc Mazda CX-5 Soul Red Crystal đa tầng
private val CX5_RED_LIGHT = Color(0xFFDC242C)
private val CX5_RED_MAIN = Color(0xFFB31219)
private val CX5_RED_DARK = Color(0xFF6E0A0F)
private val CX5_RED_DEEP = Color(0xFF450508)
private val CX5_GLASS_TOP = Color(0xFF0F151B)
private val CX5_GLASS_BOT = Color(0xFF202C38)
private val CX5_TAILLIGHT = Color(0xFFFF1E27)
private val CX5_TAILLIGHT_INNER = Color(0xFF78080E)

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
        // 1. Toàn bộ nền: Dãy tòa nhà thành phố + Mặt đường 3D chuyển động
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val horizonY = h * 0.45f

            // A. Dãy tòa nhà thành phố với các ô cửa sổ sáng đèn (Skyline ban đêm)
            drawCitySkyline(w, horizonY)

            // B. Mặt đường 3D
            val roadPath = Path().apply {
                moveTo(w * 0.44f, horizonY)
                lineTo(w * 0.56f, horizonY)
                lineTo(w * 0.84f, h)
                lineTo(w * 0.16f, h)
                close()
            }
            drawPath(
                path = roadPath,
                brush = Brush.verticalGradient(
                    colors = listOf(ROAD_COLOR.copy(alpha = 0.9f), ROAD_COLOR),
                    startY = horizonY,
                    endY = h
                )
            )

            // Viền vàng 2 bên làn đường
            drawLine(
                color = ROAD_EDGE,
                start = Offset(w * 0.44f, horizonY),
                end = Offset(w * 0.16f, h),
                strokeWidth = 3f
            )
            drawLine(
                color = ROAD_EDGE,
                start = Offset(w * 0.56f, horizonY),
                end = Offset(w * 0.84f, h),
                strokeWidth = 3f
            )

            // Vạch kẻ đứt tim đường chuyển động
            val laneCount = 5
            for (i in 0..laneCount) {
                val progress = (i.toFloat() / laneCount + laneOffset * (1f / laneCount)) % 1f
                val startY = horizonY + (h - horizonY) * (progress * progress)
                val endY = startY + 30f * (progress + 0.35f)
                if (endY <= h) {
                    drawLine(
                        color = ROAD_LANE,
                        start = Offset(w * 0.5f, startY),
                        end = Offset(w * 0.5f, endY),
                        strokeWidth = 4f + progress * 7f
                    )
                }
            }
        }

        // 2. Xe Mazda CX-5 nguyên bản sắc nét, vị trí hạ xuống 1/2 khoảng cách tới Dock
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 76.dp) // Vị trí hạ thấp cân đối sát ngay trên Dock 64dp
                .width(330.dp)
                .height(215.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawOriginalDetailedMazda()
            }
        }
    }
}

// Vẽ dãy tòa nhà thành phố với cửa sổ vàng
private fun DrawScope.drawCitySkyline(w: Float, horizonY: Float) {
    val buildingColor = Color(0xFF0A0908)
    val windowColor = Color(0xFFFFD700).copy(alpha = 0.75f)

    val buildings = listOf(
        Triple(0.08f, 0.08f, 50f),
        Triple(0.18f, 0.09f, 75f),
        Triple(0.28f, 0.07f, 60f),
        Triple(0.38f, 0.06f, 45f),
        Triple(0.48f, 0.10f, 85f),
        Triple(0.60f, 0.07f, 55f),
        Triple(0.70f, 0.09f, 70f),
        Triple(0.82f, 0.08f, 65f),
        Triple(0.91f, 0.07f, 40f)
    )

    for ((leftRatio, widthRatio, bHeight) in buildings) {
        val bLeft = w * leftRatio
        val bWidth = w * widthRatio
        val bTop = horizonY - bHeight

        // Khối tòa nhà đen
        drawRect(
            color = buildingColor,
            topLeft = Offset(bLeft, bTop),
            size = Size(bWidth, bHeight)
        )

        // Các ô cửa sổ sáng đèn
        var winY = bTop + 8f
        while (winY < horizonY - 10f) {
            var winX = bLeft + 6f
            while (winX < bLeft + bWidth - 8f) {
                if (((winX + winY).toInt() % 3) != 0) {
                    drawRect(
                        color = windowColor,
                        topLeft = Offset(winX, winY),
                        size = Size(5f, 4f)
                    )
                }
                winX += 10f
            }
            winY += 12f
        }
    }
}

// Vẽ Mazda CX-5 nguyên bản chuẩn tỉ lệ và sắc nét
private fun DrawScope.drawOriginalDetailedMazda() {
    val w = size.width
    val h = size.height

    // 1. Bóng gầm xe
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent),
            center = Offset(w * 0.5f, h * 0.88f),
            radius = w * 0.46f
        ),
        topLeft = Offset(w * 0.08f, h * 0.78f),
        size = Size(w * 0.84f, h * 0.20f)
    )

    // 2. Hai bánh lốp sau
    drawRoundRect(
        color = Color(0xFF141414),
        topLeft = Offset(w * 0.14f, h * 0.62f),
        size = Size(w * 0.12f, h * 0.26f),
        cornerRadius = CornerRadius(10f, 10f)
    )
    drawRoundRect(
        color = Color(0xFF141414),
        topLeft = Offset(w * 0.74f, h * 0.62f),
        size = Size(w * 0.12f, h * 0.26f),
        cornerRadius = CornerRadius(10f, 10f)
    )

    // 3. Cản sau thể thao (Rear Bumper Diffuser)
    val bumperPath = Path().apply {
        moveTo(w * 0.16f, h * 0.74f)
        lineTo(w * 0.20f, h * 0.66f)
        lineTo(w * 0.80f, h * 0.66f)
        lineTo(w * 0.84f, h * 0.74f)
        lineTo(w * 0.80f, h * 0.84f)
        lineTo(w * 0.20f, h * 0.84f)
        close()
    }
    drawPath(bumperPath, color = Color(0xFF181716))

    // 4. Ống xả kép mạ chrome
    drawCircle(color = Color(0xFFA0A0A0), radius = 7.5f, center = Offset(w * 0.24f, h * 0.80f))
    drawCircle(color = Color(0xFF000000), radius = 5f, center = Offset(w * 0.24f, h * 0.80f))
    drawCircle(color = Color(0xFFA0A0A0), radius = 7.5f, center = Offset(w * 0.76f, h * 0.80f))
    drawCircle(color = Color(0xFF000000), radius = 5f, center = Offset(w * 0.76f, h * 0.80f))

    // 5. Thân xe Mazda Soul Red
    val bodyPath = Path().apply {
        moveTo(w * 0.17f, h * 0.64f)
        cubicTo(w * 0.13f, h * 0.48f, w * 0.15f, h * 0.36f, w * 0.25f, h * 0.32f)
        lineTo(w * 0.75f, h * 0.32f)
        cubicTo(w * 0.85f, h * 0.36f, w * 0.87f, h * 0.48f, w * 0.83f, h * 0.64f)
        lineTo(w * 0.80f, h * 0.74f)
        lineTo(w * 0.20f, h * 0.74f)
        close()
    }
    drawPath(
        path = bodyPath,
        brush = Brush.verticalGradient(listOf(CX5_RED_LIGHT, CX5_RED_MAIN, CX5_RED_DARK, CX5_RED_DEEP))
    )

    // 6. Kính hậu (Rear Windshield)
    val glassPath = Path().apply {
        moveTo(w * 0.28f, h * 0.32f)
        cubicTo(w * 0.31f, h * 0.20f, w * 0.34f, h * 0.14f, w * 0.38f, h * 0.13f)
        lineTo(w * 0.62f, h * 0.13f)
        cubicTo(w * 0.66f, h * 0.20f, w * 0.69f, h * 0.20f, w * 0.72f, h * 0.32f)
        close()
    }
    drawPath(
        path = glassPath,
        brush = Brush.verticalGradient(listOf(CX5_GLASS_TOP, CX5_GLASS_BOT))
    )

    // 7. Cánh lướt gió đuôi (Spoiler)
    val spoilerPath = Path().apply {
        moveTo(w * 0.35f, h * 0.12f)
        lineTo(w * 0.65f, h * 0.12f)
        lineTo(w * 0.63f, h * 0.15f)
        lineTo(w * 0.37f, h * 0.15f)
        close()
    }
    drawPath(spoilerPath, color = CX5_RED_DARK)
    drawLine(
        color = Color(0xFFFF2222),
        start = Offset(w * 0.46f, h * 0.135f),
        end = Offset(w * 0.54f, h * 0.135f),
        strokeWidth = 2.5f
    )

    // 8. Cụm đèn hậu LED KODO
    val leftLight = Path().apply {
        moveTo(w * 0.16f, h * 0.42f)
        lineTo(w * 0.37f, h * 0.44f)
        lineTo(w * 0.35f, h * 0.50f)
        lineTo(w * 0.19f, h * 0.48f)
        close()
    }
    drawPath(leftLight, brush = Brush.horizontalGradient(listOf(CX5_TAILLIGHT, CX5_TAILLIGHT_INNER)))

    val rightLight = Path().apply {
        moveTo(w * 0.84f, h * 0.42f)
        lineTo(w * 0.63f, h * 0.44f)
        lineTo(w * 0.65f, h * 0.50f)
        lineTo(w * 0.81f, h * 0.48f)
        close()
    }
    drawPath(rightLight, brush = Brush.horizontalGradient(listOf(CX5_TAILLIGHT_INNER, CX5_TAILLIGHT)))

    // 9. Logo Mazda Chrome trung tâm
    drawCircle(color = Color(0xFFFFF0B8), radius = 11f, center = Offset(w * 0.5f, h * 0.44f))
    drawCircle(color = CX5_RED_MAIN, radius = 8f, center = Offset(w * 0.5f, h * 0.44f))

    // 10. Biển số xe Mazda: 79A - 137.73
    drawRoundRect(
        color = Color(0xFFF6F6F6),
        topLeft = Offset(w * 0.39f, h * 0.54f),
        size = Size(w * 0.22f, h * 0.11f),
        cornerRadius = CornerRadius(5f, 5f)
    )
    drawRoundRect(
        color = Color(0xFF1E1E1E),
        topLeft = Offset(w * 0.39f, h * 0.54f),
        size = Size(w * 0.22f, h * 0.11f),
        cornerRadius = CornerRadius(5f, 5f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
    )

    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 20f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.nativeCanvas.drawText("79A - 137.73", w * 0.5f, h * 0.62f, paint)
    }

    // Đèn phản quang đỏ
    drawRoundRect(color = Color(0xFFFF2222), topLeft = Offset(w * 0.21f, h * 0.70f), size = Size(w * 0.08f, 5f), cornerRadius = CornerRadius(2f, 2f))
    drawRoundRect(color = Color(0xFFFF2222), topLeft = Offset(w * 0.71f, h * 0.70f), size = Size(w * 0.08f, 5f), cornerRadius = CornerRadius(2f, 2f))
}
