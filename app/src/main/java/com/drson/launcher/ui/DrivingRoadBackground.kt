package com.drson.launcher.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

@Composable
fun DrivingRoadBackground(
    speedKmH: Float = 0f,
    modifier: Modifier = Modifier
) {
    var roadProgress by remember { mutableFloatStateOf(0f) }

    // Chuyển động cây và vạch kẻ đường theo tốc độ xe (0 km/h = đứng yên hoàn toàn)
    LaunchedEffect(speedKmH) {
        if (speedKmH > 0.5f) {
            var lastTime = 0L
            while (true) {
                withFrameNanos { now ->
                    if (lastTime != 0L) {
                        val dt = (now - lastTime) / 1_000_000_000f
                        val factor = speedKmH / 60f
                        roadProgress = (roadProgress + dt * factor) % 1f
                    }
                    lastTime = now
                }
            }
        }
    }

    // Xe nhún nhẹ khi di chuyển
    val infiniteTransition = rememberInfiniteTransition(label = "car_anim")
    val carBounceAnim by infiniteTransition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    val carBounce = if (speedKmH > 1f) carBounceAnim else 0f

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val horizonY = h * 0.40f
        val roadTopWidth = w * 0.08f
        val roadBottomWidth = w * 0.85f

        // Bầu trời đêm Luxury Gold/Black
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF070706), Color(0xFF14120D), Color(0xFF2B2211)),
                startY = 0f,
                endY = horizonY
            ),
            size = Size(w, horizonY)
        )

        // Bãi cỏ 2 bên
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF100E0A), Color(0xFF080705)),
                startY = horizonY,
                endY = h
            ),
            topLeft = Offset(0f, horizonY),
            size = Size(w, h - horizonY)
        )

        // Mặt đường 3D
        val roadPath = Path().apply {
            moveTo((w - roadTopWidth) / 2f, horizonY)
            lineTo((w + roadTopWidth) / 2f, horizonY)
            lineTo((w + roadBottomWidth) / 2f, h)
            lineTo((w - roadBottomWidth) / 2f, h)
            close()
        }
        drawPath(
            path = roadPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1F1D19), Color(0xFF12110F)),
                startY = horizonY,
                endY = h
            )
        )

        // Vạch kẻ đường vàng gold trôi liên tục
        val numDashes = 7
        for (i in 0 until numDashes) {
            val p = (i.toFloat() + roadProgress) % numDashes / numDashes.toFloat()
            if (p in 0f..1f) {
                val depth = p * p
                val dashY = horizonY + (h - horizonY) * depth
                val dashH = (h - horizonY) * 0.12f * depth
                val dashW = 4f + 22f * depth

                drawRect(
                    color = Color(0xFFD4AF37).copy(alpha = 0.3f + 0.7f * depth),
                    topLeft = Offset(w / 2f - dashW / 2f, dashY),
                    size = Size(dashW, dashH)
                )
            }
        }

        // Hàng cây trôi 2 bên đường
        val numTrees = 6
        for (i in 0 until numTrees) {
            val tProgress = (i.toFloat() + roadProgress) % numTrees / numTrees.toFloat()
            if (tProgress in 0.05f..1f) {
                val scale = tProgress * tProgress
                val treeY = horizonY + (h - horizonY) * scale
                val leftTreeX = (w / 2f) - (roadTopWidth / 2f + (roadBottomWidth - roadTopWidth) / 2f * scale) - (80f * scale)
                val rightTreeX = (w / 2f) + (roadTopWidth / 2f + (roadBottomWidth - roadTopWidth) / 2f * scale) + (30f * scale)

                drawTree(leftTreeX, treeY, scale)
                drawTree(rightTreeX, treeY, scale)
            }
        }

        // HÌNH ẢNH CHIẾC XE MAZDA CX-5 Ở TRUNG TÂM
        drawMazdaCX5(
            centerX = w / 2f,
            centerY = h * 0.74f + carBounce,
            scale = 1.45f
        )
    }
}

private fun DrawScope.drawTree(x: Float, y: Float, scale: Float) {
    val treeW = 60f * scale
    val treeH = 90f * scale
    val trunkW = 10f * scale
    val trunkH = 20f * scale

    drawRect(
        color = Color(0xFF1E1710),
        topLeft = Offset(x + treeW / 2f - trunkW / 2f, y),
        size = Size(trunkW, trunkH)
    )

    val foliageColor = Color(0xFF1C2B1C).copy(alpha = 0.4f + 0.6f * scale)
    val path = Path().apply {
        moveTo(x + treeW / 2f, y - treeH)
        lineTo(x + treeW, y)
        lineTo(x, y)
        close()
    }
    drawPath(path, color = foliageColor)
}

private fun DrawScope.drawMazdaCX5(centerX: Float, centerY: Float, scale: Float) {
    val carW = 220f * scale
    val carH = 95f * scale

    // Thân xe
    drawRoundRect(
        color = Color(0xFF161513),
        topLeft = Offset(centerX - carW / 2f, centerY - carH / 2f),
        size = Size(carW, carH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f * scale)
    )

    // Kính sau và mui xe
    val cabinW = carW * 0.74f
    val cabinH = carH * 0.46f
    drawRoundRect(
        color = Color(0xFF0B0A09),
        topLeft = Offset(centerX - cabinW / 2f, centerY - carH / 2f - cabinH * 0.65f),
        size = Size(cabinW, cabinH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f * scale)
    )

    // Cụm đèn hậu LED Gold Mazda
    val lightW = carW * 0.24f
    val lightH = 9f * scale
    drawRoundRect(
        color = Color(0xFFD4AF37),
        topLeft = Offset(centerX - carW / 2f + 14f * scale, centerY - 6f * scale),
        size = Size(lightW, lightH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
    )
    drawRoundRect(
        color = Color(0xFFD4AF37),
        topLeft = Offset(centerX + carW / 2f - lightW - 14f * scale, centerY - 6f * scale),
        size = Size(lightW, lightH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
    )

    // Biển số Dr. Sơn
    drawRoundRect(
        color = Color(0xFF282318),
        topLeft = Offset(centerX - 40f * scale, centerY + 16f * scale),
        size = Size(80f * scale, 24f * scale),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
    )
}
