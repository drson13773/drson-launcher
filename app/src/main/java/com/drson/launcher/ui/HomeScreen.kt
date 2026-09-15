package com.drson.launcher.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.drson.launcher.R
import com.drson.launcher.model.AppItem
import com.drson.launcher.model.HomeSlotContent
import kotlinx.coroutines.delay
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.*

private val GOLD_BRIGHT = Color(0xFFFFF0B8)
private val GOLD_ACCENT = Color(0xFFD4AF37)
private val DARK_CARD_BG = Color(0xFF14120E)

@SuppressLint("WrongConstant")
fun openNotificationPanel(context: Context) {
    try {
        val statusBarService = context.getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val expandMethod: Method = statusBarManager.getMethod("expandNotificationsPanel")
        expandMethod.invoke(statusBarService)
    } catch (_: Exception) {}
}

@SuppressLint("WrongConstant")
fun openQuickSettingsPanel(context: Context) {
    try {
        val statusBarService = context.getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val expandMethod: Method = try {
            statusBarManager.getMethod("expandSettingsPanel")
        } catch (_: Exception) {
            statusBarManager.getMethod("expandNotificationsPanel")
        }
        expandMethod.invoke(statusBarService)
    } catch (_: Exception) {}
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isEditMode by remember { mutableStateOf(false) }
    var isAppDrawerOpen by remember { mutableStateOf(false) }
    val initialFocusRequester = remember { FocusRequester() }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }

    LaunchedEffect(Unit) {
        try {
            initialFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        if (dragAmount > 35f) {
                            val touchX = change.position.x
                            if (touchX < screenWidthPx / 2) {
                                openNotificationPanel(context)
                            } else {
                                openQuickSettingsPanel(context)
                            }
                        }
                    }
                )
            }
    ) {
        // 1. NỀN PHỐI CẢNH 3D & XE MAZDA CX-5
        DrivingRoadBackground()

        // 2. GÓC TRÊN TRÁI: Logo Dr Sơn (Tia sáng xoay) + Đồng hồ (Không viền khung)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 14.dp)
        ) {
            BrandClockWidget()
        }

        // 3. GÓC TRÊN PHẢI: Lưới ứng dụng màn hình chính
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 14.dp)
        ) {
            val chunkedSlots = remember(viewModel.homeSlots.toList()) {
                viewModel.homeSlots.chunked(3)
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                var isFirstSlot = true
                for (rowSlots in chunkedSlots) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (slot in rowSlots) {
                            val pkgName = when (slot) {
                                is HomeSlotContent.App -> slot.packageName
                                else -> null
                            }
                            val app = viewModel.appFor(pkgName)
                            val currentModifier = if (isFirstSlot && app != null) {
                                isFirstSlot = false
                                Modifier.focusRequester(initialFocusRequester)
                            } else Modifier

                            FocusableDesktopSlot(
                                app = app,
                                modifier = currentModifier,
                                onClick = {
                                    if (app != null) viewModel.launchApp(context, app)
                                }
                            )
                        }
                    }
                }
            }
        }

        // 4. GÓC DƯỚI TRÁI: Đồng hồ đo tốc độ GPS (Luôn ghim ngay trên Dock)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 78.dp)
        ) {
            SpeedometerWidget()
        }

        // 5. ĐÁY MÀN HÌNH: Thanh Dock điều khiển cố định
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        ) {
            BottomDock(
                viewModel = viewModel,
                isEditMode = isEditMode,
                onOpenMenu = { isAppDrawerOpen = true },
                onEmptyDockSlotTap = { _ -> },
                onLongPressSlot = { isEditMode = !isEditMode }
            )
        }

        // Menu App Drawer
        AppDrawerOverlay(
            isOpen = isAppDrawerOpen,
            apps = viewModel.apps,
            onDismiss = { isAppDrawerOpen = false },
            onPick = { app ->
                viewModel.launchApp(context, app)
                isAppDrawerOpen = false
            }
        )
    }
}

