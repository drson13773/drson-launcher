package com.drson.launcher.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import java.lang.reflect.Method

private val GOLD_BRIGHT = Color(0xFFFFF0B8)
private val GOLD_ACCENT = Color(0xFFD4AF37)
private val DARK_CARD_BG = Color(0xFF14120E)
private val ACTIVE_BG = Color(0xFF2E2614)

@SuppressLint("MissingPermission")
@Composable
fun ControlCenterOverlay(
    isOpen: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVolume = remember { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15 }

    // Quản lý Wi-Fi
    val wifiManager = remember { context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager }
    var isWifiOn by remember { mutableStateOf(wifiManager?.isWifiEnabled ?: false) }

    // Quản lý Bluetooth
    val bluetoothManager = remember { context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager }
    val bluetoothAdapter: BluetoothAdapter? = remember { bluetoothManager?.adapter }
    var isBluetoothOn by remember { mutableStateOf(bluetoothAdapter?.isEnabled ?: false) }

    // Quản lý Dữ liệu di động 4G
    val telephonyManager = remember { context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager }
    var isDataOn by remember { mutableStateOf(checkMobileDataState(context, telephonyManager)) }

    // Xin quyền Bluetooth trên Android 12+ nếu chưa có
    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            toggleBluetoothDirectly(bluetoothAdapter) { isBluetoothOn = it }
        }
    }

    var isMuted by remember { mutableStateOf(false) }
    var volumeLevel by remember {
        mutableFloatStateOf(
            ((audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 8).toFloat() / maxVolume).coerceIn(0f, 1f)
        )
    }
    var brightnessLevel by remember { mutableFloatStateOf(0.75f) }

    // Đồng bộ lại trạng thái thực tế mỗi khi mở bảng
    LaunchedEffect(isOpen) {
        if (isOpen) {
            isWifiOn = wifiManager?.isWifiEnabled ?: false
            isBluetoothOn = bluetoothAdapter?.isEnabled ?: false
            isDataOn = checkMobileDataState(context, telephonyManager)
        }
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
            contentAlignment = Alignment.TopEnd
        ) {
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
                    // HÀNG 1: WIFI, DATA 4G, BLUETOOTH, ÂM THANH
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 1. Nút Wi-Fi (Bật/tắt ăn ngay)
                        ControlTile(
                            icon = Icons.Default.Wifi,
                            isActive = isWifiOn,
                            onClick = {
                                val target = !isWifiOn
                                val success = setWifiDirectly(context, wifiManager, target)
                                if (success) {
                                    isWifiOn = target
                                } else {
                                    // Fallback sang giao diện nhanh nếu bản ROM chặn hẳn
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        context.startActivity(Intent(Settings.Panel.ACTION_WIFI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                    } else {
                                        context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                    }
                                }
                            }
                        )

                        // 2. Nút Dữ liệu 4G (Bật/tắt ăn ngay)
                        ControlTile(
                            icon = Icons.Default.SignalCellularAlt,
                            isActive = isDataOn,
                            onClick = {
                                val target = !isDataOn
                                val success = setMobileDataDirectly(telephonyManager, target)
                                if (success) {
                                    isDataOn = target
                                } else {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_DATA_ROAMING_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                    } catch (_: Exception) {
                                        context.startActivity(Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                    }
                                }
                            }
                        )

                        // 3. Nút Bluetooth (Bật/tắt ăn ngay)
                        ControlTile(
                            icon = Icons.Default.Bluetooth,
                            isActive = isBluetoothOn,
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                } else {
                                    toggleBluetoothDirectly(bluetoothAdapter) { isBluetoothOn = it }
                                }
                            }
                        )

                        // 4. Nút Mute / Âm thanh
                        ControlTile(
                            icon = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            isActive = !isMuted,
                            onClick = {
                                isMuted = !isMuted
                                val targetVol = if (isMuted) 0 else (volumeLevel * maxVolume).toInt().coerceAtLeast(1)
                                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                            }
                        )
                    }

                    // HÀNG 2: THANH TRƯỢT ÂM LƯỢNG & ĐỘ SÁNG
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
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

                    // HÀNG 3: VỊ TRÍ (GPS), CÀI ĐẶT XE, TẮT MÀN HÌNH, ĐÓNG
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
                            icon = Icons.Default.Settings,
                            isActive = false,
                            onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                    onDismiss()
                                } catch (_: Exception) {}
                            }
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

// HÀM CAN THIỆP PHẦN CỨNG TRỰC TIẾP (DIRECT AUTOMOTIVE CONTROLS)

@Suppress("DEPRECATION")
private fun setWifiDirectly(context: Context, wifiManager: WifiManager?, targetState: Boolean): Boolean {
    if (wifiManager == null) return false
    return try {
        // Thử cách chuẩn trực tiếp
        wifiManager.isWifiEnabled = targetState
        true
    } catch (_: Exception) {
        try {
            // Thử qua Reflection bypass SDK limit
            val method: Method = wifiManager.javaClass.getDeclaredMethod("setWifiEnabled", Boolean::class.javaPrimitiveType)
            method.isAccessible = true
            method.invoke(wifiManager, targetState)
            true
        } catch (_: Exception) {
            false
        }
    }
}

@SuppressLint("MissingPermission")
private fun toggleBluetoothDirectly(bluetoothAdapter: BluetoothAdapter?, onStateChanged: (Boolean) -> Unit) {
    if (bluetoothAdapter == null) return
    try {
        @Suppress("DEPRECATION")
        if (bluetoothAdapter.isEnabled) {
            bluetoothAdapter.disable()
            onStateChanged(false)
        } else {
            bluetoothAdapter.enable()
            onStateChanged(true)
        }
    } catch (_: Exception) {}
}

private fun checkMobileDataState(context: Context, telephonyManager: TelephonyManager?): Boolean {
    return try {
        val method = telephonyManager?.javaClass?.getDeclaredMethod("getDataEnabled")
        (method?.invoke(telephonyManager) as? Boolean) ?: run {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val net = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(net)
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        }
    } catch (_: Exception) {
        false
    }
}

private fun setMobileDataDirectly(telephonyManager: TelephonyManager?, targetState: Boolean): Boolean {
    if (telephonyManager == null) return false
    return try {
        val setMethod = telephonyManager.javaClass.getDeclaredMethod("setDataEnabled", Boolean::class.javaPrimitiveType)
        setMethod.isAccessible = true
        setMethod.invoke(telephonyManager, targetState)
        true
    } catch (_: Exception) {
        try {
            // Thử qua ITelephony Service ngầm của Android Automotive
            val getITelephony: Method = telephonyManager.javaClass.getDeclaredMethod("getITelephony")
            getITelephony.isAccessible = true
            val iTelephony = getITelephony.invoke(telephonyManager)
            val dataToggleMethod = iTelephony?.javaClass?.getDeclaredMethod(
                if (targetState) "enableDataConnectivity" else "disableDataConnectivity"
            )
            dataToggleMethod?.isAccessible = true
            dataToggleMethod?.invoke(iTelephony)
            true
        } catch (_: Exception) {
            false
        }
    }
}

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
