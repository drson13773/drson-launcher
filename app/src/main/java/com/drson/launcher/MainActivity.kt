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
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drson.launcher.ui.CircularLuxurySpeedometer
import com.drson.launcher.ui.DrivingRoadBackground
import com.drson.launcher.ui.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var dockAdapter: DockGridAdapter
    private lateinit var rvDockApps: RecyclerView

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
    ) {
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

        // 1. Nạp file layout XML
        setContentView(R.layout.activity_main)

        // 2. Khởi tạo ViewModel
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        // 3. Khởi tạo RecyclerView & GridLayoutManager
        rvDockApps = findViewById(R.id.rvDockApps)
        rvDockApps.layoutManager = GridLayoutManager(this, 1, GridLayoutManager.HORIZONTAL, false)

        dockAdapter = DockGridAdapter(
            appList = viewModel.dockSlots.map { pkg -> viewModel.appFor(pkg) },
            onItemClick = { app -> viewModel.launchApp(this, app) },
            onItemLongClick = { _ -> }
        )
        rvDockApps.adapter = dockAdapter

        // 4. Nhúng đường chạy 3D và đồng hồ tốc độ
        findViewById<ComposeView>(R.id.composeRoadBackground)?.setContent {
            DrivingRoadBackground()
        }
        findViewById<ComposeView>(R.id.composeSpeedometer)?.setContent {
            CircularLuxurySpeedometer()
        }

        // 5. Cập nhật đồng hồ thời gian thực
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

        // 6. Kiểm tra quyền
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
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
