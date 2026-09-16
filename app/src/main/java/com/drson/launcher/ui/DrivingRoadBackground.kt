package com.drson.launcher.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.drson.launcher.R

@Composable
fun DrivingRoadBackground(
    speedKmH: Float = 0f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Nạp ảnh gốc xe Mazda
    val carBitmap = remember {
        val carResId = if (context.resources.getIdentifier("car_mazda", "drawable", context.packageName) != 0) {
            R.drawable.car_mazda
        } else {
            R.drawable.car_photo
        }
        try {
            BitmapFactory.decodeResource(context.resources, carResId)?.asImageBitmap()
        } catch (_: Exception) { null }
    }

    // Nạp ảnh cây gốc 2 bên đường
    val tree1Bitmap = remember {
        try {
            BitmapFactory.decodeResource(context.resources, R.drawable.bg_tree_1)?.asImageBitmap()
        } catch (_: Exception) { null }
    }

    val tree2Bitmap = remember {
        try {
            BitmapFactory.decodeResource(context.resources, R.drawable.bg_tree_2)?.asImageBitmap()
        } catch (_: Exception) { null }
    }

    // Nạp ảnh Mặt Trăng gốc
    val moonBitmap = remember {
        try {
            BitmapFactory.decodeResource(context.resources, R.drawable.bg_moon)?.asImageBitmap()
        } catch (_: Exception) { null }
    }

    // Tính toán tiến độ trôi theo tốc độ xe (0 km/h = đứng yên)
    var roadProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(speedKmH) {
        if (speedKmH > 0.5f) {
            var lastTime = 0L
            while (true) {
                withFrameNanos { now ->
                    if (lastTime != 0L) {
                        val dt = (now - lastTime) / 1_000_000_000f
                        val factor = speedKmH / 50f
                        roadProgress = (roadProgress + dt * factor) % 1f
                    }
                    lastTime = now
                }
            }
        }
    }

    // Nhún nhẹ thân xe
    val transition = rememberInfiniteTransition(label = "car_bounce")
    val bounceAnim by transition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    val carBounce = if (speedKmH > 1f) bounceAnim else 0f

    Box(modifier = modifier.fillMaxSize()) {
        // LỚP 1: ẢNH NỀN GỐC CON ĐƯỜNG VÀ DÃY NHÀ
        Image(
            painter = painterResource(id = R.drawable.final_no_dock),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        // LỚP 2: RENDER MẶT TRĂNG, CÂY TRÔI VÀ XE MAZDA
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Điểm tụ đường chân trời dựa trên ảnh final_no_dock
            val horizonY = h * 0.428f
            val roadTopWidth = w * 0.02f
            val roadBottomWidth = w * 0.92f

            // 1. Mặt trăng (bg_moon.png) ở góc trên
            moonBitmap?.let { moon ->
                val moonSize = (h * 0.20f).toInt()
                drawImage(
                    image = moon,
                    dstOffset = IntOffset((w * 0.70f).toInt(), (h * 0.04f).toInt()),
                    dstSize = IntSize(moonSize, moonSize)
                )
            }

            // 2. Hàng cây trôi 2 bên mép đường theo tốc độ GPS
            val numTrees = 5
            for (i in 0 until numTrees) {
                val tProgress = (i.toFloat() + roadProgress) % numTrees / numTrees.toFloat()
                if (tProgress in 0.06f..0.98f) {
                    val scale = tProgress * tProgress
                    val treeY = horizonY + (h - horizonY) * scale
                    val treeImg = if (i % 2 == 0) tree1Bitmap else tree2Bitmap

                    treeImg?.let { tree ->
                        val treeW = (h * 0.30f * scale).toInt()
                        val treeH = (h * 0.42f * scale).toInt()

                        if (treeW > 8 && treeH > 8) {
                            val leftX = (w / 2f) - (roadTopWidth / 2f + (roadBottomWidth - roadTopWidth) / 2f * scale) - treeW * 0.9f
                            val rightX = (w / 2f) + (roadTopWidth / 2f + (roadBottomWidth - roadTopWidth) / 2f * scale) - treeW * 0.1f

                            drawImage(
                                image = tree,
                                dstOffset = IntOffset(leftX.toInt(), (treeY - treeH).toInt()),
                                dstSize = IntSize(treeW, treeH)
                            )
                            drawImage(
                                image = tree,
                                dstOffset = IntOffset(rightX.toInt(), (treeY - treeH).toInt()),
                                dstSize = IntSize(treeW, treeH)
                            )
                        }
                    }
                }
            }

            // 3. Xe Mazda gốc nằm chính giữa làn đường
            carBitmap?.let { car ->
                val carWidth = (h * 0.58f).toInt()
                val carHeight = (carWidth * (car.height.toFloat() / car.width.toFloat())).toInt()

                val carX = (w / 2f - carWidth / 2f).toInt()
                val carY = (h * 0.65f - carHeight / 2f + carBounce).toInt()

                drawImage(
                    image = car,
                    dstOffset = IntOffset(carX, carY),
                    dstSize = IntSize(carWidth, carHeight)
                )
            }
        }
    }
}
