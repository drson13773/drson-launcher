package com.drson.launcher.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.drson.launcher.R
import kotlin.random.Random

@Composable
fun DrivingRoadBackground(
    speedKmH: Float = 0f,
    modifier: Modifier = Modifier
) {
    // Sử dụng painter chuẩn của Compose để tối ưu bộ nhớ đệm hình ảnh
    val carPainter = painterResource(id = R.drawable.car_mazda)
    val tree1Painter = painterResource(id = R.drawable.bg_tree_1)
    val tree2Painter = painterResource(id = R.drawable.bg_tree_2)
    val moonPainter = painterResource(id = R.drawable.bg_moon)

    // Animation chuyển động mượt mà không dùng vòng lặp vô tận gây block UI thread
    val infiniteTransition = rememberInfiniteTransition(label = "road_loop")
    
    val durationMs = if (speedKmH > 2f) {
        (3000 / (speedKmH / 30f).coerceIn(0.5f, 5f)).toInt()
    } else {
        0
    }

    val roadProgress by if (durationMs > 0) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMs, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "roadProgress"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    // Hiệu ứng xe nhún nhẹ khi di chuyển
    val carBounce by if (speedKmH > 2f) {
        infiniteTransition.animateFloat(
            initialValue = -2.5f,
            targetValue = 2.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(380, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bounce"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    // Hiệu ứng đèn nhà nhấp nháy
    val blinkPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blink"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Ảnh nền tĩnh con đường và dãy nhà
        Image(
            painter = painterResource(id = R.drawable.final_no_dock),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val horizonY = h * 0.428f
            val roadTopWidth = w * 0.02f
            val roadBottomWidth = w * 0.92f

            // 1. Vẽ mặt trăng góc trên phải
            val moonSize = h * 0.20f
            drawContext.canvas.save()
            drawContext.canvas.translate(w * 0.72f, h * 0.03f)
            with(moonPainter) {
                draw(size = Size(moonSize, moonSize), alpha = 0.9f)
            }
            drawContext.canvas.restore()

            // 2. Vẽ đèn nhà nhấp nháy ngẫu nhiên
            val windowSeed = (blinkPhase * 8).toInt()
            val rnd = Random(windowSeed)
            val windowWidth = w * 0.009f
            val windowHeight = h * 0.012f
            for (i in 0 until 16) {
                if (rnd.nextBoolean()) {
                    val wx = w * (0.05f + (i * 0.055f))
                    val wy = h * (0.34f + (i % 3) * 0.025f)
                    drawRect(
                        color = Color(0xFFFFD574).copy(alpha = 0.85f),
                        topLeft = Offset(wx, wy),
                        size = Size(windowWidth, windowHeight)
                    )
                }
            }

            // 3. Vạch tim đường màu vàng trôi theo tốc độ
            val numDashes = 7
            for (i in 0 until numDashes) {
                val p = (i.toFloat() + roadProgress) % numDashes / numDashes.toFloat()
                if (p in 0.05f..0.98f) {
                    val depth = p * p
                    val dashY = horizonY + (h - horizonY) * depth
                    val dashH = (h - horizonY) * 0.12f * depth
                    val dashW = (h * 0.006f) + (h * 0.028f) * depth

                    drawRect(
                        color = Color(0xFFD4AF37).copy(alpha = 0.35f + 0.65f * depth),
                        topLeft = Offset(w / 2f - dashW / 2f, dashY),
                        size = Size(dashW, dashH)
                    )
                }
            }

            // 4. Hàng cây hai bên đường trôi lùi
            val numTrees = 4
            for (i in 0 until numTrees) {
                val tProgress = (i.toFloat() + roadProgress) % numTrees / numTrees.toFloat()
                if (tProgress in 0.06f..0.98f) {
                    val scale = tProgress * tProgress
                    val treeY = horizonY + (h - horizonY) * scale
                    val treePainter = if (i % 2 == 0) tree1Painter else tree2Painter
                    val treeW = h * 0.26f * scale
                    val treeH = h * 0.38f * scale

                    if (treeW > 10f && treeH > 10f) {
                        val leftX = (w / 2f) - (roadTopWidth / 2f + (roadBottomWidth - roadTopWidth) / 2f * scale) - treeW * 0.9f
                        val rightX = (w / 2f) + (roadTopWidth / 2f + (roadBottomWidth - roadTopWidth) / 2f * scale) - treeW * 0.1f

                        // Cây trái
                        drawContext.canvas.save()
                        drawContext.canvas.translate(leftX, treeY - treeH)
                        with(treePainter) { draw(size = Size(treeW, treeH)) }
                        drawContext.canvas.restore()

                        // Cây phải
                        drawContext.canvas.save()
                        drawContext.canvas.translate(rightX, treeY - treeH)
                        with(treePainter) { draw(size = Size(treeW, treeH)) }
                        drawContext.canvas.restore()
                    }
                }
            }

            // 5. Xe Mazda CX-5 ở giữa tâm đường
            val carW = h * 0.58f
            val carH = carW * 0.62f
            val carX = (w / 2f - carW / 2f)
            val carY = (h * 0.63f - carH / 2f + carBounce)

            drawContext.canvas.save()
            drawContext.canvas.translate(carX, carY)
            with(carPainter) { draw(size = Size(carW, carH)) }
            drawContext.canvas.restore()
        }
    }
}
