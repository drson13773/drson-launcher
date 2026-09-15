package com.drson.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircularLuxurySpeedometer(
    speedKmH: Float = 0f,
    maxSpeed: Float = 220f,
    modifier: Modifier = Modifier
) {
    val animatedSpeed by animateFloatAsState(
        targetValue = speedKmH.coerceIn(0f, maxSpeed),
        animationSpec = tween(durationMillis = 300),
        label = "speed_anim"
    )

    Box(
        modifier = modifier.size(135.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            val startAngle = 140f
            val sweepAngle = 260f

            // 1. Cung nền viền kim loại tối
            drawArc(
                color = Color(0xFF2B261D),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 2. Cung hiển thị tốc độ phát sáng màu Vàng Gold
            val currentSweep = (animatedSpeed / maxSpeed) * sweepAngle
            if (currentSweep > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to Color(0xFFD4AF37),
                        0.7f to Color(0xFFFFF0B8),
                        1.0f to Color(0xFFFFD700)
                    ),
                    startAngle = startAngle,
                    sweepAngle = currentSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // 3. Vạch chia tốc độ xung quanh
            val totalTicks = 22
            for (i in 0..totalTicks) {
                val tickAngle = startAngle + (i.toFloat() / totalTicks) * sweepAngle
                val rad = Math.toRadians(tickAngle.toDouble())
                val isMajor = i % 2 == 0
                val tickLen = if (isMajor) 10.dp.toPx() else 6.dp.toPx()
                val innerR = radius - strokeWidth / 2f - 4.dp.toPx()
                val outerR = innerR - tickLen

                val startX = (center.x + innerR * cos(rad)).toFloat()
                val startY = (center.y + innerR * sin(rad)).toFloat()
                val endX = (center.x + outerR * cos(rad)).toFloat()
                val endY = (center.y + outerR * sin(rad)).toFloat()

                drawLine(
                    color = if (isMajor) Color(0xFFFFF0B8) else Color(0xFF7A6B4E),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                )
            }
        }

        // 4. Số tốc độ và đơn vị km/h ở giữa
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${animatedSpeed.toInt()}",
                color = Color(0xFFFFF0B8),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "km/h",
                color = Color(0xFFD4AF37),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
