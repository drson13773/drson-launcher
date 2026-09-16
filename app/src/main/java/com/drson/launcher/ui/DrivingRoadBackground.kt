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
    // Chỉ kích hoạt hiệu ứng khi xe thực sự di chuyển (tốc độ > 1 km/h)
    val isMoving = speedKmH > 1.0f

    val infiniteTransition = rememberInfiniteTransition(label = "road_animation")

    // Hiệu ứng nhún nhẹ của xe khi di chuyển (bằng 0 thì xe đứng yên tuyệt đối)
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

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Hình nền phong cảnh bg_rice_field.png (Đã có sẵn trăng, đồng lúa, biển và mặt đường)
        Image(
            painter = painterResource(id = R.drawable.bg_rice_field),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        // 2. Hình chiếc xe Mazda đặt ở vị trí trung tâm phía dưới (Đứng yên khi tốc độ = 0, nhún nhẹ khi chạy)
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
}package com.drson.launcher.ui

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
    // Chỉ kích hoạt hiệu ứng khi xe thực sự di chuyển (tốc độ > 1 km/h)
    val isMoving = speedKmH > 1.0f

    val infiniteTransition = rememberInfiniteTransition(label = "road_animation")

    // Hiệu ứng nhún nhẹ của xe khi di chuyển (bằng 0 thì xe đứng yên tuyệt đối)
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

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Hình nền phong cảnh bg_rice_field.png (Đã có sẵn trăng, đồng lúa, biển và mặt đường)
        Image(
            painter = painterResource(id = R.drawable.bg_rice_field),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        // 2. Hình chiếc xe Mazda đặt ở vị trí trung tâm phía dưới (Đứng yên khi tốc độ = 0, nhún nhẹ khi chạy)
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
