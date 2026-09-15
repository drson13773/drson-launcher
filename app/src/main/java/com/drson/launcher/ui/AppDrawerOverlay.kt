package com.drson.launcher.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drson.launcher.R
import com.drson.launcher.model.AppItem
import kotlin.math.cos
import kotlin.math.sin

private val GOLD_BRIGHT = Color(0xFFFFF0B8)
private val GOLD_ACCENT = Color(0xFFD4AF37)
private val DRAWER_BG = Color(0xFF0C0B0A)

@Composable
fun AppDrawerOverlay(
    isOpen: Boolean,
    apps: List<AppItem>,
    onDismiss: () -> Unit,
    onPick: (AppItem) -> Unit
) {
    if (!isOpen) return

    val infiniteTransition = rememberInfiniteTransition(label = "drawer_rays_slow")
    val drawerRotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drawer_rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DRAWER_BG)
            .clickable(onClick = onDismiss)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Logo & Tia sáng ở góc dưới bên trái
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 10.dp, bottom = 10.dp)
                .size(200.dp)
                .alpha(0.16f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(drawerRotationAngle)
            ) {
                drawSunburstRays(maxRadius = size.minDimension / 2f, rayCount = 16)
            }

            Image(
                painter = painterResource(id = R.drawable.icon_menu_brand),
                contentDescription = null,
                modifier = Modifier.size(90.dp),
                contentScale = ContentScale.Fit
            )
        }

        // Danh sách ứng dụng: 8 Cột, chữ tên ứng dụng màu Vàng Kim
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TẤT CẢ ỨNG DỤNG",
                    color = GOLD_BRIGHT,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = GOLD_ACCENT, modifier = Modifier.size(20.dp))
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 6.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(apps) { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onPick(app) }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        Image(
                            bitmap = app.icon,
                            contentDescription = app.label,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = app.label,
                            color = GOLD_BRIGHT, // Tên app toàn bộ chuyển sang màu Vàng Kim
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawSunburstRays(maxRadius: Float, rayCount: Int) {
    val center = Offset(size.width / 2f, size.height / 2f)
    for (i in 0 until rayCount) {
        val angleDeg = i * (360f / rayCount)
        val angleRad1 = Math.toRadians((angleDeg - 5.0).toDouble())
        val angleRad2 = Math.toRadians((angleDeg + 5.0).toDouble())

        val rayPath = Path().apply {
            moveTo(center.x, center.y)
            lineTo(
                (center.x + maxRadius * cos(angleRad1)).toFloat(),
                (center.y + maxRadius * sin(angleRad1)).toFloat()
            )
            lineTo(
                (center.x + maxRadius * cos(angleRad2)).toFloat(),
                (center.y + maxRadius * sin(angleRad2)).toFloat()
            )
            close()
        }

        drawPath(
            path = rayPath,
            brush = Brush.radialGradient(
                colors = listOf(
                    GOLD_ACCENT.copy(alpha = 0.6f),
                    GOLD_BRIGHT.copy(alpha = 0.3f),
                    Color.Transparent
                ),
                center = center,
                radius = maxRadius
            )
        )
    }
}
