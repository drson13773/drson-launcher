package com.drson.launcher.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.AirplanemodeInactive
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.SignalCellularOff
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.drson.launcher.R
import com.drson.launcher.hardware.DeviceControls

/** Tông màu đồng nhất với launcher: vàng đồng trên nền đen. */
private val GoldAccent = Color(0xFFC99E5C)
private val GoldAccentLight = Color(0xFFE6C178)
private val PanelBg = Color(0xE6141210)
private val CardOff = Color.White.copy(alpha = 0.10f)

/**
 * Panel Control Center kiểu iPadOS: vuốt xuống từ status bar để mở, chạm ra ngoài để đóng.
 * Đọc trạng thái Wi-Fi/Bluetooth/độ sáng/âm lượng/máy bay/data thật của máy khi mở lên.
 */
@Composable
fun ControlCenterOverlay(
    isOpen: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    var wifiOn by remember { mutableStateOf(false) }
    var bluetoothOn by remember { mutableStateOf(false) }
    var airplaneOn by remember { mutableStateOf(false) }
    var dataOn by remember { mutableStateOf(true) }
    var brightness by remember { mutableStateOf(0.5f) }
    var volume by remember { mutableStateOf(0.5f) }

    // Xin quyền BLUETOOTH_CONNECT bằng hộp thoại hệ thống ngay khi cần (Android 12+), thay vì
    // đẩy người dùng sang màn hình Cài đặt - chỉ cần cấp 1 lần rồi bấm là có tác dụng ngay.
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            DeviceControls.toggleBluetooth(context)
            bluetoothOn = DeviceControls.isBluetoothEnabled(context)
        }
    }

    // Mỗi lần mở panel, đọc lại trạng thái thật từ hệ thống (người dùng có thể đã đổi ở nơi khác).
    LaunchedEffect(isOpen) {
        if (isOpen) {
            wifiOn = DeviceControls.isWifiEnabled(context)
            bluetoothOn = DeviceControls.isBluetoothEnabled(context)
            airplaneOn = DeviceControls.isAirplaneModeOn(context)
            DeviceControls.isMobileDataEnabledOrNull(context)?.let { dataOn = it }
            brightness = DeviceControls.getBrightnessFraction(context)
            volume = DeviceControls.getVolumeFraction(context)
        }
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
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                ControlCenterPanel(
                    wifiOn = wifiOn,
                    onToggleWifi = {
                        DeviceControls.toggleWifi(context)
                        wifiOn = DeviceControls.isWifiEnabled(context)
                    },
                    bluetoothOn = bluetoothOn,
                    onToggleBluetooth = {
                        if (DeviceControls.hasBluetoothConnectPermission(context)) {
                            DeviceControls.toggleBluetooth(context)
                            bluetoothOn = DeviceControls.isBluetoothEnabled(context)
                        } else {
                            bluetoothPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT)
                        }
                    },
                    airplaneOn = airplaneOn,
                    onToggleAirplane = {
                        DeviceControls.toggleAirplaneMode(context)
                        airplaneOn = DeviceControls.isAirplaneModeOn(context)
                    },
                    dataOn = dataOn,
                    onToggleData = {
                        DeviceControls.toggleMobileData(context)
                        DeviceControls.isMobileDataEnabledOrNull(context)?.let { dataOn = it } ?: run { dataOn = !dataOn }
                    },
                    brightness = brightness,
                    onBrightnessChange = { value ->
                        brightness = value
                        if (DeviceControls.canWriteSystemSettings(context)) {
                            DeviceControls.setBrightnessFraction(context, value)
                        } else {
                            DeviceControls.requestWriteSettingsPermission(context)
                        }
                    },
                    volume = volume,
                    onVolumeChange = { value ->
                        volume = value
                        DeviceControls.setVolumeFraction(context, value)
                    },
                    // Ngăn tap bên trong panel làm đóng panel (lan tới clickable ở Box ngoài)
                    modifier = Modifier.clickable(enabled = false) {},
                )
            }
        }
    }
}

