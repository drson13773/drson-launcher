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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.drson.launcher.R

private val SKY_TOP = Color(0xFF14120F)
private val SKY_BOTTOM = Color(0xFF2C251C)
private val ROAD_DARK = Color(0xFF1E1C1A)
private val ROAD_EDGE = Color(0xFFD4AF37).copy(alpha = 0.5f)
private val ROAD_LANE = Color(0xFFFFF0B8).copy(alpha = 0.8f)

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

            // Viền vàng 2 bên làn đường
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

            // Vạch đứt tim đường chạy liên tục
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

        // 2. Xe Mazda ở vị trí rút ngắn 1/2 khoảng cách đến Dock
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 115.dp) // Rút ngắn 1/2 khoảng cách xuống dock
                .width(310.dp)           // Kích thước xe to và sắc nét
                .height(205.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.car_mazda),
                contentDescription = "Mazda CX-5",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}
