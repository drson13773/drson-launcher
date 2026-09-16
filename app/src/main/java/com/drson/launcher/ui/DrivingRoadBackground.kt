package com.drson.launcher.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.drson.launcher.R

@Composable
fun DrivingRoadBackground(
    speedKmH: Float = 0f,
    modifier: Modifier = Modifier
) {
    val isMoving = speedKmH > 1.0f

    // Tốc độ chuyển động của vạch tim đường tương ứng với giá trị tốc độ GPS
    val animationDuration = if (isMoving) {
        val calculated = (2000 / (speedKmH / 20f + 0.5f)).toInt()
        calculated.coerceIn(800, 3000)
    } else {
        Int.MAX_VALUE
    }

    val infiniteTransition = rememberInfiniteTransition(label = "road_animation")

    // Hiệu ứng trượt vạch tim đường
    val roadOffset by if (isMoving) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 100f,
            animationSpec = infiniteRepeatable(
                animation = tween(animationDuration, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "road_scroll"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    // Hiệu ứng nhún nhẹ khi xe chạy
    val carBounce by if (isMoving) {
        infiniteTransition.animateFloat(
            initialValue = -2f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(
                animation = tween(400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "car_bounce"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // Vẽ vạch tim đường màu vàng chạy qua khi xe di chuyển
                if (isMoving) {
                    val centerX = size.width / 2f
                    val startY = size.height * 0.68f
                    val endY = size.height * 0.95f
                    
                    val dashLength = 15f
                    val gapLength = 25f
                    val totalPattern = dashLength + gapLength
                    val currentShift = (roadOffset / 100f) * totalPattern

                    var y = startY + (currentShift % totalPattern)
                    while (y < endY) {
                        drawLine(
                            color = Color(0xFFFFD700).copy(alpha = 0.85f),
                            start = Offset(centerX, y),
                            end = Offset(centerX, y + dashLength),
                            strokeWidth = 3.5f
                        )
                        y += totalPattern
                    }
                }
            }
    ) {
        // 1. Hình nền bg_rice_field.png
        Image(
            painter = painterResource(id = R.drawable.bg_rice_field),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        // 2. Xe Mazda CX-5 (Đã thu nhỏ tỷ lệ fillMaxWidth từ 0.45f xuống 0.28f để vừa vặn với con đường)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = carBounce.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.car_mazda),
                contentDescription = "Car Mazda",
                modifier = Modifier
                    .fillMaxWidth(0.28f) // Tỷ lệ chuẩn, không bị to quá so với mặt đường
                    .aspectRatio(1.6f),
                contentScale = ContentScale.Fit
            )
        }
    }
}
