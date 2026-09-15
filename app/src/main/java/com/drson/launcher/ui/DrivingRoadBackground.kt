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
    // Biến lưu trữ tiến độ trôi của mặt đường và cây cối
    var roadProgress by remember { mutableFloatStateOf(0f) }

    // Tính toán chuyển động dựa trên frame và tốc độ thực tế (0 = đứng yên hoàn toàn)
    LaunchedEffect(speedKmH) {
        if (speedKmH > 0.5f) {
            var lastFrameTime = 0L
            while (true) {
                withFrameNanos { frameTimeNanos ->
                    if (lastFrameTime != 0L) {
                        val dt = (frameTimeNanos - lastFrameTime) / 1_000_000_000f // giây
                        // Tốc độ càng cao thì bước tăng càng nhanh (100 km/h quay 1 chu kỳ trong ~0.8s)
                        val speedFactor = speedKmH / 70f
                        roadProgress = (roadProgress + dt * speedFactor) % 1f
                    }
                    lastFrameTime = frameTimeNanos
                }
            }
        }
    }

    // Hiệu ứng nhún xe (chỉ nhún khi xe đang di chuyển)
    val infiniteTransition = rememberInfiniteTransition(label = "driving_loop")
    val carBounceAnim by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "car_bounce"
    )
    val carBounce = if (speedKmH > 1f) carBounceAnim else 0f

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val horizonY = h * 0.42f // Đường chân trời
        val roadTopWidth = w * 0.08f
        val roadBottomWidth = w * 0.85f

        // 1. Bầu trời đêm sang trọng
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF070706),
                    Color(0xFF14120D),
                    Color(0xFF2B2211)
                ),
                startY = 0f,
                endY = horizonY
            ),
            size = Size(w, horizonY)
        )

        // 2. Lề đường / Bãi cỏ hai bên
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

        // 4. Vạch kẻ đường đứt khúc màu Vàng Gold
        val numDashes = 7
        for (i in 0 until numDashes) {
            val p = (i.toFloat() + roadProgress) % numDashes / numDashes.toFloat()
            if (p in 0f..1f) {
                val depth = p * p
                val dashY = horizonY + (h - horizonY) * depth
                val dashH = (h - horizonY) * 0.12f * depth
                val dashW = 4f + 20f * depth

                drawRect(
                    color = Color(0xFFD4AF37).copy(alpha = 0.3f + 0.7f * depth),
                    topLeft = Offset(w / 2f - dashW / 2f, dashY),
                    size = Size(dashW, dashH)
                )
            }
        }

        // 5. Hàng cây hai bên đường trôi theo tốc độ (Tương xứng với đồng hồ)
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

        // 6. Xe Mazda CX-5
        drawLuxuryCar(
            centerX = w / 2f,
            centerY = h * 0.78f + carBounce,
            scale = 1.35f
        )
    }
}

private fun DrawScope.drawTree(x: Float, y: Float, scale: Float) {
    val treeW = 60f * scale
    val treeH = 90f * scale
    val trunkW = 10f * scale
    val trunkH = 20f * scale

    // Thân cây
    drawRect(
        color = Color(0xFF1E1710),
        topLeft = Offset(x + treeW / 2f - trunkW / 2f, y),
        size = Size(trunkW, trunkH)
    )

    // Tán lá
    val foliageColor = Color(0xFF1A261A).copy(alpha = 0.4f + 0.6f * scale)
    val path = Path().apply {
        moveTo(x + treeW / 2f, y - treeH)
        lineTo(x + treeW, y)
        lineTo(x, y)
        close()
    }
    drawPath(path, color = foliageColor)
}

private fun DrawScope.drawLuxuryCar(centerX: Float, centerY: Float, scale: Float) {
    val carW = 200f * scale
    val carH = 90f * scale

    drawRoundRect(
        color = Color(0xFF151412),
        topLeft = Offset(centerX - carW / 2f, centerY - carH / 2f),
        size = Size(carW, carH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f * scale)
    )

    val cabinW = carW * 0.75f
    val cabinH = carH * 0.45f
    drawRoundRect(
        color = Color(0xFF0A0A09),
        topLeft = Offset(centerX - cabinW / 2f, centerY - carH / 2f - cabinH * 0.6f),
        size = Size(cabinW, cabinH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f * scale)
    )

    val lightW = carW * 0.22f
    val lightH = 8f * scale
    drawRoundRect(
        color = Color(0xFFD4AF37),
        topLeft = Offset(centerX - carW / 2f + 12f * scale, centerY - 8f * scale),
        size = Size(lightW, lightH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
    )
    drawRoundRect(
        color = Color(0xFFD4AF37),
        topLeft = Offset(centerX + carW / 2f - lightW - 12f * scale, centerY - 8f * scale),
        size = Size(lightW, lightH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
    )

    drawRoundRect(
        color = Color(0xFF262116),
        topLeft = Offset(centerX - 35f * scale, centerY + 14f * scale),
        size = Size(70f * scale, 22f * scale),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f)
    )
}
