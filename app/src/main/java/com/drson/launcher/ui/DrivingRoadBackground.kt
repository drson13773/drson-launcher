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
    // Chỉ kích hoạt hiệu ứng khi xe thực sự di chuyển (tốc độ > 1 km/h)
    val isMoving = speedKmH > 1.0f

    // Tính toán thời gian lặp animation tương ứng với tốc độ (giới hạn mượt mà, không quá nhanh)
    val animationDuration = if (isMoving) {
        val calculated = (2000 / (speedKmH / 20f + 0.5f)).toInt()
        calculated.coerceIn(800, 3000)
    } else {
        Int.MAX_VALUE
    }

    val infiniteTransition = rememberInfiniteTransition(label = "road_animation")

    // Hiệu ứng trượt vạch tim đường tạo cảm giác tiến về phía trước
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

    // Hiệu ứng nhún nhẹ của xe khi di chuyển
    val carBounce by if (isMoving) {
        infiniteTransition.animateFloat(
            initialValue = -2.5f,
            targetValue = 2.5f,
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
                // Vẽ vạch tim đường chạy qua khi xe di chuyển
                if (isMoving) {
                    val centerX = size.width / 2f
                    val startY = size.height * 0.65f
                    val endY = size.height * 0.95f
                    
                    val dashLength = 20f
                    val gapLength = 30f
                    val totalPattern = dashLength + gapLength
                    val currentShift = (roadOffset / 100f) * totalPattern

                    var y = startY + (currentShift % totalPattern)
                    while (y < endY) {
                        drawLine(
                            color = Color(0xFFFFD700).copy(alpha = 0.8f),
                            start = Offset(centerX, y),
                            end = Offset(centerX, y + dashLength),
                            strokeWidth = 4f
                        )
                        y += totalPattern
                    }
                }
            }
    ) {
        // 1. Hình nền phong cảnh bg_rice_field.png
        Image(
            painter = painterResource(id = R.drawable.bg_rice_field),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        // 2. Xe Mazda CX-5 ở vị trí trung tâm phía dưới
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
                    .fillMaxWidth(0.45f)
                    .aspectRatio(1.6f),
                contentScale = ContentScale.Fit
            )
        }
    }
}
