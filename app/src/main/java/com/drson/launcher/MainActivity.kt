package com.drson.launcher

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drson.launcher.music.PureMusicActivity
import com.drson.launcher.ui.CircularLuxurySpeedometer
import com.drson.launcher.ui.DrivingRoadBackground
import com.drson.launcher.ui.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()

        setContentView(R.layout.activity_main)
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        setupComposeViews()
        startClockUpdates()
        setupDockClicks()
        checkAndRequestPermissions()
    }

    private fun setupDockClicks() {
        // 1. NÚT MENU: Mở danh sách tất cả App dạng lưới 6 cột
        findViewById<View>(R.id.btnMainMenu)?.setOnClickListener {
            showFullAppDrawerDialog()
        }

        // 2. NÚT ĐIỆN THOẠI: Mở bàn phím quay số
        findViewById<View>(R.id.btnDockPhone)?.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(this, "Không tìm thấy ứng dụng gọi điện", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. NÚT CAMERA 360: Tìm app camera 360/camera hành trình
        findViewById<View>(R.id.btnDockCamera)?.setOnClickListener {
            openAppByKeywords(
                listOf("camera", "cam360", "panorama", "dvr", "cam"),
                "Mở Camera"
            )
        }

        // 4. NÚT DR SƠN MUSIC: Mở màn hình nghe nhạc không quảng cáo
        findViewById<View>(R.id.btnDrSonMusic)?.setOnClickListener {
            val intent = Intent(this, PureMusicActivity::class.java)
            startActivity(intent)
        }

        // 5. NÚT ZING MP3: Tìm app Zing MP3
        findViewById<View>(R.id.btnDockZingMp3)?.setOnClickListener {
            openAppByKeywords(
                listOf("com.zing.mp3", "zingmp3", "zing"),
                "Zing MP3"
            )
        }

        // 6. NÚT VIETMAP: Tìm app Vietmap Live / Vietmap S1 / Vietmap S2
        findViewById<View>(R.id.btnDockVietmap)?.setOnClickListener {
            openAppByKeywords(
                listOf("vietmap", "live.vietmap", "navigation", "navitel", "maps"),
                "Vietmap"
            )
        }

        // Bấm vào Logo phía trên để mở nhanh Dr Sơn Music
        findViewById<View>(R.id.imgBrandLogo)?.setOnClickListener {
            startActivity(Intent(this, PureMusicActivity::class.java))
        }
    }

    /**
     * Tìm kiếm thông minh và khởi chạy ứng dụng theo danh sách từ khóa Package/Tên
     */
    private fun openAppByKeywords(keywords: List<String>, appTitle: String) {
        val app = viewModel.apps.firstOrNull { item ->
            keywords.any { kw ->
                item.packageName.lowercase().contains(kw) || item.label.lowercase().contains(kw)
            }
        }

        if (app != null) {
            viewModel.launchApp(this, app)
        } else {
            // Thử khởi chạy trực tiếp qua package nếu là app đặc thù
            val directPackages = when (appTitle) {
                "Zing MP3" -> listOf("com.zing.mp3")
                "Vietmap" -> listOf("com.vietmap.live", "com.vietmap.s1", "com.vietmap.s2")
                else -> emptyList()
            }
            var launched = false
            for (pkg in directPackages) {
                val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launchIntent)
                    launched = true
                    break
                }
            }
            if (!launched) {
                Toast.makeText(this, "Không tìm thấy $appTitle trên thiết bị", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showFullAppDrawerDialog() {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_app_drawer)

        dialog.window?.let { w ->
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        val rvGrid = dialog.findViewById<RecyclerView>(R.id.rvAppDrawerGrid)
        val btnClose = dialog.findViewById<ImageView>(R.id.btnAppDrawerClose)

        rvGrid.layoutManager = GridLayoutManager(this, 6)
        rvGrid.adapter = AppDrawerAdapter(viewModel.apps) { app ->
            dialog.dismiss()
            viewModel.launchApp(this, app)
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupComposeViews() {
        findViewById<ComposeView>(R.id.composeRoadBackground)?.setContent {
            val speed by viewModel.currentSpeed
            DrivingRoadBackground(speedKmH = speed)
        }
        findViewById<ComposeView>(R.id.composeSpeedometer)?.setContent {
            val speed by viewModel.currentSpeed
            CircularLuxurySpeedometer(speedKmH = speed)
        }
    }

    private fun startClockUpdates() {
        val tvTime = findViewById<TextView>(R.id.tvClockTime)
        val tvDate = findViewById<TextView>(R.id.tvClockDate)
        lifecycleScope.launch {
            val tFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dFormat = SimpleDateFormat("EEEE, dd 'thg' M", Locale("vi", "VN"))
            while (true) {
                val now = Calendar.getInstance().time
                tvTime?.text = tFormat.format(now)
                tvDate?.text = dFormat.format(now)
                delay(1000)
            }
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
        if (hasFocus) {
            hideSystemBars()
        }
    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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
