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

@Composable
fun AppDrawerOverlay(
    isOpen: Boolean,
    apps: List<AppItem>,
    onDismiss: () -> Unit,
    onPick: (AppItem) -> Unit
) {
    if (!isOpen) return

    val infiniteTransition = rememberInfiniteTransition(label = "drawer_rays_anim")
    val drawerRotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drawer_rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .clickable(onClick = onDismiss)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Tia sáng dài xoay nhanh phía sau nền menu
        Canvas(
            modifier = Modifier
                .size(520.dp)
                .rotate(drawerRotationAngle)
        ) {
            drawSunburstRays(maxRadius = 260f, rayCount = 20)
        }

        // Logo Dr Sơn mờ phía sau danh sách ứng dụng
        Image(
            painter = painterResource(id = R.drawable.icon_menu_brand),
            contentDescription = null,
            modifier = Modifier.size(170.dp),
            alpha = 0.35f
        )

        // Lưới icon ứng dụng
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TẤT CẢ ỨNG DỤNG",
                    color = GOLD_BRIGHT,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = GOLD_ACCENT)
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 90.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(apps) { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPick(app) }
                            .padding(8.dp)
                    ) {
                        Image(
                            bitmap = app.icon,
                            contentDescription = app.label,
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = app.label,
                            color = Color.White,
                            fontSize = 11.sp,
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
        val angleRad1 = Math.toRadians((angleDeg - 4.5).toDouble())
        val angleRad2 = Math.toRadians((angleDeg + 4.5).toDouble())

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
                    GOLD_ACCENT.copy(alpha = 0.55f),
                    GOLD_BRIGHT.copy(alpha = 0.25f),
                    Color.Transparent
                ),
                center = center,
                radius = maxRadius
            )
        )
    }
}
