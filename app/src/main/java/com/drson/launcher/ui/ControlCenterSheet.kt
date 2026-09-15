package com.drson.launcher.ui

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ControlCenterSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val wifiManager = remember {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    }

    var isWifiEnabled by remember {
        mutableStateOf(wifiManager?.isWifiEnabled ?: false)
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .combinedClickable(
                    onClick = onDismiss,
                    onLongClick = {}
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .padding(top = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .combinedClickable(onClick = {}, onLongClick = {}),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1A1814)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Tiêu đề Trung tâm điều khiển
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Trung tâm điều khiển",
                            color = Color(0xFFFFF0B8),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Cài đặt chung",
                                tint = Color(0xFFD4AF37)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Lưới phím tắt chức năng nhanh
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // 1. NÚT WIFI: Chạm để bật/tắt (Android < 10) hoặc mở panel (Android 10+) - Nhấn giữ để vào Cài đặt Wi-Fi chi tiết
                        QuickToggleItem(
                            icon = if (isWifiEnabled) Icons.Default.Wifi else Icons.Default.WifiOff,
                            label = "Wi-Fi",
                            isActive = isWifiEnabled,
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    // Android 10+ mở panel kết nối mạng nhanh
                                    try {
                                        context.startActivity(
                                            Intent(Settings.Panel.ACTION_WIFI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    } catch (_: Exception) {
                                        context.startActivity(
                                            Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    }
                                } else {
                                    @Suppress("DEPRECATION")
                                    val newState = !(wifiManager?.isWifiEnabled ?: false)
                                    @Suppress("DEPRECATION")
                                    wifiManager?.isWifiEnabled = newState
                                    isWifiEnabled = newState
                                }
                            },
                            onLongClick = {
                                // NHẤN GIỮ: MỞ TRỰC TIẾP CÀI ĐẶT WI-FI HỆ THỐNG
                                Toast.makeText(context, "Mở cài đặt Wi-Fi", Toast.LENGTH_SHORT).show()
                                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                                onDismiss()
                            }
                        )

                        // 2. NÚT BLUETOOTH
                        QuickToggleItem(
                            icon = Icons.Default.Bluetooth,
                            label = "Bluetooth",
                            isActive = true,
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            },
                            onLongClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                                onDismiss()
                            }
                        )

                        // 3. NÚT ÂM LƯỢNG
                        QuickToggleItem(
                            icon = Icons.Default.VolumeUp,
                            label = "Âm thanh",
                            isActive = true,
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            },
                            onLongClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                                onDismiss()
                            }
                        )

                        // 4. NÚT ĐỘ SÁNG MÀN HÌNH
                        QuickToggleItem(
                            icon = Icons.Default.BrightnessMedium,
                            label = "Màn hình",
                            isActive = true,
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            },
                            onLongClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickToggleItem(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isActive) Color(0xFFD4AF37) else Color(0xFF2A2722))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color(0xFF141311) else Color(0xFF888888),
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = if (isActive) Color(0xFFFFF0B8) else Color(0xFF888888),
            fontSize = 12.sp
        )
    }
}
