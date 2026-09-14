package com.drson.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drson.launcher.R
import com.drson.launcher.model.AppItem
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val GOLD_BRIGHT = Color(0xFFFFF0B8)
private val GOLD_SOLID = Color(0xFFFFD56B)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerOverlay(
    isOpen: Boolean,
    apps: List<AppItem>,
    title: String = "Tất cả ứng dụng",
    onDismiss: () -> Unit,
    onPick: (AppItem) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps
        else apps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 4 })
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0C0A08))
        ) {
            // --- CỤM LOGO NỀN GÓC DƯỚI BÊN TRÁI ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, bottom = 20.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .alpha(0.55f),
                    contentAlignment = Alignment.Center
                ) {
                    // Quầng sáng và tia hào quang tỏa ra từ góc dưới trái
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val maxR = size.minDimension / 2f

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    GOLD_SOLID.copy(alpha = 0.28f),
                                    GOLD_SOLID.copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = maxR
                            ),
                            radius = maxR,
                            center = center
                        )

                        val nRays = 24
                        for (i in 0 until nRays) {
                            val angleDeg = i * (360f / nRays)
                            val angleRad = Math.toRadians(angleDeg.toDouble())
                            val rayLen = maxR * (0.65f + 0.35f * abs(sin(Math.toRadians((angleDeg * 2).toDouble()))).toFloat())
                            val innerR = maxR * 0.20f
                            val p1 = Offset(
                                center.x + (innerR * cos(angleRad)).toFloat(),
                                center.y + (innerR * sin(angleRad)).toFloat()
                            )
                            val p2 = Offset(
                                center.x + (rayLen * cos(angleRad)).toFloat(),
                                center.y + (rayLen * sin(angleRad)).toFloat()
                            )
                            drawLine(
                                color = GOLD_BRIGHT.copy(alpha = 0.25f),
                                start = p1,
                                end = p2,
                                strokeWidth = 2.0f
                            )
                        }
                    }

                    // Hình ảnh logo đơn nhất, sắc nét với ánh kim
                    Image(
                        painter = painterResource(id = R.drawable.brand_glyph),
                        contentDescription = "Dr Sơn Brand",
                        modifier = Modifier
                            .size(130.dp)
                            .padding(bottom = 6.dp),
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(
                            color = GOLD_BRIGHT,
                            blendMode = BlendMode.SrcIn
                        )
                    )
                }
            }

            // Lớp phủ Gradient mờ
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.50f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.60f)
                            )
                        )
                    )
            )

            // --- NỘI DUNG DANH SÁCH ỨNG DỤNG ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                // Header: Tiêu đề + Tìm kiếm + Đóng
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        color = GOLD_BRIGHT,
                        fontSize = 18.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Tìm ứng dụng...", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GOLD_BRIGHT, modifier = Modifier.size(16.dp)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = GOLD_BRIGHT,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedContainerColor = Color.Black.copy(alpha = 0.45f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.35f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .width(220.dp)
                                .height(44.dp)
                        )

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.12f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Lưới icon ứng dụng (7 cột)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    items(filteredApps) { app ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onPick(app) }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            Image(
                                bitmap = app.icon,
                                contentDescription = app.label,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = app.label,
                                color = Color.White,
                                fontSize = 11.sp,
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
}
