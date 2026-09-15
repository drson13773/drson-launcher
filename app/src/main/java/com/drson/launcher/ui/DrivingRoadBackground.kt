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
import androidx.compose.ui.geometry.Size
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
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "road_offset"
    )

    // 2. Hiệu ứng nháy đèn các tòa nhà: TỐC ĐỘ NHANH & ĐỘ TƯƠNG PHẢN CỰC RÕ (450ms - 800ms)
    val fastBlink1 by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fast_blink_1"
    )

    val fastBlink2 by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fast_blink_2"
    )

    val fastBlink3 by infiniteTransition.animateFloat(
        initialValue = 0.10f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(780, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fast_blink_3"
    )

    val fastBlink4 by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fast_blink_4"
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

            // DÃY TÒA NHÀ & CỬA SỔ NHẤP NHÁY ĐÈN SẮC NÉT
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
                    color = Color(0xFF0C0B0A),
                    topLeft = Offset(startX, topY),
                    size = Size(bWidth, bHeight)
                )

                // Viền cạnh trên tòa nhà
                drawLine(
                    color = Color(0xFF332D23),
                    start = Offset(startX, topY),
                    end = Offset(startX + bWidth, topY),
                    strokeWidth = 1.8f
                )

                // Vẽ các ô cửa sổ đèn nhấp nháy
                val cols = 4
                val rows = (bHeight / 15f).toInt().coerceAtLeast(3)
                val padX = bWidth / (cols + 1)
                val padY = bHeight / (rows + 1)

                for (r in 1..rows) {
                    for (c in 1..cols) {
                        val winX = startX + c * padX - 3f
                        val winY = topY + r * padY - 3f

                        val blinkFactor = when ((bIndex * 11 + r * 5 + c * 3) % 4) {
                            0 -> fastBlink1
                            1 -> fastBlink2
                            2 -> fastBlink3
                            else -> fastBlink4
                        }

                        val isYellowLight = ((bIndex + r + c) % 4) != 0
                        val baseColor = if (isYellowLight) GOLD_ACCENT else GOLD_BRIGHT

                        // 1. Vầng sáng tỏa ra (Glow) quanh ô cửa sổ
                        if (blinkFactor > 0.45f) {
                            drawRect(
                                color = baseColor.copy(alpha = 0.25f * blinkFactor),
                                topLeft = Offset(winX - 1.5f, winY - 1.5f),
                                size = Size(9f, 9f)
                            )
                        }

                        // 2. Tâm bóng đèn sáng rực
                        drawRect(
                            color = baseColor.copy(alpha = (0.15f + 0.85f * blinkFactor).coerceIn(0f, 1f)),
                            topLeft = Offset(winX, winY),
                            size = Size(6f, 6f)
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
                color = GOLD_ACCENT.copy(alpha = 0.75f),
                start = Offset((w - roadTopW) / 2f, horizonY),
                end = Offset((w - roadBottomW) / 2f, bottomY),
                strokeWidth = 3f
            )
            drawLine(
                color = GOLD_ACCENT.copy(alpha = 0.75f),
                start = Offset((w + roadTopW) / 2f, horizonY),
                end = Offset((w + roadBottomW) / 2f, bottomY),
                strokeWidth = 3f
            )

            // Vạch kẻ giữa làn đường di chuyển
            val centerX = w / 2f
            val totalDashes = 7
            for (i in 0..totalDashes) {
                val progress = ((i.toFloat() + roadOffset) % totalDashes) / totalDashes.toFloat()
                val dashY = horizonY + (bottomY - horizonY) * (progress * progress)
                val dashH = 10f + 32f * progress
                val dashW = 2.5f + 5f * progress

                drawRect(
                    color = ROAD_MARKING.copy(alpha = 0.3f + 0.7f * progress),
                    topLeft = Offset(centerX - dashW / 2f, dashY),
                    size = Size(dashW, dashH)
                )
            }
        }

        // HÌNH ẢNH XE MAZDA CX-5 ĐỎ Ở GIỮA ĐƯỜNG
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