@Composable
fun BrandClockWidget() {
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    // Hiệu ứng tia sáng / vầng hào quang xoay tròn quanh Logo Dr Sơn
    val infiniteTransition = rememberInfiniteTransition(label = "logo_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_angle"
    )

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, dd 'thg' M", Locale("vi", "VN"))
        while (true) {
            val now = Calendar.getInstance().time
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now)
            delay(1000)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(4.dp)
    ) {
        // Cụm Logo sắc nét cùng vòng tia sáng xoay
        Box(
            modifier = Modifier.size(62.dp),
            contentAlignment = Alignment.Center
        ) {
            // Vòng hào quang vàng xoay tròn phía sau
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(rotationAngle)
            ) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            GOLD_ACCENT.copy(alpha = 0.6f),
                            Color.Transparent,
                            GOLD_BRIGHT.copy(alpha = 0.9f),
                            Color.Transparent
                        )
                    ),
                    radius = size.minDimension / 2f
                )
            }

            // Logo Dr Sơn sắc nét
            Image(
                painter = painterResource(id = R.drawable.icon_menu_brand),
                contentDescription = "Dr Son Brand",
                modifier = Modifier.size(54.dp),
                contentScale = ContentScale.Fit
            )
        }

        // Cụm giờ và ngày tháng (Không khung viền)
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = currentTime,
                color = GOLD_BRIGHT,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                text = currentDate,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SpeedometerWidget() {
    val context = LocalContext.current
    var currentSpeed by remember { mutableFloatStateOf(0f) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    DisposableEffect(hasPermission) {
        if (!hasPermission) return@DisposableEffect onDispose {}

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (location.hasSpeed()) {
                    currentSpeed = location.speed * 3.6f
                }
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                500L,
                0.5f,
                locationListener
            )
        } catch (_: SecurityException) {}

        onDispose {
            try {
                locationManager?.removeUpdates(locationListener)
            } catch (_: SecurityException) {}
        }
    }

    Box(
        modifier = Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .border(1.dp, GOLD_ACCENT.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (hasPermission) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "TỐC ĐỘ XE",
                        color = GOLD_ACCENT,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format(Locale.US, "%.0f", currentSpeed),
                            color = GOLD_BRIGHT,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "km/h",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Speedometer",
                    tint = GOLD_ACCENT,
                    modifier = Modifier.size(30.dp)
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "TỐC ĐỘ GPS",
                    color = GOLD_BRIGHT,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                    colors = ButtonDefaults.buttonColors(containerColor = GOLD_ACCENT),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("Bật GPS", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FocusableDesktopSlot(
    app: AppItem?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .size(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (app != null) Color.Black.copy(alpha = 0.5f) else Color.Transparent)
            .focusable(enabled = (app != null), interactionSource = interactionSource)
            .then(
                if (isFocused && app != null) {
                    Modifier.border(2.dp, GOLD_BRIGHT, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            )
            .clickable(enabled = (app != null), onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (app != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    bitmap = app.icon,
                    contentDescription = app.label,
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = app.label,
                    color = Color.White,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun BottomDock(
    viewModel: HomeViewModel,
    isEditMode: Boolean,
    onOpenMenu: () -> Unit,
    onEmptyDockSlotTap: (Int) -> Unit,
    onLongPressSlot: () -> Unit,
) {
    val context = LocalContext.current
    val menuInteractionSource = remember { MutableInteractionSource() }
    val isMenuFocused by menuInteractionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.8f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Nút Menu chính với icon_launcher.png
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1912))
                .focusable(enabled = true, interactionSource = menuInteractionSource)
                .then(
                    if (isMenuFocused) Modifier.border(2.dp, GOLD_BRIGHT, RoundedCornerShape(12.dp))
                    else Modifier.border(1.dp, GOLD_BRIGHT.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                )
                .clickable(onClick = onOpenMenu)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher),
                contentDescription = "Menu ứng dụng",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        // 4 ô ứng dụng trên Dock
        viewModel.dockSlots.take(4).forEachIndexed { index, packageName ->
            val app = viewModel.appFor(packageName)
            FocusableDockSlotCell(
                app = app,
                isEditMode = isEditMode,
                onTap = {
                    if (app != null) viewModel.launchApp(context, app) else onEmptyDockSlotTap(index)
                },
                onLongPress = {
                    onLongPressSlot()
                    if (app != null) viewModel.setDockSlot(context, index, null)
                },
            )
        }

        Spacer(Modifier.width(6.dp))

        // Trình phát nhạc & Âm lượng kéo dài
        ExpandedNowPlayingBar(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
    }
}

@Composable
private fun FocusableDockSlotCell(
    app: AppItem?,
    isEditMode: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (app != null) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f))
            .focusable(enabled = (app != null), interactionSource = interactionSource)
            .then(
                if (isFocused && app != null) Modifier.border(2.dp, GOLD_BRIGHT, RoundedCornerShape(12.dp))
                else Modifier.border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            )
            .clickable(onClick = onTap)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (app != null) {
            Image(
                bitmap = app.icon,
                contentDescription = app.label,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
            )
        }
    }
}

@Composable
private fun ExpandedNowPlayingBar(modifier: Modifier = Modifier) {
    var isPlaying by remember { mutableStateOf(false) }
    var trackProgress by remember { mutableFloatStateOf(0.35f) }
    var volumeLevel by remember { mutableFloatStateOf(0.7f) }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DARK_CARD_BG)
            .border(1.dp, GOLD_ACCENT.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = {}, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = GOLD_ACCENT, modifier = Modifier.size(18.dp))
            }
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(GOLD_ACCENT)
                    .clickable { isPlaying = !isPlaying },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = {}, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = GOLD_ACCENT, modifier = Modifier.size(18.dp))
            }
        }

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("01:25", color = Color.Gray, fontSize = 9.sp)
            Slider(
                value = trackProgress,
                onValueChange = { trackProgress = it },
                colors = SliderDefaults.colors(
                    thumbColor = GOLD_BRIGHT,
                    activeTrackColor = GOLD_ACCENT,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier.weight(1f).height(16.dp)
            )
            Text("04:10", color = Color.Gray, fontSize = 9.sp)
        }

        Row(
            modifier = Modifier.width(110.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(Icons.Default.VolumeUp, contentDescription = "Volume", tint = GOLD_ACCENT, modifier = Modifier.size(16.dp))
            Slider(
                value = volumeLevel,
                onValueChange = { volumeLevel = it },
                colors = SliderDefaults.colors(
                    thumbColor = GOLD_BRIGHT,
                    activeTrackColor = GOLD_ACCENT,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier.weight(1f).height(16.dp)
            )
        }
    }
}
