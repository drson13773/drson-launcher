package com.drson.launcher.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
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
        // 1. Phối cảnh đường chạy 3D và Skyline ban đêm
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val horizonY = h * 0.45f

            drawCitySkyline(w, horizonY)

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

            // Vạch kẻ tim đường chuyển động liên tục
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

        // 2. Hình ảnh Mazda CX-5 trong suốt đặt cách Dock 85dp
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 85.dp) // Vị trí chuẩn: thu ngắn 1/2 khoảng cách đến Dock
                .width(310.dp)
                .height(205.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.car_mazda),
                contentDescription = "Mazda CX-5 79A-137.73",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

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

        drawRect(
            color = buildingColor,
            topLeft = Offset(bLeft, bTop),
            size = Size(bWidth, bHeight)
        )

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
