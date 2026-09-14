package com.drson.launcher.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.drson.launcher.R
import com.drson.launcher.driving.DrivingData
import com.drson.launcher.driving.DrivingDataProvider
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val GOLD = Color(0xFFC99E5C)
private val GOLD_BRIGHT = Color(0xFFFFF0B8)
private val GOLD_SOLID = Color(0xFFFFD56B)
private val SKY_TOP = Color(0xFF16130E)
private val SKY_HORIZON = Color(0xFF3A342C)
private val ROAD_SURFACE = Color(0xFF2E2B27)

private data class Window(
    val xFraction: Float,
    val yFraction: Float,
    val lit: Boolean,
    val blinkPeriodMs: Float,
    val blinkPhase: Float,
)
private data class Building(
    val xFraction: Float,
    val widthFraction: Float,
    val heightFraction: Float,
    val shade: Float,
    val windows: List<Window>,
)

@Composable
fun DrivingRoadBackground(modifier: Modifier = Modifier) {
    val buildings = remember {
        val rnd = Random(7)
        buildList {
            var x = -0.02f
            while (x < 1.02f) {
                val w = 0.035f + rnd.nextFloat() * 0.05f
                val hFrac = 0.07f + rnd.nextFloat() * 0.19f
                val windows = buildList {
                    val cols = 2 + rnd.nextInt(3)
                    val rows = 3 + rnd.nextInt(5)
                    for (r in 0 until rows) {
                        for (c in 0 until cols) {
                            add(
                                Window(
                                    xFraction = (c + 0.5f) / cols,
                                    yFraction = (r + 0.5f) / rows,
                                    lit = rnd.nextFloat() < 0.38f,
                                    blinkPeriodMs = 1500f + rnd.nextFloat() * 3500f,
                                    blinkPhase = rnd.nextFloat(),
                                )
                            )
                        }
                    }
                }
                add(Building(x, w, hFrac, 0.16f + rnd.nextFloat() * 0.14f, windows))
                x += w + 0.006f + rnd.nextFloat() * 0.018f
            }
        }
    }

    val infinite = rememberInfiniteTransition(label = "driving")
    val dashProgress by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1700, easing = LinearEasing), RepeatMode.Restart),
        label = "dash",
    )
    val timeMs by infinite.animateFloat(
        initialValue = 0f, targetValue = 600_000f,
        animationSpec = infiniteRepeatable(tween(600_000, easing = LinearEasing), RepeatMode.Restart),
        label = "time",
    )

    val logoGlowRotation by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12_000, easing = LinearEasing), RepeatMode.Restart),
        label = "logoGlowRotation",
    )

    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasLocationPermission = granted }
    val drivingProvider = remember { DrivingDataProvider(context) }
    val drivingData by drivingProvider.data.collectAsState()
    DisposableEffect(hasLocationPermission) {
        if (hasLocationPermission) drivingProvider.start()
        onDispose { drivingProvider.stop() }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // Tăng kích thước logo để hiển thị chi tiết rõ nét
        val logoWidth = (maxWidth * 0.075f).coerceIn(60.dp, 80.dp)
        val glowSize = logoWidth * 3.2f
        val gaugeSize = (maxHeight * 0.48f).coerceIn(170.dp, 250.dp)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val horizonY = h * 0.42f

            drawRect(Brush.verticalGradient(listOf(SKY_TOP, SKY_HORIZON), startY = 0f, endY = horizonY * 1.15f))
            drawRect(Brush.verticalGradient(listOf(SKY_HORIZON, ROAD_SURFACE), startY = horizonY * 0.6f, endY = horizonY))

            val mountainPath = Path().apply {
                moveTo(0f, horizonY)
                var x = 0f
                var toggle = true
                while (x < w) {
                    val step = w * 0.09f
                    val peak = horizonY - (if (toggle) 34f else 18f)
                    lineTo(x + step / 2, peak)
                    lineTo(x + step, horizonY - 10f)
                    x += step
                    toggle = !toggle
                }
                lineTo(w, horizonY)
                close()
            }
            drawPath(mountainPath, color = Color(0xFF221E1A))

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0.16f, 0.15f, 0.14f, 0.55f)),
                    startY = horizonY * 0.35f, endY = horizonY,
                ),
                topLeft = Offset(0f, horizonY * 0.35f),
                size = androidx.compose.ui.geometry.Size(w, horizonY * 0.65f),
            )

            buildings.forEach { b ->
                val bx = b.xFraction * w
                val bw = b.widthFraction * w
                val bh = b.heightFraction * horizonY
                val top = horizonY - bh
                drawRect(
                    color = Color(0xFF0B0A09),
                    topLeft = Offset(bx, top),
                    size = androidx.compose.ui.geometry.Size(bw, bh + 4f),
                )
                drawRect(
                    color = Color(0xFF3A342C).copy(alpha = 0.9f),
                    topLeft = Offset(bx, top),
                    size = androidx.compose.ui.geometry.Size(2.5f, bh + 4f),
                )
                b.windows.forEach { win ->
                    if (win.lit) {
                        val wx = bx + win.xFraction * bw
                        val wy = top + win.yFraction * bh
                        val winW = (bw * 0.16f).coerceAtLeast(2f)
                        val winH = (bh * 0.09f).coerceAtLeast(2f)
                        val t = (timeMs / win.blinkPeriodMs + win.blinkPhase) % 1f
                        val pulse = abs(sin(Math.PI * t)).toFloat()
                        val winAlpha = 0.25f + 0.6f * pulse
                        drawRect(
                            color = GOLD_SOLID.copy(alpha = winAlpha),
                            topLeft = Offset(wx - winW / 2, wy - winH / 2),
                            size = androidx.compose.ui.geometry.Size(winW, winH),
                        )
                    }
                }
            }

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0f, 0f, 0f, 0.30f), Color.Transparent),
                    startY = horizonY - 50f, endY = horizonY + 25f,
                ),
                topLeft = Offset(0f, horizonY - 50f),
                size = androidx.compose.ui.geometry.Size(w, 75f),
            )

            val cx = w / 2f
            val bottomHalfW = w * 0.60f
            val topHalfW = w * 0.028f
            val roadPath = Path().apply {
                moveTo(cx - bottomHalfW, h)
                lineTo(cx + bottomHalfW, h)
                lineTo(cx + topHalfW, horizonY)
                lineTo(cx - topHalfW, horizonY)
                close()
            }
            drawPath(roadPath, color = ROAD_SURFACE)

            val nStripes = 5
            for (i in 0 until nStripes) {
                val frac0 = i.toFloat() / nStripes
                val frac1 = (i + 0.5f) / nStripes
                val bx0 = cx + bottomHalfW * (frac0 * 2f - 1f) * 0.9f
                val bx1 = cx + bottomHalfW * (frac1 * 2f - 1f) * 0.9f
                val stripePath = Path().apply {
                    moveTo(bx0, h)
                    lineTo(bx1, h)
                    lineTo(cx, horizonY)
                    close()
                }
                drawPath(stripePath, color = Color.White.copy(alpha = 0.02f))
            }

            for (side in intArrayOf(-1, 1)) {
                val pBottom = Offset(cx + side * bottomHalfW * 0.94f, h)
                val pTop = Offset(cx + side * topHalfW * 1.3f, horizonY + 6f)
                drawLine(Color(0.35f, 0.29f, 0.19f), pBottom, pTop, strokeWidth = 5f)
            }

            val glowPath = Path().apply {
                moveTo(cx - w * 0.135f, h)
                lineTo(cx + w * 0.135f, h)
                lineTo(cx + 16f, horizonY + 40f)
                lineTo(cx - 16f, horizonY + 40f)
                close()
            }
            drawPath(
                glowPath,
                brush = Brush.verticalGradient(
                    colors = listOf(GOLD_SOLID.copy(alpha = 0.16f), Color.Transparent),
                    startY = h, endY = horizonY,
                ),
            )

            val nDashes = 7
            for (i in 0 until nDashes) {
                val phase = (dashProgress + i.toFloat() / nDashes) % 1f
                val t0 = phase
                val t1 = (phase + 0.14f).coerceAtMost(1f)
                if (t1 <= t0) continue
                val y0 = h - (h - horizonY) * t0.pow(1.6f)
                val y1 = h - (h - horizonY) * t1.pow(1.6f)
                val width0 = 10f * (1f - t0) + 1f
                val width1 = 10f * (1f - t1) + 1f
                val dashPath = Path().apply {
                    moveTo(cx - width0 / 2, y0)
                    lineTo(cx + width0 / 2, y0)
                    lineTo(cx + width1 / 2, y1)
                    lineTo(cx - width1 / 2, y1)
                    close()
                }
                val alpha = (0.35f + 0.5f * (1f - t0)).coerceIn(0f, 1f)
                drawPath(dashPath, color = GOLD.copy(alpha = alpha))
            }
        }

        Image(
            painter = painterResource(id = R.drawable.car_photo),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.24f)
                .padding(bottom = 105.dp),
            contentScale = ContentScale.FillWidth,
        )

        // --- CỤM LOGO (CHỈ 1 CHỮ DR SƠN TRONG LOGO) & ĐỒNG HỒ SỐ ---
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(logoWidth),
                contentAlignment = Alignment.Center
            ) {
                // Tia sáng xoay nền
                Canvas(modifier = Modifier.size(glowSize)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val baseR = size.minDimension / 2f

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GOLD_SOLID.copy(alpha = 0.25f),
                                GOLD_SOLID.copy(alpha = 0.08f),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = baseR * 0.75f,
                        ),
                        radius = baseR * 0.75f,
                        center = center,
                    )

                    val nRays = 32
                    for (i in 0 until nRays) {
                        val angleDeg = logoGlowRotation + i * (360f / nRays)
                        val angleRad = Math.toRadians(angleDeg.toDouble())
                        val rayLen = baseR * (0.85f + 0.60f * abs(sin(Math.toRadians((angleDeg * 2.5).toDouble()))).toFloat())
                        val innerR = baseR * 0.22f
                        val p1 = Offset(
                            center.x + (innerR * cos(angleRad)).toFloat(),
                            center.y + (innerR * sin(angleRad)).toFloat(),
                        )
                        val p2 = Offset(
                            center.x + (rayLen * cos(angleRad)).toFloat(),
                            center.y + (rayLen * sin(angleRad)).toFloat(),
                        )
                        
                        val isMainRay = i % 2 == 0
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    GOLD_BRIGHT.copy(alpha = if (isMainRay) 0.35f else 0.20f),
                                    Color.Transparent
                                ),
                                start = p1,
                                end = p2
                            ),
                            start = p1,
                            end = p2,
                            strokeWidth = if (isMainRay) 2.4f else 1.5f,
                        )
                    }
                }

                // Hình ảnh logo sắc nét
                Image(
                    painter = painterResource(id = R.drawable.brand_glyph),
                    contentDescription = "Dr Sơn",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(
                        color = GOLD_BRIGHT,
                        blendMode = BlendMode.SrcIn
                    )
                )
            }

            Spacer(Modifier.width(16.dp))

            // Đồng hồ số bên cạnh
            BigClockWidget()
        }

        // CỤM ĐỒNG HỒ TỐC ĐỘ
        if (!hasLocationPermission) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 90.dp),
            ) {
                LocationPermissionPrompt(onRequest = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) })
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp, top = 50.dp),
            ) {
                SpeedGauge(
                    speedKmh = drivingData.speedKmh,
                    size = gaugeSize
                )
            }
        }
    }
}

