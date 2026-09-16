package com.drson.launcher

import android.Manifest
import android.content.Context
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import kotlin.math.abs

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
            var isEditMode by remember { mutableStateOf(false) }
            var selectedSlotForAdd by remember { mutableStateOf<Int?>(null) }
            var selectedAppForOption by remember { mutableStateOf<Pair<Int, AppItem>?>(null) }

            // Lưu trữ vị trí 8 ô tiện ích vào SharedPreferences
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("launcher_slots", Context.MODE_PRIVATE) }
            val pinnedPackages = remember {
                mutableStateListOf<String?>().apply {
                    for (i in 0 until 8) {
                        add(prefs.getString("slot_$i", null))
                    }
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    // BẮT CỬ CHỈ VUỐT
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {},
                            onDrag = { change, dragAmount ->
                                val position = change.position
                                val screenWidth = size.width
                                val (dx, dy) = dragAmount

                                // 1. Vuốt ngang giữa màn hình -> Mở trang danh sách app 6 cột
                                if (abs(dx) > abs(dy) && abs(dx) > 28f) {
                                    showAppDrawer = true
                                    change.consume()
                                }
                                // 2. Vuốt từ trên xuống
                                else if (dy > 30f && position.y < size.height * 0.4f) {
                                    if (position.x > screenWidth / 2f) {
                                        // Vuốt trên-phải xuống -> Mở Trung tâm điều khiển
                                        showControlCenter = true
                                    } else {
                                        // Vuốt trên-trái xuống -> Mở Thông báo hệ thống
                                        openSystemNotificationShade()
                                    }
                                    change.consume()
                                }
                            }
                        )
                    }
            ) {
                val screenW = maxWidth
                val screenH = maxHeight
                val speed by viewModel.currentSpeed

                // 1. NỀN GỐC, TIM ĐƯỜNG TRÔI, DÃY NHÀ NHÁY ĐÈN, CÂY TRÔI THEO GPS, XE MAZDA
                DrivingRoadBackground(
                    speedKmH = speed,
                    modifier = Modifier.fillMaxSize()
                )

                // 2. PHÍA TRÊN TRÁI: LOGO VÀ ĐỒNG HỒ THỜI GIAN
                TopBrandAndClock(
                    onLogoClick = {
                        startActivity(
                            Intent(this@MainActivity, PureMusicActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = screenW * 0.025f, top = screenH * 0.025f)
                )

                // 3. KHOẢNG TRỐNG BÊN TRÁI CON ĐƯỜNG VỚI DÃY NHÀ: ĐỒNG HỒ TỐC ĐỘ GPS
                val speedometerSize = screenH * 0.28f
                CircularLuxurySpeedometer(
                    speedKmH = speed,
                    modifier = Modifier
                        .size(speedometerSize)
                        .align(Alignment.CenterStart)
                        .padding(start = screenW * 0.035f)
                )

                // 4. HỆ THỐNG Ô TIỆN ÍCH QUANH XE (ẨN MẶC ĐỊNH, GIỮ LÂU NỔI Ô CHỜ GHIM APP)
                HomeScreenWidgetGrid(
                    pinnedPackages = pinnedPackages,
                    viewModel = viewModel,
                    isEditMode = isEditMode,
                    onEmptySlotClick = { slotIdx ->
                        selectedSlotForAdd = slotIdx
                    },
                    onAppClick = { app ->
                        viewModel.launchApp(this@MainActivity, app)
                    },
                    onAppLongClick = { slotIdx, app ->
                        selectedAppForOption = Pair(slotIdx, app)
                    },
                    onBackgroundLongClick = {
                        isEditMode = !isEditMode
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // 5. THANH DOCK TỰ CO GIÃN VỚI NÚT MENU BÊN TRÁI CÙNG
                LuxuryBottomDock(
                    onAppDrawerClick = { showAppDrawer = true },
                    onPhoneClick = { launchDialer() },
                    onMusicClick = {
                        startActivity(
                            Intent(this@MainActivity, PureMusicActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                )

                // DIALOG CHỌN APP KHI BẤM DẤU CỘNG
                selectedSlotForAdd?.let { slotIndex ->
                    AppPickerDialog(
                        apps = viewModel.apps,
                        onAppSelected = { app ->
                            pinnedPackages[slotIndex] = app.packageName
                            prefs.edit().putString("slot_$slotIndex", app.packageName).apply()
                            selectedSlotForAdd = null
                        },
                        onDismiss = { selectedSlotForAdd = null }
                    )
                }

                // DIALOG ĐỔI HOẶC XÓA APP ĐÃ GHIM
                selectedAppForOption?.let { (slotIndex, app) ->
                    AlertDialog(
                        onDismissRequest = { selectedAppForOption = null },
                        title = { Text(text = app.label, color = Color(0xFFD4AF37)) },
                        text = { Text("Bạn muốn đổi sang ứng dụng khác hay gỡ khỏi màn hình?", color = Color.White) },
                        confirmButton = {
                            TextButton(onClick = {
                                val target = slotIndex
                                selectedAppForOption = null
                                selectedSlotForAdd = target
                            }) {
                                Text("Đổi ứng dụng", color = Color(0xFFD4AF37))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                pinnedPackages[slotIndex] = null
                                prefs.edit().remove("slot_$slotIndex").apply()
                                selectedAppForOption = null
                            }) {
                                Text("Gỡ bỏ", color = Color(0xFFFF6B6B))
                            }
                        },
                        containerColor = Color(0xFF1E1A16)
                    )
                }

                // TRANG DANH SÁCH ỨNG DỤNG 6 CỘT TÍNH TOÁN KÍCH THƯỚC ĐỘNG
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

    private fun openSystemNotificationShade() {
        try {
            @Suppress("WrongConstant")
            val sbservice = getSystemService("statusbar")
            val statusbarManager = Class.forName("android.app.StatusBarManager")
            val showsb = statusbarManager.getMethod("expandNotificationsPanel")
            showsb.invoke(sbservice)
        } catch (_: Exception) {
            Toast.makeText(this, "Không thể mở thanh thông báo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchDialer() {
        try {
            startActivity(Intent(Intent.ACTION_DIAL).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        } catch (_: Exception) {
            Toast.makeText(this, "Không tìm thấy ứng dụng gọi điện", Toast.LENGTH_SHORT).show()
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
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = timeStr, color = Color(0xFFFFF0B8), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(text = dateStr, color = Color(0xFFD4AF37), fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreenWidgetGrid(
    pinnedPackages: List<String?>,
    viewModel: HomeViewModel,
    isEditMode: Boolean,
    onEmptySlotClick: (Int) -> Unit,
    onAppClick: (AppItem) -> Unit,
    onAppLongClick: (Int, AppItem) -> Unit,
    onBackgroundLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = {},
                onLongClick = onBackgroundLongClick
            )
    ) {
        // Hàng 1: Phía trên xe (3 vị trí 0, 1, 2)
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-110).dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            WidgetSlotItem(0, pinnedPackages.getOrNull(0), viewModel, isEditMode, onEmptySlotClick, onAppClick, onAppLongClick)
            WidgetSlotItem(1, pinnedPackages.getOrNull(1), viewModel, isEditMode, onEmptySlotClick, onAppClick, onAppLongClick)
            WidgetSlotItem(2, pinnedPackages.getOrNull(2), viewModel, isEditMode, onEmptySlotClick, onAppClick, onAppLongClick)
        }

        // Hàng 2: Hai bên sườn xe (Vị trí 3, 4 bên trái và 5, 6 bên phải, chừa khoảng giữa không đè lên xe Mazda)
        Row(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .align(Alignment.Center)
                .offset(y = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                WidgetSlotItem(3, pinnedPackages.getOrNull(3), viewModel, isEditMode, onEmptySlotClick, onAppClick, onAppLongClick)
                WidgetSlotItem(4, pinnedPackages.getOrNull(4), viewModel, isEditMode, onEmptySlotClick, onAppClick, onAppLongClick)
            }
            Spacer(modifier = Modifier.width(180.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                WidgetSlotItem(5, pinnedPackages.getOrNull(5), viewModel, isEditMode, onEmptySlotClick, onAppClick, onAppLongClick)
                WidgetSlotItem(6, pinnedPackages.getOrNull(6), viewModel, isEditMode, onEmptySlotClick, onAppClick, onAppLongClick)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WidgetSlotItem(
    index: Int,
    packageName: String?,
    viewModel: HomeViewModel,
    isEditMode: Boolean,
    onEmptySlotClick: (Int) -> Unit,
    onAppClick: (AppItem) -> Unit,
    onAppLongClick: (Int, AppItem) -> Unit
) {
    val app = if (!packageName.isNullOrEmpty()) viewModel.appFor(packageName) else null

    if (app != null) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .combinedClickable(
                    onClick = { onAppClick(app) },
                    onLongClick = { onAppLongClick(index, app) }
                )
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x991E1A16)),
                contentAlignment = Alignment.Center
            ) {
                Image(bitmap = app.icon, contentDescription = app.label, modifier = Modifier.size(42.dp))
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = app.label, color = Color(0xFFC7B299), fontSize = 11.sp, maxLines = 1)
        }
    } else if (isEditMode) {
        // Nổi ô nét đứt kèm dấu cộng khi nhấn giữ màn hình
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, Color(0xFFD4AF37), RoundedCornerShape(12.dp))
                .background(Color(0x551E1A16))
                .clickable { onEmptySlotClick(index) },
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Thêm app", tint = Color(0xFFD4AF37))
        }
    } else {
        // Mặc định ẩn hoàn toàn ô trống
        Spacer(modifier = Modifier.size(54.dp))
    }
}

@Composable
fun LuxuryBottomDock(
    onAppDrawerClick: () -> Unit,
    onPhoneClick: () -> Unit,
    onMusicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.wrapContentWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xD914120E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. NÚT MENU ĐỨNG ĐẦU TIÊN BÊN TRÁI (ic_launcher.png)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { onAppDrawerClick() },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher),
                    contentDescription = "Menu",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 2. PHÍM TẮT GỌI ĐIỆN THOẠI (icon_phone_gold.png)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { onPhoneClick() },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.icon_phone_gold),
                    contentDescription = "Gọi điện",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 3. TRÌNH PHÁT NHẠC THU NHỎ DR SƠN MUSIC (ic_drson_music.xml)
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x66332D24)),
                modifier = Modifier.clickable { onMusicClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_drson_music),
                        contentDescription = "Music Icon",
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Dr Sơn Music",
                            color = Color(0xFFFFF0B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Nhạc không quảng cáo",
                            color = Color(0xFFB89E72),
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppPickerDialog(
    apps: List<AppItem>,
    onAppSelected: (AppItem) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chọn ứng dụng để ghim", color = Color(0xFFD4AF37)) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(apps) { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onAppSelected(app) }
                            .padding(4.dp)
                    ) {
                        Image(bitmap = app.icon, contentDescription = app.label, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = app.label, color = Color.White, fontSize = 10.sp, maxLines = 1)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = Color(0xFFD4AF37)) }
        },
        containerColor = Color(0xFF1E1A16)
    )
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
    ) {
        // Ảnh nền danh sách ứng dụng
        Image(
            painter = painterResource(id = R.drawable.wallpaper_left_small),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.35f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            // Tiêu đề và nút đóng
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TẤT CẢ ỨNG DỤNG",
                    color = Color(0xFFD4AF37),
                    fontSize = 17.sp,
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

            Spacer(modifier = Modifier.height(10.dp))

            // Tính toán kích thước tự co giãn: 6 cột, khoảng cách = 1/2 kích thước icon
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val availableWidth = maxWidth
                // 6 cột icon + 5 khoảng cách (0.5 icon) = 8.5 đơn vị
                val iconBoxSize = availableWidth / 8.5f
                val spacing = iconBoxSize / 2f

                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    items(apps) { app ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(iconBoxSize)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onAppClick(app) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(iconBoxSize)
                                    .clip(RoundedCornerShape(iconBoxSize * 0.24f))
                                    .background(Color(0xFF221E18))
                                    .padding(iconBoxSize * 0.14f),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = app.icon,
                                    contentDescription = app.label,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.height(5.dp))

                            Text(
                                text = app.label,
                                color = Color(0xFFFFF0B8),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
