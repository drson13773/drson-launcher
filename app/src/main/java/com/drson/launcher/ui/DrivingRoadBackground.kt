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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
private val GOLD_BRIGHT = Color(0xFFE6C178)
private val SKY_TOP = Color(0xFF16130E)
private val SKY_HORIZON = Color(0xFF3A342C) // sáng hơn gần đường chân trời, giống ảnh mẫu
private val ROAD_SURFACE = Color(0xFF2E2B27) // sáng hơn hẳn để phân biệt rõ với bầu trời/nhà

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

/**
 * Nền "đang lái xe": chỉ vạch kẻ giữa đường di chuyển (mô phỏng xe chạy tới trước) - dãy nhà xa
 * đứng YÊN vì xe đi thẳng, nhà ở khoảng cách xa gần như không có thị sai ngang thực tế (nhà trôi
 * ngang sẽ trông giống xe đang rẽ, không hợp lý khi đường phía trước thẳng).
 * Ảnh xe là ảnh THẬT do người dùng cung cấp (car_photo.png), giữ nguyên không chỉnh sửa.
 *
 * Đây là HÌNH NỀN DUY NHẤT của launcher (luôn hiển thị, không có lựa chọn nào khác), nên đồng hồ
 * giờ + đồng hồ tốc độ thật (GPS) được vẽ LUÔN vào đây - hiện thường trực phía sau Home Screen /
 * Dock, không cần vuốt sang trang riêng mới thấy. Vùng bên trái màn hình (nơi đặt 2 đồng hồ này)
 * được `HomeScreen.kt` chừa trống, không gán app/widget vào đó để tránh icon đè lên.
 */
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
    // Đồng hồ thời gian toàn cục (mili-giây) để tính nhịp nhấp nháy riêng cho từng cửa sổ.
    val timeMs by infinite.animateFloat(
        initialValue = 0f, targetValue = 600_000f,
        animationSpec = infiniteRepeatable(tween(600_000, easing = LinearEasing), RepeatMode.Restart),
        label = "time",
    )

    // ---------- Dữ liệu lái xe THẬT (tốc độ + GPS qua LocationManager) ----------
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
        // Kích thước logo giữ nguyên tỉ lệ cũ (9% bề rộng màn hình); quầng sáng toả rộng hơn
        // logo khoảng 2.6 lần để tạo hiệu ứng hào quang giống ảnh nền lớn ở Home Screen.
        val logoWidth = maxWidth * 0.09f
        val glowSize = logoWidth * 2.6f
        val glowInset = (glowSize - logoWidth) / 2

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val horizonY = h * 0.42f

            drawRect(Brush.verticalGradient(listOf(SKY_TOP, SKY_HORIZON), startY = 0f, endY = horizonY * 1.15f))
            drawRect(Brush.verticalGradient(listOf(SKY_HORIZON, ROAD_SURFACE), startY = horizonY * 0.6f, endY = horizonY))

            // --- Dãy núi mờ xa phía sau toà nhà - tạo chiều sâu như ảnh tham khảo ---
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

            // Dải trời sáng hơn ngay trên đường chân trời (giống sương sớm/hoàng hôn của CarWebGuru)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0.16f, 0.15f, 0.14f, 0.55f)),
                    startY = horizonY * 0.35f, endY = horizonY,
                ),
                topLeft = Offset(0f, horizonY * 0.35f),
                size = androidx.compose.ui.geometry.Size(w, horizonY * 0.65f),
            )

            // --- Skyline: ĐỨNG YÊN, đen đậm để nổi khối rõ nét trên nền trời sáng hơn ---
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
                // viền sáng nhẹ bên trái toà nhà (cạnh hắt sáng) để tách khối rõ hơn
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
                        // Nhấp nháy: mỗi cửa sổ có chu kỳ + lệch pha riêng nên không sáng/tắt đồng loạt.
                        val t = (timeMs / win.blinkPeriodMs + win.blinkPhase) % 1f
                        val pulse = kotlin.math.abs(kotlin.math.sin(Math.PI * t)).toFloat()
                        val winAlpha = 0.25f + 0.6f * pulse
                        drawRect(
                            color = GOLD_BRIGHT.copy(alpha = winAlpha),
                            topLeft = Offset(wx - winW / 2, wy - winH / 2),
                            size = androidx.compose.ui.geometry.Size(winW, winH),
                        )
                    }
                }
            }

            // sương mờ trước chân trời cho có chiều sâu
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0f, 0f, 0f, 0.30f), Color.Transparent),
                    startY = horizonY - 50f, endY = horizonY + 25f,
                ),
                topLeft = Offset(0f, horizonY - 50f),
                size = androidx.compose.ui.geometry.Size(w, 75f),
            )

            // --- Mặt đường ---
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

            // Texture ánh sáng hắt trên mặt đường (các dải sáng-tối xen kẽ toả từ điểm hội tụ,
            // giống hiệu ứng đèn pha lên nhựa đường trong ảnh tham khảo CarWebGuru)
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

            // đèn pha hắt sáng lên mặt đường
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
                    colors = listOf(GOLD_BRIGHT.copy(alpha = 0.16f), Color.Transparent),
                    startY = h, endY = horizonY,
                ),
            )

            // --- Vạch kẻ giữa đường - ĐỘNG, lướt chậm rãi về phía người xem ---
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

        // Ảnh xe THẬT của bạn - đặt cố định phía dưới, giữ nguyên không chỉnh sửa.
        // Giảm nhẹ kích thước (0.315 -> 0.27) để có khoảng trống thật giữa chân trời và nóc xe,
        // đủ chỗ cho đồng hồ/tốc độ của cụm đồng hồ không bị đè lên xe.
        Image(
            painter = painterResource(id = R.drawable.car_photo),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.27f)
                .padding(bottom = 150.dp),
            contentScale = ContentScale.FillWidth,
        )

        // Xoay chậm liên tục các tia sáng quanh logo - dùng chung "infinite transition" đã có
        // ở trên (vạch kẻ đường / cửa sổ nhấp nháy) để khỏi tạo thêm Composition riêng.
        val logoGlowRotation by infinite.animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(70_000, easing = LinearEasing), RepeatMode.Restart),
            label = "logoGlowRotation",
        )

        // Logo thương hiệu "Dr Sơn" - góc trên-trái, bản glyph trong suốt (không viền đen),
        // đặt trong quầng sáng + tia sáng vàng đồng toả ra, giống hiệu ứng hào quang quanh
        // logo lớn ở ảnh nền Home Screen.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 14.dp - glowInset, y = 14.dp - glowInset)
                .size(glowSize),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxR = size.minDimension / 2f

                // Quầng sáng mờ lan toả từ tâm logo ra ngoài.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GOLD_BRIGHT.copy(alpha = 0.32f),
                            GOLD_BRIGHT.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = maxR,
                    ),
                    radius = maxR,
                    center = center,
                )

                // Các tia sáng mảnh toả đều quanh logo, dài ngắn xen kẽ, xoay chậm liên tục.
                val nRays = 24
                for (i in 0 until nRays) {
                    val angleDeg = logoGlowRotation + i * (360f / nRays)
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val rayLen = maxR * (0.55f + 0.35f * abs(sin(Math.toRadians((angleDeg * 2).toDouble()))).toFloat())
                    val innerR = maxR * 0.26f
                    val p1 = Offset(
                        center.x + (innerR * cos(angleRad)).toFloat(),
                        center.y + (innerR * sin(angleRad)).toFloat(),
                    )
                    val p2 = Offset(
                        center.x + (rayLen * cos(angleRad)).toFloat(),
                        center.y + (rayLen * sin(angleRad)).toFloat(),
                    )
                    drawLine(
                        color = GOLD_BRIGHT.copy(alpha = 0.18f),
                        start = p1,
                        end = p2,
                        strokeWidth = 2.2f,
                    )
                }
            }
            Image(
                painter = painterResource(id = R.drawable.brand_glyph),
                contentDescription = "Dr Sơn",
                modifier = Modifier.size(logoWidth),
                contentScale = ContentScale.FillWidth,
            )
        }

        // ---------- Cụm đồng hồ giờ + đồng hồ tốc độ THẬT - LUÔN hiển thị ----------
        // Đặt bên trái màn hình, dưới hàng status bar thật (chừa top đủ lớn để không bị đè) và
        // trên khu vực Dock (chừa bottom đủ lớn) - vị trí tính theo % bề rộng/cao màn hình
        // (maxWidth/maxHeight của chính BoxWithConstraints này) nên tự co giãn theo màn hình xe.
        if (!hasLocationPermission) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 130.dp),
            ) {
                LocationPermissionPrompt(onRequest = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) })
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = maxWidth * 0.19f, top = 84.dp),
            ) {
                BigClockWidget()
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 20.dp),
            ) {
                SpeedGauge(speedKmh = drivingData.speedKmh, size = (maxWidth * 0.34f).coerceAtMost(480.dp))
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 36.dp, bottom = 130.dp),
            ) {
                GpsInfoCard(data = drivingData)
            }
        }
    }
}

