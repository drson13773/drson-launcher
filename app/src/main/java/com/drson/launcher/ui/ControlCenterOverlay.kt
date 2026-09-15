package com.drson.launcher.ui

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.wifi.WifiManager
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val GOLD_BRIGHT = Color(0xFFFFF0B8)
private val GOLD_ACCENT = Color(0xFFD4AF37)
private val DARK_CARD_BG = Color(0xFF14120E)
private val ACTIVE_BG = Color(0xFF282319)

@Composable
fun ControlCenterOverlay(
    isOpen: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVolume = remember { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15 }

    var isWifiOn by remember { mutableStateOf(true) }
    var isBluetoothOn by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var volumeLevel by remember {
        mutableFloatStateOf(
            ((audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 8).toFloat() / maxVolume).coerceIn(0f, 1f)
        )
    }
    var brightnessLevel by remember { mutableFloatStateOf(0.75f) }

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
            contentAlignment = Alignment.TopEnd
        ) {
            // Khung Trung tâm điều khiển chính
            Box(
                modifier = Modifier
                    .padding(top = 16.dp, end = 20.dp)
                    .width(360.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0D0C0A).copy(alpha = 0.96f))
                    .border(1.2.dp, GOLD_ACCENT.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                    .clickable(enabled = false) {}
                    .padding(18.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // HÀNG 1: 4 KHỐI CHỨC NĂNG RIÊNG BIỆT (KHÔNG CÓ NHÃN CHỮ)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ControlTile(
                            icon = Icons.Default.Wifi,
                            isActive = isWifiOn,
                            onClick = {
                                isWifiOn = !isWifiOn
                                try {
                                    context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                } catch (_: Exception) {}
                            }
                        )

                        ControlTile(
                            icon = Icons.Default.Bluetooth,
                            isActive = isBluetoothOn,
                            onClick = {
                                isBluetoothOn = !isBluetoothOn
                                try {
                                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                } catch (_: Exception) {}
                            }
                        )

                        ControlTile(
                            icon = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            isActive = !isMuted,
                            onClick = {
                                isMuted = !isMuted
                                val targetVol = if (isMuted) 0 else (volumeLevel * maxVolume).toInt()
                                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                            }
                        )

                        ControlTile(
                            icon = Icons.Default.Settings,
                            isActive = false,
                            onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                    onDismiss()
                                } catch (_: Exception) {}
                            }
                        )
                    }

                    // HÀNG 2: 2 CỘT THANH TRƯỢT ÂM LƯỢNG & ĐỘ SÁNG THON GỌN
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Khối Âm Lượng (Icon Loa)
                        CompactSliderBlock(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.VolumeUp,
                            value = volumeLevel,
                            onValueChange = {
                                volumeLevel = it
                                isMuted = (it == 0f)
                                audioManager?.setStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    (it * maxVolume).toInt(),
                                    0
                                )
                            }
                        )

                        // Khối Độ Sáng (Icon Mặt Trời)
                        CompactSliderBlock(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.WbSunny,
                            value = brightnessLevel,
                            onValueChange = {
                                brightnessLevel = it
                                try {
                                    val brightnessInt = (it * 255).toInt().coerceIn(10, 255)
                                    Settings.System.putInt(
                                        context.contentResolver,
                                        Settings.System.SCREEN_BRIGHTNESS,
                                        brightnessInt
                                    )
                                } catch (_: Exception) {}
                            }
                        )
                    }

                    // HÀNG 3: 4 KHỐI CHỨC NĂNG BỔ TRỢ (GPS, KHÓA MÀN HÌNH, TRỢ LÝ, ĐÓNG)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ControlTile(
                            icon = Icons.Default.LocationOn,
                            isActive = true,
                            onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                } catch (_: Exception) {}
                            }
                        )

                        ControlTile(
                            icon = Icons.Default.DarkMode,
                            isActive = true,
                            onClick = {}
                        )

                        ControlTile(
                            icon = Icons.Default.ScreenLockPortrait,
                            isActive = false,
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_MAIN).apply {
                                        addCategory(Intent.CATEGORY_HOME)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    })
                                    onDismiss()
                                } catch (_: Exception) {}
                            }
                        )

                        ControlTile(
                            icon = Icons.Default.Close,
                            isActive = false,
                            onClick = onDismiss
                        )
                    }
                }
            }
        }
    }
}

// KHỐI VUÔNG CHỨC NĂNG TỐI GIẢN (KHÔNG CHỮ)
@Composable
private fun ControlTile(
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isActive) ACTIVE_BG else DARK_CARD_BG)
            .border(
                1.dp,
                if (isActive) GOLD_ACCENT else GOLD_ACCENT.copy(alpha = 0.25f),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) GOLD_BRIGHT else GOLD_ACCENT.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )
    }
}

// KHỐI THANH TRƯỢT THON GỌN (CHỈ GIỮ ICON LOA / MẶT TRỜI)
@Composable
private fun CompactSliderBlock(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DARK_CARD_BG)
            .border(1.dp, GOLD_ACCENT.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E1A14)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GOLD_BRIGHT,
                modifier = Modifier.size(17.dp)
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = GOLD_BRIGHT,
                activeTrackColor = GOLD_ACCENT,
                inactiveTrackColor = Color.White.copy(alpha = 0.12f)
            ),
            modifier = Modifier
                .weight(1f)
                .height(18.dp)
        )
    }
}
