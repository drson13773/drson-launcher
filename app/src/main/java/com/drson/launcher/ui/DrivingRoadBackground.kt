package com.drson.launcher.ui

import android.graphics.BitmapFactory
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.drson.launcher.R
import kotlin.random.Random

@Composable
fun DrivingRoadBackground(
    speedKmH: Float = 0f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val carBitmap = remember {
        val resId = if (context.resources.getIdentifier("car_mazda", "drawable", context.packageName) != 0) {
            R.drawable.car_mazda
        } else {
            R.drawable.car_photo
        }
        try { BitmapFactory.decodeResource(context.resources, resId)?.asImageBitmap() } catch (_: Exception) { null }
    }

    val tree1Bitmap = remember {
        try { BitmapFactory.decodeResource(context.resources, R.drawable.bg_tree_1)?.asImageBitmap() } catch (_: Exception) { null }
    }

    val tree2Bitmap = remember {
        try { BitmapFactory.decodeResource(context.resources, R.drawable.bg_tree_2)?.asImageBitmap() } catch (_: Exception) { null }
    }

    val moonBitmap = remember {
        try { BitmapFactory.decodeResource(context.resources, R.drawable.bg_moon)?.asImageBitmap() } catch (_: Exception) { null }
    }

    // 1. Quãng đường trôi vạch kẻ đường & cây cối theo tốc độ GPS
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

    // 2. Xe nhún nhẹ khi di chuyển
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

    // 3. Hiệu ứng đèn nhà nhấp nháy ngẫu nhiên
    val blinkPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lights"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Nền tĩnh con đường và dãy nhà
        Image(
            painter = painterResource(id = R.drawable.final_no_dock),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val horizonY = h * 0.428f
            val roadTopWidth = w * 0.02f
            val roadBottomWidth = w * 0.92f

            // A. Mặt trăng ở góc trên bên phải
            moonBitmap?.let { moon ->
                val moonSize = (h * 0.20f).toInt()
                drawImage(
                    image = moon,
                    dstOffset = IntOffset((w * 0.72f).toInt(), (h * 0.03f).toInt()),
                    dstSize = IntSize(moonSize, moonSize)
                )
            }

            // B. Hiệu ứng các ô đèn nhà bật/tắt ngẫu nhiên
            val windowSeed = (blinkPhase * 10).toInt()
            val rnd = Random(windowSeed)
            val windowWidth = w * 0.009f
            val windowHeight = h * 0.012f
            for (i in 0 until 18) {
                if (rnd.nextBoolean()) {
                    val wx = w * (0.05f + (i * 0.05f))
                    val wy = h * (0.34f + (i % 3) * 0.025f)
                    drawRect(
                        color = Color(0xFFFFD574).copy(alpha = 0.85f),
                        topLeft = Offset(wx, wy),
                        size = Size(windowWidth, windowHeight)
                    )
                }
            }

            // C. Vạch tim đường màu Vàng Gold trôi theo tốc độ GPS
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

            // D. Cây cối hai bên đường trôi lùi theo tốc độ
            val numTrees = 5
            for (i in 0 until numTrees) {
                val tProgress = (i.toFloat() + roadProgress) % numTrees / numTrees.toFloat()
                if (tProgress in 0.06f..0.98f) {
                    val scale = tProgress * tProgress
                    val treeY = horizonY + (h - horizonY) * scale
                    val treeImg = if (i % 2 == 0) tree1Bitmap else tree2Bitmap

                    treeImg?.let { tree ->
                        val treeW = (h * 0.28f * scale).toInt()
                        val treeH = (h * 0.40f * scale).toInt()

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

            // E. Xe Mazda CX-5 ở trung tâm mặt đường
            carBitmap?.let { car ->
                val carWidth = (h * 0.58f).toInt()
                val carHeight = (carWidth * (car.height.toFloat() / car.width.toFloat())).toInt()
                val carX = (w / 2f - carWidth / 2f).toInt()
                val carY = (h * 0.63f - carHeight / 2f + carBounce).toInt()

                drawImage(
                    image = car,
                    dstOffset = IntOffset(carX, carY),
                    dstSize = IntSize(carWidth, carHeight)
                )
            }
        }
    }
}