@Composable
private fun LocationPermissionPrompt(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Cần quyền vị trí", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Text(
            "Để hiện tốc độ thật, launcher cần quyền truy cập vị trí của thiết bị.",
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Cấp quyền",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(GOLD)
                .clickable(onClick = onRequest)
                .padding(horizontal = 14.dp, vertical = 7.dp),
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
    val dateFmt = remember { SimpleDateFormat("EEEE, d MMMM", Locale("vi", "VN")) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            timeFmt.format(now),
            color = Color(0xFFE6C178),
            fontSize = 88.sp,
            fontWeight = FontWeight.Light,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            dateFmt.format(now).replaceFirstChar { it.uppercase() },
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 15.sp,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SpeedGauge(speedKmh: Float, size: Dp = 190.dp) {
    val speed = speedKmh.coerceIn(0f, 180f)
    val maxSpeed = 180
    val majorStep = 20
    val startAngle = 135f
    val sweep = 270f
    val scale = size.value / 190f // co dan cac chi tiet (tick, chu, kim) theo kich thuoc thuc te

    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerR = this.size.minDimension / 2f

            // Bóng đổ ngoài cùng - tạo cảm giác vòng kim loại nổi khối
            drawCircle(color = Color.Black.copy(alpha = 0.5f), radius = outerR, center = center)

            // Vòng bezel kim loại dạng khối - dùng sweepGradient mô phỏng ánh sáng phản chiếu
            // quanh vòng kim loại (thay vì 1 màu phẳng), cho cảm giác "hầm hố" hơn.
            val bezelWidth = 22f * scale
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
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = bezelWidth),
            )
            // Viền chỉ mảnh sáng + tối áp sát 2 bên bezel để tách khối rõ hơn (hiệu ứng vát cạnh)
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = outerR - 2f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f * scale),
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.5f),
                radius = outerR - bezelWidth - 1f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f * scale),
            )

            // Viền phát sáng nhẹ ngay trong bezel (giữ lại hiệu ứng glow cũ, thu nhỏ lại)
            for (i in 4 downTo 1) {
                drawCircle(
                    color = Color(0xFFC99E5C).copy(alpha = 0.06f * i),
                    radius = outerR - bezelWidth - i * 2f * scale,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f * scale),
                )
            }
            drawCircle(color = Color(0xFF0A0908), radius = outerR - bezelWidth - 10f * scale, center = center)
            drawCircle(
                color = Color(0xFFC99E5C).copy(alpha = 0.6f),
                radius = outerR - bezelWidth - 10f * scale,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f * scale),
            )

            val faceR = outerR - bezelWidth - 10f * scale
            val tickOuterR = faceR - 12f * scale
            val majorTickInnerR = tickOuterR - 16f * scale
            val minorTickInnerR = tickOuterR - 9f * scale
            val ticksTotal = maxSpeed / (majorStep / 4) // vạch phụ, 4 vạch phụ mỗi khoảng chính

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
                    strokeWidth = (if (isMajor) 3.5f else 1.8f) * scale,
                )
                if (isMajor) {
                    val labelR = majorTickInnerR - 20f * scale
                    val lx = center.x + (labelR * cos(angleRad)).toFloat()
                    val ly = center.y + (labelR * sin(angleRad)).toFloat()
                    drawContext.canvas.nativeCanvas.drawText(
                        value.toString(),
                        lx,
                        ly + 5f * scale, // căn giữa dọc gần đúng theo baseline chữ
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 38f * scale
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                            isFakeBoldText = true
                        },
                    )
                }
            }

            // Kim chỉ tốc độ - tam giác thon dài, giống kim đồng hồ tốc độ analog thật.
            val needleAngleDeg = startAngle + sweep * (speed / maxSpeed)
            val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())
            val needleLen = majorTickInnerR - 6f * scale
            val tipX = center.x + (needleLen * cos(needleAngleRad)).toFloat()
            val tipY = center.y + (needleLen * sin(needleAngleRad)).toFloat()
            val perpAngle = needleAngleRad + Math.PI / 2
            val baseWidth = 5f * scale
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

            // Trục kim (hub)
            drawCircle(color = Color(0xFF0A0908), radius = 16f * scale, center = center)
            drawCircle(color = Color(0xFFC99E5C), radius = 16f * scale, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f * scale))
        }

        // Chữ ký "Dr Sơn" ngay dưới trục kim - dùng đúng font chữ ký trong logo thương hiệu.
        Text(
            "Dr Sơn",
            color = Color(0xFFE6C178),
            fontSize = (22f * scale).sp,
            fontFamily = FontFamily(Font(R.font.alex_brush)),
            modifier = Modifier.align(Alignment.Center).padding(top = (30f * scale).dp),
        )

        // Số km/h nhỏ, đặt gần đáy mặt đồng hồ - giống vị trí số odometer trong ảnh tham khảo.
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = (34f * scale).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("${speed.toInt()}", color = Color.White, fontSize = (20f * scale).sp, fontWeight = FontWeight.Medium)
            Text("km/h", color = Color.White.copy(alpha = 0.6f), fontSize = (10f * scale).sp)
        }
    }
}

@Composable
private fun GpsInfoCard(data: DrivingData) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (data.hasGpsFix && data.latitude != null && data.longitude != null) {
            Text(
                String.format(Locale.US, "%.4f, %.4f", data.latitude, data.longitude),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
            )
            data.altitudeMeters?.let {
                Text("${it.toInt()} m", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
            }
        } else {
            Text("Đang định vị GPS...", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
        }
    }
}
