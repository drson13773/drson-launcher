package com.drson.launcher.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.drson.launcher.notifications.LauncherNotificationListenerService
import com.drson.launcher.notifications.NotificationAccess
import com.drson.launcher.notifications.NotificationEntry
import com.drson.launcher.notifications.NotificationRepository
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * Trung tâm thông báo kiểu iPadOS: vuốt xuống từ NỬA TRÁI status bar để mở (nửa phải mở
 * Control Center - xem HomeScreen.kt). Đọc dữ liệu thật từ [NotificationRepository], được
 * [LauncherNotificationListenerService] ghi vào mỗi khi hệ thống có thông báo mới.
 */
@Composable
fun NotificationCenterOverlay(
    isOpen: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val notifications by NotificationRepository.notifications.collectAsState()
    var accessEnabled by remember { mutableStateOf(NotificationAccess.isEnabled(context)) }

    // Mỗi lần mở panel, kiểm tra lại quyền - người dùng có thể vừa bật/tắt trong Settings.
    LaunchedEffect(isOpen) {
        if (isOpen) accessEnabled = NotificationAccess.isEnabled(context)
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(150)),
        modifier = Modifier.zIndex(20f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(onClick = onDismiss),
        ) {
            AnimatedVisibility(
                visible = isOpen,
                enter = slideInVertically(animationSpec = tween(220)) { -it },
                exit = slideOutVertically(animationSpec = tween(200)) { -it },
                modifier = Modifier.align(Alignment.TopStart),
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp, start = 16.dp)
                        .width(360.dp)
                        .heightIn(max = 520.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xE6141210))
                        .clickable(enabled = false) {} // chặn tap trong panel lan ra ngoài
                        .padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Thông báo", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        if (accessEnabled && notifications.isNotEmpty()) {
                            Text(
                                "Xoá tất cả",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                modifier = Modifier.clickable { LauncherNotificationListenerService.dismissAll() },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    when {
                        !accessEnabled -> NotificationAccessPrompt(context)
                        notifications.isEmpty() -> Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "Không có thông báo nào",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                            )
                        }
                        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(notifications, key = { it.key }) { entry ->
                                NotificationCard(
                                    entry = entry,
                                    onTap = {
                                        entry.contentIntent?.let {
                                            try {
                                                it.send()
                                            } catch (e: Exception) {
                                                // PendingIntent đã bị huỷ (app gốc có thể đã đóng) - bỏ qua an toàn.
                                            }
                                        }
                                        onDismiss()
                                    },
                                    onDismissSwipe = { LauncherNotificationListenerService.dismiss(entry.key) },
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
private fun NotificationAccessPrompt(context: Context) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(16.dp),
    ) {
        Text(
            "Cần cấp quyền truy cập thông báo",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Android yêu cầu cấp quyền này thủ công 1 lần để launcher đọc được thông báo hệ thống.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Mở Cài đặt",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFC99E5C))
                .clickable { NotificationAccess.openSettings(context) }
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun NotificationCard(
    entry: NotificationEntry,
    onTap: () -> Unit,
    onDismissSwipe: () -> Unit,
) {
    var offsetX by remember { mutableStateOf(0f) }
    val dismissThresholdPx = with(LocalDensity.current) { 140.dp.toPx() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = offsetX
                alpha = 1f - (abs(offsetX) / dismissThresholdPx).coerceIn(0f, 1f) * 0.7f
            }
            .pointerInput(entry.key) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (abs(offsetX) > dismissThresholdPx) onDismissSwipe() else offsetX = 0f
                    },
                    onDragCancel = { offsetX = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount
                    },
                )
            }
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onTap)
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(SquircleShape(0.6f))
                .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            entry.icon?.let { drawable ->
                val bitmap = remember(drawable) { drawable.toBitmap().asImageBitmap() }
                Image(bitmap = bitmap, contentDescription = entry.appLabel, modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(entry.appLabel, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                Text(formatTime(entry.postTime), color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            }
            if (entry.title.isNotBlank()) {
                Text(
                    entry.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (entry.text.isNotBlank()) {
                Text(
                    entry.text,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
