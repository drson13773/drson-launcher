package com.drson.launcher.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.drson.launcher.R

private val GOLD_BRIGHT = Color(0xFFFFF0B8)
private val GOLD_ACCENT = Color(0xFFD4AF37)
private val ROAD_COLOR = Color(0xFF191715)
private val ROAD_MARKING = Color(0xFFE5C158)

@Composable
fun DrivingRoadBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "driving_sync_anim")
    
    // Tốc độ giảm đi một nửa: Tăng thời gian từ 1600ms lên 3200ms
    val roadProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "road_progress"
    )

    // Hiệu ứng đèn các tòa nhà nhấp nháy
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

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()
        val density = LocalDensity.current

        // Tỷ lệ chuẩn gốc
        val horizonY = screenHeightPx * 0.44f
        val bottomY = screenHeightPx * 0.88f
        val roadTopW = screenWidthPx * 0.16f
        val roadBottomW = screenWidthPx * 0.82f
        val centerX = screenWidthPx / 2f

        Canvas(modifier = Modifier.fillMaxSize()) {
            // 1. Bầu trời đêm
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

            // 2. Dãy tòa nhà
            val buildingConfigs = listOf(
                Triple(screenWidthPx * 0.08f, 75f, 110f),
                Triple(screenWidthPx * 0.16f, 65f, 140f),
                Triple(screenWidthPx * 0.23f, 70f, 95f),
                Triple(screenWidthPx * 0.30f, 60f, 130f),
                Triple(screenWidthPx * 0.36f, 55f, 85f),
                Triple(screenWidthPx * 0.45f, 90f, 150f),
                Triple(screenWidthPx * 0.55f, 60f, 90f),
                Triple(screenWidthPx * 0.62f, 65f, 135f),
                Triple(screenWidthPx * 0.70f, 75f, 115f),
                Triple(screenWidthPx * 0.78f, 70f, 145f),
                Triple(screenWidthPx * 0.86f, 80f, 100f)
            )

            buildingConfigs.forEachIndexed { bIndex, (startX, bWidth, bHeight) ->
                val topY = horizonY - bHeight
                drawRect(
                    color = Color(0xFF0C0B0A),
                    topLeft = Offset(startX, topY),
                    size = Size(bWidth, bHeight)
                )

                drawLine(
                    color = Color(0xFF332D23),
                    start = Offset(startX, topY),
                    end = Offset(startX + bWidth, topY),
                    strokeWidth = 1.8f
                )

                val cols = 4
                val rows = (bHeight / 15f).toInt().coerceAtLeast(3)
                val padX = bWidth / (cols + 1)
                val padY = bHeight / (rows + 1)

                for (r in 1..rows) {
                    for (c in 1..cols) {
                        val winX = startX + c * padX - 3f
                        val winY = topY + r * padY - 3f

                        val blinkFactor = if ((bIndex + r + c) % 2 == 0) fastBlink1 else fastBlink2
                        val baseColor = if (((bIndex + r + c) % 4) != 0) GOLD_ACCENT else GOLD_BRIGHT

                        drawRect(
                            color = baseColor.copy(alpha = (0.15f + 0.85f * blinkFactor).coerceIn(0f, 1f)),
                            topLeft = Offset(winX, winY),
                            size = Size(6f, 6f)
                        )
                    }
                }
            }

            // 3. Con đường 3D
            val roadPath = Path().apply {
                moveTo((screenWidthPx - roadTopW) / 2f, horizonY)
                lineTo((screenWidthPx + roadTopW) / 2f, horizonY)
                lineTo((screenWidthPx + roadBottomW) / 2f, bottomY)
                lineTo((screenWidthPx - roadBottomW) / 2f, bottomY)
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

            // Vạch mép đường vàng kim
            drawLine(
                color = GOLD_ACCENT.copy(alpha = 0.75f),
                start = Offset((screenWidthPx - roadTopW) / 2f, horizonY),
                end = Offset((screenWidthPx - roadBottomW) / 2f, bottomY),
                strokeWidth = 3f
            )
            drawLine(
                color = GOLD_ACCENT.copy(alpha = 0.75f),
                start = Offset((screenWidthPx + roadTopW) / 2f, horizonY),
                end = Offset((screenWidthPx + roadBottomW) / 2f, bottomY),
                strokeWidth = 3f
            )

            // Vạch kẻ làn giữa trôi chậm đồng bộ
            val totalDashes = 6
            for (i in 0 until totalDashes) {
                val p = ((i.toFloat() / totalDashes) + roadProgress) % 1.0f
                val dashY = horizonY + (bottomY - horizonY) * (p * p)
                val dashH = 10f + 32f * p
                val dashW = 2.5f + 5f * p

                drawRect(
                    color = ROAD_MARKING.copy(alpha = 0.3f + 0.7f * p),
                    topLeft = Offset(centerX - dashW / 2f, dashY),
                    size = Size(dashW, dashH)
                )
            }
        }

        // 4. MẶT TRĂNG GÓC TRÊN BÊN PHẢI
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 28.dp)
                .size(76.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFFAEB).copy(alpha = 0.35f),
                            Color(0xFFFFDF73).copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension / 1.8f
                    ),
                    radius = size.minDimension / 1.8f,
                    center = center
                )
            }

            Image(
                painter = painterResource(id = R.drawable.bg_moon),
                contentDescription = "Moon",
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Fit
            )
        }

        // 5. CÂY BÊN TRÁI ĐƯỜNG: bg_tree_2 (Chuyển động chậm đồng bộ)
        val leftTreeP = (roadProgress + 0.5f) % 1.0f
        RoadsideMovingTree(
            treeRes = R.drawable.bg_tree_2,
            progress = leftTreeP,
            isRightSide = false,
            screenWidthPx = screenWidthPx,
            horizonY = horizonY,
            bottomY = bottomY,
            roadTopW = roadTopW,
            roadBottomW = roadBottomW,
            density = density
        )

        // 6. CÂY 1 BÊN PHẢI ĐƯỜNG: bg_tree_1 (Chuyển động chậm đồng bộ)
        val rightTree1P = roadProgress % 1.0f
        RoadsideMovingTree(
            treeRes = R.drawable.bg_tree_1,
            progress = rightTree1P,
            isRightSide = true,
            screenWidthPx = screenWidthPx,
            horizonY = horizonY,
            bottomY = bottomY,
            roadTopW = roadTopW,
            roadBottomW = roadBottomW,
            density = density
        )

        // 7. CÂY 2 BÊN PHẢI ĐƯỜNG: bg_tree_1 (Chuyển động chậm đồng bộ)
        val rightTree2P = (roadProgress + 0.5f) % 1.0f
        RoadsideMovingTree(
            treeRes = R.drawable.bg_tree_1,
            progress = rightTree2P,
            isRightSide = true,
            screenWidthPx = screenWidthPx,
            horizonY = horizonY,
            bottomY = bottomY,
            roadTopW = roadTopW,
            roadBottomW = roadBottomW,
            density = density
        )

        // 8. XE MAZDA CX-5 ĐỎ Ở TRUNG TÂM
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

