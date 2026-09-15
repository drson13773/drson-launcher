package com.drson.launcher.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.drson.launcher.R

private val SKY_TOP = Color(0xFF100E0C)
private val SKY_BOTTOM = Color(0xFF262018)
private val ROAD_COLOR = Color(0xFF1C1A17)
private val ROAD_EDGE = Color(0xFFD4AF37).copy(alpha = 0.5f)
private val ROAD_LANE = Color(0xFFFFF0B8).copy(alpha = 0.85f)

@Composable
fun DrivingRoadBackground(modifier: Modifier = Modifier) {
    // Hiệu ứng vạch tim đường chuyển động vô tận
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
        // 1. Phối cảnh bầu trời đêm, tòa nhà thành phố và mặt đường 3D
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val horizonY = h * 0.45f

            // Dãy tòa nhà ánh đèn vàng ban đêm
            drawCitySkyline(w, horizonY)

            // Mặt đường tỏa rộng về phía người lái
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

            // Viền vàng hai bên mép đường
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

            // Vạch đứt tim đường chạy liên tục
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

        // 2. Xe Mazda CX-5 và Lớp phủ biển số chuẩn Việt Nam 79A - 137.73
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 85.dp) // Hạ thấp cách Dock khoảng cách chuẩn đẹp
                .width(320.dp)
                .height(210.dp),
            contentAlignment = Alignment.Center
        ) {
            // Hiển thị ảnh vector gốc của xe Mazda CX-5
            Image(
                painter = painterResource(id = R.drawable.car_mazda),
                contentDescription = "Mazda CX-5",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // Vẽ đè biển số sắc nét 79A - 137.73 với font số 1 có chân rõ ràng
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Nền biển số màu trắng sáng
                drawRoundRect(
                    color = Color(0xFFFBFBFB),
                    topLeft = Offset(w * 0.354f, h * 0.412f),
                    size = Size(w * 0.292f, h * 0.088f),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // Viền đen biển số
                drawRoundRect(
                    color = Color(0xFF1A1A1A),
                    topLeft = Offset(w * 0.354f, h * 0.412f),
                    size = Size(w * 0.292f, h * 0.088f),
                    cornerRadius = CornerRadius(4f, 4f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8f)
                )

                // Ký tự biển số 79A - 137.73 (Font Monospace Bold)
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 21f
                        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    canvas.nativeCanvas.drawText("79A - 137.73", w * 0.5f, h * 0.472f, paint)
                }
            }
        }
    }
}

// Hàm vẽ dãy tòa nhà thành phố với các ô cửa sổ sáng đèn
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

        // Khung nhà màu đen
        drawRect(
            color = buildingColor,
            topLeft = Offset(bLeft, bTop),
            size = Size(bWidth, bHeight)
        )

        // Các ô cửa sổ vàng
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
