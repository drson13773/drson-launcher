package com.drson.launcher.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GOLD_BRIGHT = Color(0xFFFFF0B8)
private val GOLD_ACCENT = Color(0xFFD4AF37)
private val GOLD_MUTED = Color(0xFFC7A75C)
private val DARK_CARD_BG = Color(0xFF14120E)

data class LuxuryNotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val time: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun NotificationPanelOverlay(
    isOpen: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val panelWidth = (configuration.screenWidthDp * 0.46f).dp

    var notifications by remember {
        mutableStateOf(
            listOf(
                LuxuryNotificationItem(
                    id = "1",
                    title = "Hệ thống Mazda CX-5",
                    message = "Áp suất lốp 4 bánh ổn định (2.3 bar). Xe sẵn sàng khởi hành.",
                    time = "Vừa xong",
                    icon = Icons.Default.DirectionsCar
                ),
                LuxuryNotificationItem(
                    id = "2",
                    title = "Định vị vệ tinh GPS",
                    message = "Đã khóa 16 vệ tinh. Độ chính xác vận tốc đạt mức tối đa.",
                    time = "10 phút trước",
                    icon = Icons.Default.GpsFixed
                ),
                LuxuryNotificationItem(
                    id = "3",
                    title = "Kiki Auto Assistant",
                    message = "Trợ lý giọng nói đã sẵn sàng nhận lệnh từ phím vô lăng.",
                    time = "Hôm nay",
                    icon = Icons.Default.Mic
                )
            )
        )
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 })
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.TopStart
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 16.dp, start = 20.dp, bottom = 24.dp)
                    .width(panelWidth)
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0D0C0A).copy(alpha = 0.96f))
                    .border(1.2.dp, GOLD_ACCENT.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                    .clickable(enabled = false) {}
                    .padding(18.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // HEADER THÔNG BÁO
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E1A14))
                                    .border(1.dp, GOLD_ACCENT.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = GOLD_BRIGHT,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Thông báo hệ thống",
                                color = GOLD_BRIGHT,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (notifications.isNotEmpty()) {
                                TextButton(
                                    onClick = { notifications = emptyList() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Xóa hết", color = GOLD_MUTED, fontSize = 11.5.sp)
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.06f))
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = GOLD_BRIGHT,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = GOLD_ACCENT.copy(alpha = 0.2f)
                    )

                    // DANH SÁCH THÔNG BÁO
                    if (notifications.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Không có thông báo mới",
                                color = GOLD_MUTED.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(notifications.size) { index ->
                                val item = notifications[index]
                                NotificationCard(
                                    item = item,
                                    onDelete = {
                                        notifications = notifications.filter { it.id != item.id }
                                    }
                                )
                            }
                        }
                    }

                    // FOOTER: CÀI ĐẶT THÔNG BÁO HỆ THỐNG
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DARK_CARD_BG)
                            .border(1.dp, GOLD_ACCENT.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .clickable {
                                try {
                                    context.startActivity(
                                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                    )
                                    onDismiss()
                                } catch (_: Exception) {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                    } catch (_: Exception) {}
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Mở cài đặt thông báo Android",
                            color = GOLD_MUTED,
                            fontSize = 12.sp
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = GOLD_ACCENT,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    item: LuxuryNotificationItem,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DARK_CARD_BG)
            .border(1.dp, GOLD_ACCENT.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF221D15))
                .border(1.dp, GOLD_ACCENT.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = GOLD_BRIGHT,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    color = GOLD_BRIGHT,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.time,
                    color = GOLD_MUTED,
                    fontSize = 10.5.sp
                )
            }
            Text(
                text = item.message,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = GOLD_MUTED.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
