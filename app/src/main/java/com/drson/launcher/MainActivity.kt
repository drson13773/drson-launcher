package com.drson.launcher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import com.drson.launcher.model.AppItem
import com.drson.launcher.music.PureMusicActivity
import com.drson.launcher.ui.CircularLuxurySpeedometer
import com.drson.launcher.ui.ControlCenterSheet
import com.drson.launcher.ui.DrivingRoadBackground
import com.drson.launcher.ui.HomeViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: HomeViewModel

    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_PHONE_STATE
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            viewModel.initGpsSpeedListener()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            try {
                startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) {}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        checkAndRequestPermissions()

        setContent {
            var showAppDrawer by remember { mutableStateOf(false) }
            var showControlCenter by remember { mutableStateOf(false) }

            // BOX TỰ CO DÃN TỶ LỆ THEO MỌI LOẠI MÀN HÌNH XE Ô TÔ
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 45f) {
                                showControlCenter = true
                            } else if (dragAmount < -45f) {
                                showAppDrawer = true
                            }
                        }
                    }
            ) {
                val screenW = maxWidth
                val screenH = maxHeight
                val speed by viewModel.currentSpeed

                // 1. NỀN ĐƯỜNG 3D VÀ XE MAZDA CX-5 TỰ SCALE THEO CHIỀU CAO
                DrivingRoadBackground(
                    speedKmH = speed,
                    modifier = Modifier.fillMaxSize()
                )

                // 2. LOGO VÀ ĐỒNG HỒ THỜI GIAN (GÓC TRÊN TRÁI)
                TopBrandAndClock(
                    onLogoClick = {
                        startActivity(Intent(this@MainActivity, PureMusicActivity::class.java))
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = screenW * 0.03f, top = screenH * 0.04f)
                )

                // 3. ĐỒNG HỒ TỐC ĐỘ GPS (CHIẾM ~28% CHIỀU CAO MÀN HÌNH)
                val speedometerSize = screenH * 0.28f
                CircularLuxurySpeedometer(
                    speedKmH = speed,
                    modifier = Modifier
                        .size(speedometerSize)
                        .align(Alignment.BottomStart)
                        .padding(start = screenW * 0.03f, bottom = screenH * 0.14f)
                )

                // 4. THANH DOCK CÂN ĐỐI TỰ ÔM KHÍT Ở ĐÁY
                LuxuryBottomDock(
                    onMenuClick = { showAppDrawer = true },
                    onPhoneClick = { launchDialer() },
                    onCameraClick = { launchAppByKeywords(listOf("camera", "cam360", "panorama", "dvr", "cam"), "Camera 360") },
                    onMusicClick = { startActivity(Intent(this@MainActivity, PureMusicActivity::class.java)) },
                    onZingClick = { launchAppByKeywords(listOf("com.zing.mp3", "zingmp3", "zing"), "Zing MP3") },
                    onVietmapClick = { launchAppByKeywords(listOf("vietmap", "live.vietmap", "navigation", "navitel", "maps"), "Vietmap") },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = screenH * 0.025f)
                )

                // TRANG DANH SÁCH ỨNG DỤNG LƯỚI 6 CỘT
                if (showAppDrawer) {
                    AppDrawerGridDialog(
                        apps = viewModel.apps,
                        onAppClick = { app ->
                            showAppDrawer = false
                            viewModel.launchApp(this@MainActivity, app)
                        },
                        onDismiss = { showAppDrawer = false }
                    )
                }

                // TRUNG TÂM ĐIỀU KHIỂN
                ControlCenterSheet(
                    isVisible = showControlCenter,
                    onDismiss = { showControlCenter = false }
                )
            }
        }
    }

    private fun launchDialer() {
        try {
            startActivity(Intent(Intent.ACTION_DIAL).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        } catch (_: Exception) {
            Toast.makeText(this, "Không tìm thấy ứng dụng gọi điện", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchAppByKeywords(keywords: List<String>, title: String) {
        val app = viewModel.apps.firstOrNull { item ->
            keywords.any { kw -> item.packageName.lowercase().contains(kw) || item.label.lowercase().contains(kw) }
        }
        if (app != null) {
            viewModel.launchApp(this, app)
        } else {
            Toast.makeText(this, "Không tìm thấy ứng dụng $title", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndRequestPermissions() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        } else {
            viewModel.initGpsSpeedListener()
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }
}

@Composable
fun TopBrandAndClock(
    onLogoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var timeStr by remember { mutableStateOf("") }
    var dateStr by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val tFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dFormat = SimpleDateFormat("EEEE, dd 'thg' M", Locale("vi", "VN"))
        while (true) {
            val now = Calendar.getInstance().time
            timeStr = tFormat.format(now)
            dateStr = dFormat.format(now)
            delay(1000)
        }
    }

    Row(
        modifier = modifier.clickable { onLogoClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.icon_menu_brand),
            contentDescription = "Brand Logo",
            modifier = Modifier.size(68.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = timeStr,
                color = Color(0xFFFFF0B8),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = dateStr,
                color = Color(0xFFD4AF37),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun LuxuryBottomDock(
    onMenuClick: () -> Unit,
    onPhoneClick: () -> Unit,
    onCameraClick: () -> Unit,
    onMusicClick: () -> Unit,
    onZingClick: () -> Unit,
    onVietmapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.wrapContentWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xE612100C)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockIconButton(iconRes = R.drawable.ic_launcher, onClick = onMenuClick)
            Spacer(modifier = Modifier.width(10.dp))

            DockVectorButton(icon = Icons.Default.Call, tint = Color(0xFFD4AF37), onClick = onPhoneClick)
            Spacer(modifier = Modifier.width(10.dp))

            DockIconButton(iconRes = R.drawable.icon_camera_gold, onClick = onCameraClick)
            Spacer(modifier = Modifier.width(10.dp))

            DockIconButton(iconRes = R.drawable.ic_drson_music, onClick = onMusicClick)
            Spacer(modifier = Modifier.width(10.dp))

            DockVectorButton(icon = Icons.Default.PlayArrow, tint = Color(0xFFFFF0B8), onClick = onZingClick)
            Spacer(modifier = Modifier.width(10.dp))

            DockVectorButton(icon = Icons.Default.Map, tint = Color(0xFFD4AF37), onClick = onVietmapClick)
        }
    }
}

@Composable
private fun DockIconButton(iconRes: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun DockVectorButton(icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(0xFF221E18))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
fun AppDrawerGridDialog(
    apps: List<AppItem>,
    onAppClick: (AppItem) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF2080706))
            .clickable { onDismiss() }
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TẤT CẢ ỨNG DỤNG",
                    color = Color(0xFFD4AF37),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = Color(0xFFFFF0B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(apps) { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onAppClick(app) }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF242018)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = app.icon,
                                contentDescription = app.label,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = app.label,
                            color = Color(0xFFFFF0B8),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
