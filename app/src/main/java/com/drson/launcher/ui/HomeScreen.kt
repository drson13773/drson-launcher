package com.drson.launcher.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Typeface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.drson.launcher.R
import com.drson.launcher.model.AppItem
import kotlinx.coroutines.delay
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

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
        // 1. Nền 3D thành phố & xe Mazda CX-5
        DrivingRoadBackground()

        // 2. Góc trên bên trái: Logo Dr Sơn (Tia sáng rẻ quạt xoay nhanh) + Đồng hồ thực (Không viền)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 14.dp)
        ) {
            BrandClockWidget()
        }

        // 3. Góc tam giác bên trái đường: Đồng hồ tốc độ tròn Luxury Gold
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp, top = 65.dp)
                .size(190.dp)
        ) {
            CircularLuxurySpeedometer()
        }

        // 4. Đáy màn hình: Thanh Dock cố định
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

        // App Drawer Menu
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

    // Hiệu ứng tia sáng rẻ quạt xoay tròn với tốc độ nhanh
    val infiniteTransition = rememberInfiniteTransition(label = "sunburst_anim")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
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
        Box(
            modifier = Modifier.size(70.dp),
            contentAlignment = Alignment.Center
        ) {
            // Vẽ các tia sáng rẻ quạt (Sunburst Rays) xoay tròn
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(rotationAngle)
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val rayCount = 16
                val maxRadius = size.minDimension / 1.8f

                for (i in 0 until rayCount) {
                    val angleDeg = i * (360f / rayCount)
                    val angleRad1 = Math.toRadians((angleDeg - 4.5).toDouble())
                    val angleRad2 = Math.toRadians((angleDeg + 4.5).toDouble())

                    val rayPath = Path().apply {
                        moveTo(center.x, center.y)
                        lineTo(
                            (center.x + maxRadius * cos(angleRad1)).toFloat(),
                            (center.y + maxRadius * sin(angleRad1)).toFloat()
                        )
                        lineTo(
                            (center.x + maxRadius * cos(angleRad2)).toFloat(),
                            (center.y + maxRadius * sin(angleRad2)).toFloat()
                        )
                        close()
                    }

                    drawPath(
                        path = rayPath,
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GOLD_ACCENT.copy(alpha = 0.55f),
                                GOLD_BRIGHT.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = maxRadius
                        )
                    )
                }
            }

            // Logo Dr Sơn sắc nét
            Image(
                painter = painterResource(id = R.drawable.icon_menu_brand),
                contentDescription = "Dr Son Brand",
                modifier = Modifier.size(56.dp),
                contentScale = ContentScale.Fit
            )
        }

        // Cụm giờ và ngày (Không viền)
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
fun CircularLuxurySpeedometer() {
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

    // Góc quay kim đo mượt mà từ -135 độ (0 km/h) đến +135 độ (180 km/h)
    val animatedSpeed by animateFloatAsState(
        targetValue = currentSpeed.coerceIn(0f, 180f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "needle_speed"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                if (!hasPermission) launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h / 2f)
            val radius = w / 2f - 6f

            // 1. Mặt đồng hồ đen sâu
            drawCircle(
                color = Color(0xFF0C0B0A),
                radius = radius,
                center = center
            )

            // 2. Viền kim loại mạ vàng 3D đa tầng (Bezel)
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF8C7335),
                        Color(0xFFFFEFA8),
                        Color(0xFF5A481F),
                        Color(0xFFFFDF73),
                        Color(0xFF8C7335)
                    )
                ),
                radius = radius,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f)
            )
            drawCircle(
                color = Color(0xFF1E1A14),
                radius = radius - 6f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )

            // 3. Vạch chia tốc độ và các con số từ 0 đến 180 km/h
            val startAngle = 135f
            val totalSweep = 270f
            val maxSpeed = 180f

            for (speed in 0..180 step 10) {
                val fraction = speed / maxSpeed
                val angleDeg = startAngle + fraction * totalSweep
                val angleRad = Math.toRadians(angleDeg.toDouble())

                val isMajor = (speed % 20 == 0)
                val tickLength = if (isMajor) 14f else 8f
                val strokeW = if (isMajor) 2.5f else 1.2f

                val outerR = radius - 12f
                val innerR = outerR - tickLength

                val startP = Offset(
                    (center.x + outerR * cos(angleRad)).toFloat(),
                    (center.y + outerR * sin(angleRad)).toFloat()
                )
                val endP = Offset(
                    (center.x + innerR * cos(angleRad)).toFloat(),
                    (center.y + innerR * sin(angleRad)).toFloat()
                )

                drawLine(
                    color = if (isMajor) GOLD_BRIGHT else GOLD_ACCENT.copy(alpha = 0.6f),
                    start = startP,
                    end = endP,
                    strokeWidth = strokeW
                )

                // Vẽ số hiển thị
                if (isMajor) {
                    val textR = innerR - 16f
                    val textX = (center.x + textR * cos(angleRad)).toFloat()
                    val textY = (center.y + textR * sin(angleRad)).toFloat() + 5f

                    drawIntoCanvas { canvas ->
                        val paint = Paint().apply {
                            color = android.graphics.Color.parseColor("#FFF0B8")
                            textSize = 19f
                            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                            textAlign = Paint.Align.CENTER
                            isAntiAlias = true
                        }
                        canvas.nativeCanvas.drawText("$speed", textX, textY, paint)
                    }
                }
            }

            // 4. Chữ "Dr Sơn" nghệ thuật ở nửa dưới tâm đồng hồ
            drawIntoCanvas { canvas ->
                val brandPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#E6CA65")
                    textSize = 28f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.nativeCanvas.drawText("Dr Sơn", center.x, center.y + 36f, brandPaint)

                val speedValPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 24f
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.nativeCanvas.drawText(String.format(Locale.US, "%.0f", currentSpeed), center.x, center.y + 58f, speedValPaint)

                val kmhPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#9E9E9E")
                    textSize = 12f
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.nativeCanvas.drawText("km/h", center.x, center.y + 70f, kmhPaint)
            }

            // 5. Kim đo tốc độ mạ vàng kim xoay theo góc tốc độ
            val needleFraction = animatedSpeed / maxSpeed
            val needleAngleDeg = startAngle + needleFraction * totalSweep

            rotate(degrees = needleAngleDeg + 90f, pivot = center) {
                val needlePath = Path().apply {
                    moveTo(center.x - 3.5f, center.y)
                    lineTo(center.x - 1f, center.y - (radius - 22f))
                    lineTo(center.x + 1f, center.y - (radius - 22f))
                    lineTo(center.x + 3.5f, center.y)
                    close()
                }
                drawPath(needlePath, brush = Brush.verticalGradient(listOf(GOLD_BRIGHT, GOLD_ACCENT)))
            }

            // Nắp chụp tâm kim
            drawCircle(color = Color(0xFF1E1912), radius = 11f, center = center)
            drawCircle(color = GOLD_ACCENT, radius = 11f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f))
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
            .background(Color.Black.copy(alpha = 0.85f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Nút Menu mở App Drawer (Logo Dr Sơn có viền vàng ic_launcher.png)
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

        // 4 ô ứng dụng trên Dock (Ô 1: Điện thoại Phone Gold, Ô 2: Music)
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
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (app != null) {
            Image(
                bitmap = app.icon,
                contentDescription = app.label,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
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