@Composable
private fun ControlCenterPanel(
    wifiOn: Boolean,
    onToggleWifi: () -> Unit,
    bluetoothOn: Boolean,
    onToggleBluetooth: () -> Unit,
    airplaneOn: Boolean,
    onToggleAirplane: () -> Unit,
    dataOn: Boolean,
    onToggleData: () -> Unit,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(top = 8.dp, end = 16.dp)
            .width(340.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(PanelBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Lưới 2x2: Máy bay, Wi-Fi, Data, Bluetooth
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconToggleCard(
                label = "Máy bay",
                isOn = airplaneOn,
                iconOn = Icons.Filled.AirplanemodeActive,
                iconOff = Icons.Filled.AirplanemodeInactive,
                modifier = Modifier.weight(1f),
                onClick = onToggleAirplane,
            )
            IconToggleCard(
                label = "Wi-Fi",
                isOn = wifiOn,
                iconOn = Icons.Filled.Wifi,
                iconOff = Icons.Filled.WifiOff,
                modifier = Modifier.weight(1f),
                onClick = onToggleWifi,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconToggleCard(
                label = "Data",
                isOn = dataOn,
                iconOn = Icons.Filled.SignalCellular4Bar,
                iconOff = Icons.Filled.SignalCellularOff,
                modifier = Modifier.weight(1f),
                onClick = onToggleData,
            )
            IconToggleCard(
                label = "Bluetooth",
                isOn = bluetoothOn,
                iconOn = Icons.Filled.Bluetooth,
                iconOff = Icons.Filled.BluetoothDisabled,
                modifier = Modifier.weight(1f),
                onClick = onToggleBluetooth,
            )
        }

        // Hai thanh trượt dọc: độ sáng + âm lượng
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            VerticalSliderPill(
                value = brightness,
                onValueChange = onBrightnessChange,
                icon = Icons.Filled.WbSunny,
                modifier = Modifier.weight(1f),
            )
            VerticalSliderPill(
                value = volume,
                onValueChange = onVolumeChange,
                icon = if (volume <= 0f) Icons.Filled.VolumeOff
                       else if (volume < 0.5f) Icons.Filled.VolumeDown
                       else Icons.Filled.VolumeUp,
                modifier = Modifier.weight(1f),
            )
        }

        // Logo thương hiệu Dr Sơn - đặt cuối panel, dưới toàn bộ các nút điều khiển.
        Image(
            painter = painterResource(id = R.drawable.icon_brand),
            contentDescription = null,
            modifier = Modifier
                .padding(top = 4.dp)
                .size(36.dp)
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(10.dp)),
        )
    }
}

/**
 * Ô bật/tắt có icon, dùng cho lưới Máy bay/Wi-Fi/Data/Bluetooth.
 * Bật = nền vàng đồng, icon+chữ đen. Tắt = nền tối mờ, icon+chữ trắng mờ.
 */
@Composable
private fun IconToggleCard(
    label: String,
    isOn: Boolean,
    iconOn: androidx.compose.ui.graphics.vector.ImageVector,
    iconOff: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val contentColor = if (isOn) Color(0xFF201607) else Color.White.copy(alpha = 0.85f)
    Column(
        modifier = modifier
            .height(76.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isOn) GoldAccent else CardOff)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(
            imageVector = if (isOn) iconOn else iconOff,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
        Column {
            Text(label, color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                if (isOn) "Bật" else "Tắt",
                color = contentColor.copy(alpha = 0.75f),
                fontSize = 11.sp,
            )
        }
    }
}

/**
 * Thanh trượt DỌC kiểu "pill" (giống Control Center iOS): nền tối mờ, phần đã trượt tô vàng đồng
 * dâng từ đáy lên, icon nằm ở đáy. Kéo lên/xuống để chỉnh giá trị 0..1.
 */
@Composable
private fun VerticalSliderPill(
    value: Float,
    onValueChange: (Float) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(150.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(CardOff),
    ) {
        // Phần tô màu dâng từ đáy lên theo giá trị hiện tại
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(fraction = value.coerceIn(0.06f, 1f))
                .align(Alignment.BottomCenter)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(GoldAccentLight, GoldAccent),
                    ),
                ),
        )

        // Lớp Slider trong suốt để bắt thao tác kéo, xoay dọc bằng graphicsLayer + layout.
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = Color.Transparent,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { rotationZ = -90f }
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        Constraints(
                            minWidth = constraints.minHeight,
                            maxWidth = constraints.maxHeight,
                            minHeight = constraints.minWidth,
                            maxHeight = constraints.maxWidth,
                        ),
                    )
                    layout(placeable.height, placeable.width) {
                        placeable.place(
                            x = -(placeable.width / 2 - placeable.height / 2),
                            y = -(placeable.height / 2 - placeable.width / 2),
                        )
                    }
                },
        )

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (value > 0.55f) Color(0xFF201607) else Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
                .size(20.dp),
        )
    }
}


