package com.drson.launcher.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.drson.launcher.R

private val GOLD_BRIGHT = Color(0xFFFFF0B8)
private val GOLD_ACCENT = Color(0xFFD4AF37)
private val ROAD_COLOR = Color(0xFF191715)
private val ROAD_MARKING = Color(0xFFE5C158)

@Composable
fun DrivingRoadBackground(modifier: Modifier = Modifier) {
    // 1. Animation di chuyển vạch kẻ đường liên tục
    val infiniteTransition = rememberInfiniteTransition(label = "road_anim")
    val roadOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "road_offset"
    )

    // 2. Hiệu ứng nhấp nháy đèn cửa sổ các tòa nhà theo nhiều chu kỳ khác nhau
    val blink1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blink_1"
    )

    val blink2 by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blink_2"
    )

    val blink3 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blink_3"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Nền bầu trời đêm
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF070706),
                        Color(0xFF14120E),
                        Color(0xFF1C1914)
                    )
                ),
                size = size
            )

            // DÃY TÒA NHÀ & CỬA SỔ NHẤP NHÁY ĐÈN
            val horizonY = h * 0.44f
            val buildingConfigs = listOf(
                Triple(w * 0.08f, 75f, 110f),
                Triple(w * 0.16f, 65f, 140f),
                Triple(w * 0.23f, 70f, 95f),
                Triple(w * 0.30f, 60f, 130f),
                Triple(w * 0.36f, 55f, 85f),
                Triple(w * 0.45f, 90f, 150f),
                Triple(w * 0.55f, 60f, 90f),
                Triple(w * 0.62f, 65f, 135f),
                Triple(w * 0.70f, 75f, 115f),
                Triple(w * 0.78f, 70f, 145f),
                Triple(w * 0.86f, 80f, 100f)
            )

            buildingConfigs.forEachIndexed { bIndex, (startX, bWidth, bHeight) ->
                val topY = horizonY - bHeight
                // Thân tòa nhà
                drawRect(
                    color = Color(0xFF0F0E0C),
                    topLeft = Offset(startX, topY),
                    size = androidx.compose.ui.geometry.Size(bWidth, bHeight)
                )

                // Viền trên nhẹ
                drawLine(
                    color = Color(0xFF26221B),
                    start = Offset(startX, topY),
                    end = Offset(startX + bWidth, topY),
                    strokeWidth = 1.5f
                )

                // Vẽ các ô cửa sổ có đèn nhấp nháy
                val cols = 4
                val rows = (bHeight / 16f).toInt().coerceAtLeast(3)
                val padX = bWidth / (cols + 1)
                val padY = bHeight / (rows + 1)

                for (r in 1..rows) {
                    for (c in 1..cols) {
                        val winX = startX + c * padX - 2.5f
                        val winY = topY + r * padY - 2.5f

                        val blinkFactor = when ((bIndex * 7 + r * 3 + c) % 3) {
                            0 -> blink1
                            1 -> blink2
                            else -> blink3
                        }

                        val isYellowLight = ((bIndex + r + c) % 5) != 0
                        val winColor = if (isYellowLight) {
                            GOLD_ACCENT.copy(alpha = 0.35f + 0.65f * blinkFactor)
                        } else {
                            GOLD_BRIGHT.copy(alpha = 0.2f + 0.75f * blinkFactor)
                        }

                        drawRect(
                            color = winColor,
                            topLeft = Offset(winX, winY),
                            size = androidx.compose.ui.geometry.Size(5f, 5f)
                        )
                    }
                }
            }

            // CON ĐƯỜNG 3D VỀ PHÍA CHÂN TRỜI
            val roadTopW = w * 0.16f
            val roadBottomW = w * 0.82f
            val bottomY = h * 0.88f

            val roadPath = Path().apply {
                moveTo((w - roadTopW) / 2f, horizonY)
                lineTo((w + roadTopW) / 2f, horizonY)
                lineTo((w + roadBottomW) / 2f, bottomY)
                lineTo((w - roadBottomW) / 2f, bottomY)
                close()
            }

            drawPath(
                path = roadPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF141210), ROAD_COLOR, Color(0xFF24201A)),
                    startY = horizonY,
                    endY = bottomY
                )
            )

            // Vạch mép đường màu vàng kim
            drawLine(
                color = GOLD_ACCENT.copy(alpha = 0.7f),
                start = Offset((w - roadTopW) / 2f, horizonY),
                end = Offset((w - roadBottomW) / 2f, bottomY),
                strokeWidth = 3f
            )
            drawLine(
                color = GOLD_ACCENT.copy(alpha = 0.7f),
                start = Offset((w + roadTopW) / 2f, horizonY),
                end = Offset((w + roadBottomW) / 2f, bottomY),
                strokeWidth = 3f
            )

            // Vạch kẻ đường giữa làn di chuyển liên tục
            val centerX = w / 2f
            val totalDashes = 7
            for (i in 0..totalDashes) {
                val progress = ((i.toFloat() + roadOffset) % totalDashes) / totalDashes.toFloat()
                val dashY = horizonY + (bottomY - horizonY) * (progress * progress) // Phối cảnh xa gần 3D
                val dashH = 10f + 32f * progress
                val dashW = 2.5f + 5f * progress

                drawRect(
                    color = ROAD_MARKING.copy(alpha = 0.3f + 0.7f * progress),
                    topLeft = Offset(centerX - dashW / 2f, dashY),
                    size = androidx.compose.ui.geometry.Size(dashW, dashH)
                )
            }
        }

        // HÌNH ẢNH XE MAZDA CX-5 ĐỎ ĐẶT Ở GIỮA ĐƯỜNG
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 70.dp)
                .size(width = 240.dp, height = 175.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.car_mazda),
                contentDescription = "Mazda CX-5",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}
