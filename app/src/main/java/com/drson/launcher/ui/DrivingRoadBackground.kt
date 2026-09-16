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
    val infiniteTransition = rememberInfiniteTransition(label = "road_loop")

    val carBounce by if (speedKmH > 2f) {
        infiniteTransition.animateFloat(
            initialValue = -4f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(
                animation = tween(350, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bounce"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Ảnh nền tĩnh con đường và dãy nhà
        Image(
            painter = painterResource(id = R.drawable.final_no_dock),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        // 2. Mặt trăng góc trên phải
        Image(
            painter = painterResource(id = R.drawable.bg_moon),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = 32.dp)
                .size(75.dp),
            contentScale = ContentScale.Fit
        )

        // 3. Xe Mazda CX-5 ở vị trí trung tâm phía dưới
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
