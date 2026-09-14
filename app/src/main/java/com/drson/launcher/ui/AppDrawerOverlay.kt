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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
            // --- HÌNH NỀN STACKED LOGO TRUNG TÂM (LÀM MỜ NỀN) ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.30f), // Độ mờ vừa phải để không rối mắt khi nhìn icon
                contentAlignment = Alignment.Center
            ) {
                // 1. Quầng hào quang tia sáng xoay nhẹ
                Canvas(modifier = Modifier.size(340.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxR = size.minDimension / 2f

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GOLD_SOLID.copy(alpha = 0.22f),
                                GOLD_SOLID.copy(alpha = 0.06f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = maxR
                        ),
                        radius = maxR,
                        center = center
                    )

                    val nRays = 28
                    for (i in 0 until nRays) {
                        val angleDeg = i * (360f / nRays)
                        val angleRad = Math.toRadians(angleDeg.toDouble())
                        val rayLen = maxR * (0.6f + 0.35f * abs(sin(Math.toRadians((angleDeg * 2).toDouble()))).toFloat())
                        val innerR = maxR * 0.25f
                        val p1 = Offset(
                            center.x + (innerR * cos(angleRad)).toFloat(),
                            center.y + (innerR * sin(angleRad)).toFloat()
                        )
                        val p2 = Offset(
                            center.x + (rayLen * cos(angleRad)).toFloat(),
                            center.y + (rayLen * sin(angleRad)).toFloat()
                        )
                        drawLine(
                            color = GOLD_BRIGHT.copy(alpha = 0.18f),
                            start = p1,
                            end = p2,
                            strokeWidth = 2.0f
                        )
                    }
                }

                // 2. Cụm Logo Xếp Dọc: Bác sĩ -> Chữ Dr Sơn -> Con dao mổ hướng từ phải qua trái
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Tầng 1: Hình Bác Sĩ
                    Image(
                        painter = painterResource(id = R.drawable.brand_glyph),
                        contentDescription = null,
                        modifier = Modifier.size(110.dp),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(Modifier.height(4.dp))

                    // Tầng 2: Chữ Dr Sơn
                    Text(
                        text = "Dr Sơn",
                        color = GOLD_BRIGHT,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily(Font(R.font.alex_brush)),
                        style = TextStyle(
                            textAlign = TextAlign.Center
                        )
                    )

                    Spacer(Modifier.height(4.dp))

                    // Tầng 3: Con dao mổ làm gạch chân (Lật scaleX = -1f để hướng lưỡi dao từ phải qua trái)
                    Image(
                        painter = painterResource(id = R.drawable.icon_scalpel),
                        contentDescription = null,
                        modifier = Modifier
                            .width(100.dp)
                            .height(20.dp)
                            .scale(scaleX = -1f, scaleY = 1f), // Hướng lưỡi dao từ phải sang trái
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Lớp phủ Gradient mờ trên dưới
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.55f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.65f)
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
                // Thanh Header: Tiêu đề + Tìm kiếm + Nút đóng
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
                        fontWeight = FontWeight.SemiBold
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

                // Lưới ứng dụng: 7 cột, icon 50dp, khoảng cách gọn
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