@Composable
private fun LocationPermissionPrompt(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(max = 260.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Cần quyền vị trí", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Để hiển thị tốc độ thật, vui lòng cấp quyền vị trí thiết bị.",
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Cấp quyền",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(GOLD)
                .clickable(onClick = onRequest)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun BigClockWidget() {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1000)
        }
    }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("EEEE, d 'thg' M", Locale("vi", "VN")) }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = timeFmt.format(now),
            color = GOLD_BRIGHT,
            fontSize = 32.sp,
            fontWeight = FontWeight.Light,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            lineHeight = 34.sp,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.8f),
                    offset = Offset(1.5f, 1.5f),
                    blurRadius = 4f
                )
            )
        )
        Text(
            text = dateFmt.format(now).replaceFirstChar { it.uppercase() },
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.8f),
                    offset = Offset(1f, 1f),
                    blurRadius = 3f
                )
            )
        )
    }
}

@Composable
private fun SpeedGauge(speedKmh: Float, size: Dp = 200.dp) {
    val speed = speedKmh.coerceIn(0f, 180f)
    val maxSpeed = 180
    val majorStep = 20
    val startAngle = 135f
    val sweep = 270f
    val scale = size.value / 200f

    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerR = this.size.minDimension / 2f

            drawCircle(color = Color.Black.copy(alpha = 0.5f), radius = outerR, center = center)

            val bezelWidth = 18f * scale
            val bezelBrush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFF3A2E1C), Color(0xFFE6C178), Color(0xFFC99E5C),
                    Color(0xFF2A2015), Color(0xFFC99E5C), Color(0xFFE6C178),
                    Color(0xFF3A2E1C),
                ),
                center = center,
            )
            drawCircle(
                brush = bezelBrush,
                radius = outerR - bezelWidth / 2,
                center = center,
                style = Stroke(width = bezelWidth),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = outerR - 2f,
                center = center,
                style = Stroke(width = 1.5f * scale),
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.5f),
                radius = outerR - bezelWidth - 1f,
                center = center,
                style = Stroke(width = 2f * scale),
            )

            for (i in 4 downTo 1) {
                drawCircle(
                    color = Color(0xFFC99E5C).copy(alpha = 0.06f * i),
                    radius = outerR - bezelWidth - i * 2f * scale,
                    center = center,
                    style = Stroke(width = 3f * scale),
                )
            }
            drawCircle(color = Color(0xFF0A0908), radius = outerR - bezelWidth - 8f * scale, center = center)
            drawCircle(
                color = Color(0xFFC99E5C).copy(alpha = 0.6f),
                radius = outerR - bezelWidth - 8f * scale,
                center = center,
                style = Stroke(width = 2.5f * scale),
            )

            val faceR = outerR - bezelWidth - 8f * scale
            val tickOuterR = faceR - 8f * scale
            val majorTickInnerR = tickOuterR - 12f * scale
            val minorTickInnerR = tickOuterR - 7f * scale
            val ticksTotal = maxSpeed / (majorStep / 4)

            for (i in 0..ticksTotal) {
                val value = i * (majorStep / 4)
                val angleDeg = startAngle + sweep * (value.toFloat() / maxSpeed)
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val isMajor = value % majorStep == 0
                val innerR = if (isMajor) majorTickInnerR else minorTickInnerR
                val p1 = Offset(
                    center.x + (tickOuterR * cos(angleRad)).toFloat(),
                    center.y + (tickOuterR * sin(angleRad)).toFloat(),
                )
                val p2 = Offset(
                    center.x + (innerR * cos(angleRad)).toFloat(),
                    center.y + (innerR * sin(angleRad)).toFloat(),
                )
                drawLine(
                    color = if (isMajor) Color(0xFFE6C178) else Color(0xFFE6C178).copy(alpha = 0.45f),
                    start = p1,
                    end = p2,
                    strokeWidth = (if (isMajor) 3.0f else 1.5f) * scale,
                )
                if (isMajor) {
                    val labelR = majorTickInnerR - 14f * scale
                    val lx = center.x + (labelR * cos(angleRad)).toFloat()
                    val ly = center.y + (labelR * sin(angleRad)).toFloat()
                    drawContext.canvas.nativeCanvas.drawText(
                        value.toString(),
                        lx,
                        ly + 4f * scale,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 26f * scale
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                            isFakeBoldText = true
                        },
                    )
                }
            }

            val needleAngleDeg = startAngle + sweep * (speed / maxSpeed)
            val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())
            val needleLen = majorTickInnerR - 4f * scale
            val tipX = center.x + (needleLen * cos(needleAngleRad)).toFloat()
            val tipY = center.y + (needleLen * sin(needleAngleRad)).toFloat()
            val perpAngle = needleAngleRad + Math.PI / 2
            val baseWidth = 4.0f * scale
            val b1 = Offset(
                center.x + (baseWidth * cos(perpAngle)).toFloat(),
                center.y + (baseWidth * sin(perpAngle)).toFloat(),
            )
            val b2 = Offset(
                center.x - (baseWidth * cos(perpAngle)).toFloat(),
                center.y - (baseWidth * sin(perpAngle)).toFloat(),
            )
            val needlePath = Path().apply {
                moveTo(b1.x, b1.y)
                lineTo(tipX, tipY)
                lineTo(b2.x, b2.y)
                close()
            }
            drawPath(needlePath, color = Color(0xFFE6C178))

            drawCircle(color = Color(0xFF0A0908), radius = 12f * scale, center = center)
            drawCircle(color = Color(0xFFC99E5C), radius = 12f * scale, center = center, style = Stroke(width = 2f * scale))
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = (42f * scale).dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Dr Sơn",
                color = Color.Black.copy(alpha = 0.85f),
                fontSize = (18f * scale).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily(Font(R.font.alex_brush)),
                modifier = Modifier.offset(x = 1.dp, y = 1.dp)
            )
            Text(
                "Dr Sơn",
                color = GOLD_BRIGHT,
                fontSize = (18f * scale).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily(Font(R.font.alex_brush)),
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = (20f * scale).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("${speed.toInt()}", color = Color.White, fontSize = (16f * scale).sp, fontWeight = FontWeight.Medium)
            Text("km/h", color = Color.White.copy(alpha = 0.6f), fontSize = (8.5f * scale).sp)
        }
    }
}