@Composable
private fun RoadsideMovingTree(
    treeRes: Int,
    progress: Float,
    isRightSide: Boolean,
    screenWidthPx: Float,
    horizonY: Float,
    bottomY: Float,
    roadTopW: Float,
    roadBottomW: Float,
    density: androidx.compose.ui.unit.Density
) {
    val currentYPx = horizonY + (bottomY - horizonY) * (progress * progress)
    val currentRoadHalfW = (roadTopW + (roadBottomW - roadTopW) * (progress * progress)) / 2f
    val centerX = screenWidthPx / 2f

    val baseWidthDp = 26f + 95f * progress
    val baseHeightDp = 30f + 115f * progress

    val widthPx = with(density) { baseWidthDp.dp.toPx() }
    val heightPx = with(density) { baseHeightDp.dp.toPx() }

    val roadsideMargin = with(density) { (2f + 8f * progress).dp.toPx() }

    val currentXPx = if (isRightSide) {
        centerX + currentRoadHalfW + roadsideMargin
    } else {
        centerX - currentRoadHalfW - roadsideMargin - widthPx
    }

    val alpha = when {
        progress < 0.08f -> progress / 0.08f
        progress > 0.88f -> (1.0f - progress) / 0.12f
        else -> 1.0f
    }.coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .offset(
                x = with(density) { currentXPx.toDp() },
                y = with(density) { (currentYPx - heightPx * 0.95f).toDp() }
            )
            .size(width = baseWidthDp.dp, height = baseHeightDp.dp)
            .alpha(alpha)
    ) {
        Image(
            painter = painterResource(id = treeRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}
