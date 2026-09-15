package com.drson.launcher

import android.Manifest
import android.app.AlertDialog
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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drson.launcher.model.AppItem
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

        setContentView(R.layout.activity_main)
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        setupDockRecyclerView()
        setupComposeViews()
        startClockUpdates()
        setupScreenInteractions()
        checkAndRequestPermissions()
    }

    private fun setupDockRecyclerView() {
        rvDockApps = findViewById(R.id.rvDockApps)
        rvDockApps.layoutManager = GridLayoutManager(this, 1, GridLayoutManager.HORIZONTAL, false)

        dockAdapter = DockGridAdapter(
            appList = getDockAppItems(),
            onItemClick = { position, app ->
                if (app != null) {
                    viewModel.launchApp(this, app)
                } else {
                    // Chạm ô trống -> Mở bảng chọn app để thêm vào Dock
                    openAppPickerForSlot(position)
                }
            },
            onItemLongClick = { position, app ->
                if (app != null) {
                    showSlotOptionDialog(position, app)
                } else {
                    openAppPickerForSlot(position)
                }
            }
        )
        rvDockApps.adapter = dockAdapter
    }

    private fun getDockAppItems(): List<AppItem?> {
        val slots = viewModel.dockSlots
        return (0 until 4).map { idx ->
            val pkg = slots.getOrNull(idx)
            if (!pkg.isNullOrEmpty()) viewModel.appFor(pkg) else null
        }
    }

    // Hộp thoại chọn ứng dụng thêm vào Dock
    private fun openAppPickerForSlot(slotIndex: Int) {
        val appList = viewModel.apps
        val appNames = appList.map { it.label }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Chọn ứng dụng ghim vào Dock (Vị trí ${slotIndex + 1})")
            .setItems(appNames) { _, which ->
                val selectedApp = appList[which]
                viewModel.setDockSlot(this, slotIndex, selectedApp.packageName)
                dockAdapter.updateData(getDockAppItems())
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    // Hộp thoại khi nhấn giữ vào ô đã có ứng dụng
    private fun showSlotOptionDialog(slotIndex: Int, app: AppItem) {
        val options = arrayOf("Đổi ứng dụng khác", "Gỡ khỏi thanh Dock")
        AlertDialog.Builder(this)
            .setTitle(app.label)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openAppPickerForSlot(slotIndex)
                    1 -> {
                        viewModel.setDockSlot(this, slotIndex, null)
                        dockAdapter.updateData(getDockAppItems())
                    }
                }
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    private fun setupScreenInteractions() {
        // Nút mở Menu chính
        findViewById<View>(R.id.btnMainMenu)?.setOnClickListener {
            showFullAppDrawerDialog()
        }

        // Nhấn giữ vào khoảng trống trên màn hình chính
        findViewById<ConstraintLayout>(R.id.brandClockContainer)?.setOnLongClickListener {
            showLauncherSettingsDialog()
            true
        }
    }

    private fun showFullAppDrawerDialog() {
        val appList = viewModel.apps
        val appNames = appList.map { it.label }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Tất cả ứng dụng")
            .setItems(appNames) { _, which ->
                val selectedApp = appList[which]
                viewModel.launchApp(this, selectedApp)
            }
            .show()
    }

    private fun showLauncherSettingsDialog() {
        val options = arrayOf("Mở danh sách tất cả ứng dụng", "Mở Cài đặt hệ thống xe")
        AlertDialog.Builder(this)
            .setTitle("Tùy chọn Màn hình chính")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showFullAppDrawerDialog()
                    1 -> {
                        try {
                            startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (_: Exception) {}
                    }
                }
            }
            .show()
    }

    private fun setupComposeViews() {
        findViewById<ComposeView>(R.id.composeRoadBackground)?.setContent {
            DrivingRoadBackground()
        }
        findViewById<ComposeView>(R.id.composeSpeedometer)?.setContent {
            CircularLuxurySpeedometer()
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
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        if (::dockAdapter.isInitialized) {
            dockAdapter.updateData(getDockAppItems())
        }
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
