package com.drson.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.drson.launcher.accessibility.LauncherAccessibilityService
import com.drson.launcher.model.AppItem

/**
 * App Switcher hiển thị các app người dùng vừa mở QUA LAUNCHER NÀY (xem ghi chú trong
 * HomeViewModel.recentApps) - Android không cấp quyền đọc "recent tasks" hay live-thumbnail
 * thật của hệ thống cho app thường. Có thêm nút mở Recents thật của hệ thống qua Accessibility
 * Service (tuỳ chọn, cần bật 1 lần) cho ai muốn xem đúng đa nhiệm thật của Android.
 */
@Composable
fun AppSwitcherOverlay(
    isOpen: Boolean,
    recentApps: List<AppItem>,
    onDismiss: () -> Unit,
    onOpenApp: (AppItem) -> Unit,
    onRemoveFromRecents: (AppItem) -> Unit,
) {
    val context = LocalContext.current

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(150)),
        modifier = Modifier.zIndex(30f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onDismiss),
        ) {
            AnimatedVisibility(
                visible = isOpen,
                enter = slideInVertically(animationSpec = tween(220)) { it },
                exit = slideOutVertically(animationSpec = tween(200)) { it },
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .clickable(enabled = false) {}, // chặn tap lan ra ngoài làm đóng overlay
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Ứng dụng gần đây",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        SystemRecentsButton(context)
                    }

                    if (recentApps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "Chưa có ứng dụng nào được mở gần đây",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                            )
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 28.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(recentApps, key = { it.packageName }) { app ->
                                RecentAppCard(
                                    app = app,
                                    onTap = { onOpenApp(app) },
                                    onSwipedAway = { onRemoveFromRecents(app) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemRecentsButton(context: android.content.Context) {
    Text(
        "Recents hệ thống",
        color = Color.White.copy(alpha = 0.85f),
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .clickable {
                val opened = LauncherAccessibilityService.openSystemRecents()
                if (!opened) {
                    // Service chưa bật -> đưa người dùng tới màn hình bật Accessibility (chỉ cần làm 1 lần).
                    LauncherAccessibilityService.openAccessibilitySettings(context)
                }
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun RecentAppCard(
    app: AppItem,
    onTap: () -> Unit,
    onSwipedAway: () -> Unit,
) {
    // Vuốt lên trên card để "đóng" app khỏi danh sách gần đây (không tắt tiến trình thật - xem ghi chú class).
    var offsetY by remember { mutableStateOf(0f) }
    var isBeingRemoved by remember { mutableStateOf(false) }
    val dismissThresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { 120.dp.toPx() }

    Column(
        modifier = Modifier
            .width(180.dp)
            .graphicsLayer {
                translationY = offsetY
                alpha = 1f - (-offsetY / dismissThresholdPx).coerceIn(0f, 1f) * 0.6f
            }
            .pointerInput(app.packageName) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (offsetY < -dismissThresholdPx) {
                            isBeingRemoved = true
                            onSwipedAway()
                        } else {
                            offsetY = 0f
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        // Chỉ cho kéo lên (giá trị âm), giống thao tác "đóng app" trên iPadOS.
                        offsetY = (offsetY + dragAmount).coerceAtMost(0f)
                    },
                )
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.14f))
                .clickable(onClick = onTap),
            contentAlignment = Alignment.Center,
        ) {
            // Không có API hệ thống cho app thường lấy live-thumbnail thật của app khác,
            // nên hiển thị icon lớn thay cho ảnh chụp màn hình (xem ghi chú đầu file).
            Image(
                bitmap = app.icon,
                contentDescription = app.label,
                modifier = Modifier
                    .size(72.dp)
                    .clip(SquircleShape(0.6f)),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            app.label,
            color = Color.White,
            fontSize = 13.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
