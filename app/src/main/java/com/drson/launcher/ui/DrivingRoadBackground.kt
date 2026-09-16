package com.drson.launcher.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.drson.launcher.R

@Composable
fun DrivingRoadBackground(
    speedKmH: Float = 0f,
    modifier: Modifier = Modifier
) {
    // Chỉ tạo hiệu ứng nhún/chuyển động khi tốc độ thực tế > 1 km/h (bằng 0 thì xe đứng yên hoàn toàn)
    val isMoving = speedKmH > 1.0f
    
    val infiniteTransition = rememberInfiniteTransition(label = "road_loop")

    val carBounce by if (isMoving) {
        infiniteTransition.animateFloat(
            initialValue = -3f,
            targetValue = 3f,
            animationSpec = infiniteRepeatable(
                animation = tween(400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bounce"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Hình nền mới bg_rice_field.png (Đã có sẵn trăng và cảnh vật, không vẽ đè)
        Image(
            painter = painterResource(id = R.drawable.bg_rice_field),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        // 2. Xe Mazda CX-5 ở vị trí trung tâm phía dưới (Dừng khi tốc độ = 0, nhún nhẹ khi xe chạy)
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
