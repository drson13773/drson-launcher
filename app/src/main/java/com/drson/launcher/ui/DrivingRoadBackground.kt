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

        // CÂN ĐỐI TỶ LỆ THEO CHIỀU CAO THỰC TẾ
        val horizonY = h * 0.40f
        val roadTopWidth = h * 0.16f
        val roadBottomWidth = h * 1.55f

        // 1. Bầu trời đêm sang trọng
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF070706), Color(0xFF14120D), Color(0xFF2B2211)),
                startY = 0f,
                endY = horizonY
            ),
            size = Size(w, horizonY)
        )

        // 2. Lề đường / Bãi cỏ 2 bên
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF100E0A), Color(0xFF080705)),
                startY = horizonY,
                endY = h
            ),
            topLeft = Offset(0f, horizonY),
            size = Size(w, h - horizonY)
        )

        // 3. Mặt đường nhựa phối cảnh 3D
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

        // 4. Vạch kẻ đường vàng gold chuyển động lặp
        val numDashes = 7
        for (i in 0 until numDashes) {
            val p = (i.toFloat() + roadProgress) % numDashes / numDashes.toFloat()
            if (p in 0f..1f) {
                val depth = p * p
                val dashY = horizonY + (h - horizonY) * depth
                val dashH = (h - horizonY) * 0.12f * depth
                val dashW = (h * 0.008f) + (h * 0.035f) * depth

                drawRect(
                    color = Color(0xFFD4AF37).copy(alpha = 0.3f + 0.7f * depth),
                    topLeft = Offset(w / 2f - dashW / 2f, dashY),
                    size = Size(dashW, dashH)
                )
            }
        }

        // 5. Hàng cây chuyển động theo phối cảnh
        val numTrees = 6
        for (i in 0 until numTrees) {
            val tProgress = (i.toFloat() + roadProgress) % numTrees / numTrees.toFloat()
            if (tProgress in 0.05f..1f) {
                val scale = tProgress * tProgress
                val treeY = horizonY + (h - horizonY) * scale
                val leftTreeX = (w / 2f) - (roadTopWidth / 2f + (roadBottomWidth - roadTopWidth) / 2f * scale) - (h * 0.12f * scale)
                val rightTreeX = (w / 2f) + (roadTopWidth / 2f + (roadBottomWidth - roadTopWidth) / 2f * scale) + (h * 0.04f * scale)

                drawTree(leftTreeX, treeY, scale, h)
                drawTree(rightTreeX, treeY, scale, h)
            }
        }

        // 6. Hình ảnh xe Mazda CX-5 ở trung tâm (Scale theo chiều cao h)
        drawMazdaCX5(
            centerX = w / 2f,
            centerY = h * 0.73f + carBounce,
            baseHeight = h
        )
    }
}

private fun DrawScope.drawTree(x: Float, y: Float, scale: Float, baseHeight: Float) {
    val treeW = (baseHeight * 0.10f) * scale
    val treeH = (baseHeight * 0.16f) * scale
    val trunkW = (baseHeight * 0.016f) * scale
    val trunkH = (baseHeight * 0.032f) * scale

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

private fun DrawScope.drawMazdaCX5(centerX: Float, centerY: Float, baseHeight: Float) {
    val carW = baseHeight * 0.48f
    val carH = baseHeight * 0.21f

    // Thân xe bo tròn
    drawRoundRect(
        color = Color(0xFF161513),
        topLeft = Offset(centerX - carW / 2f, centerY - carH / 2f),
        size = Size(carW, carH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(carH * 0.22f)
    )

    // Kính sau & mui xe
    val cabinW = carW * 0.74f
    val cabinH = carH * 0.46f
    drawRoundRect(
        color = Color(0xFF0B0A09),
        topLeft = Offset(centerX - cabinW / 2f, centerY - carH / 2f - cabinH * 0.65f),
        size = Size(cabinW, cabinH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cabinH * 0.25f)
    )

    // Cụm đèn hậu LED Gold Mazda
    val lightW = carW * 0.24f
    val lightH = carH * 0.10f
    drawRoundRect(
        color = Color(0xFFD4AF37),
        topLeft = Offset(centerX - carW / 2f + carW * 0.06f, centerY - lightH / 2f),
        size = Size(lightW, lightH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
    )
    drawRoundRect(
        color = Color(0xFFD4AF37),
        topLeft = Offset(centerX + carW / 2f - lightW - carW * 0.06f, centerY - lightH / 2f),
        size = Size(lightW, lightH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
    )

    // Biển số xe Dr. Sơn
    val plateW = carW * 0.36f
    val plateH = carH * 0.24f
    drawRoundRect(
        color = Color(0xFF282318),
        topLeft = Offset(centerX - plateW / 2f, centerY + carH * 0.16f),
        size = Size(plateW, plateH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
    )
}
