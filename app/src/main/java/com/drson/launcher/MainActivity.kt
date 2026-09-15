package com.drson.launcher

import android.Manifest
import android.app.AlertDialog
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
import com.drson.launcher.model.AppItem
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
    private lateinit var dockAdapter: DockGridAdapter
    private lateinit var rvDockApps: RecyclerView
    private var isEditMode: Boolean = false

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
            isEditMode = false,
            onItemClick = { position, app ->
                if (app != null) {
                    viewModel.launchApp(this, app)
                } else {
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

    private fun toggleEditMode() {
        isEditMode = !isEditMode
        dockAdapter.setEditMode(isEditMode)
    }

    private fun openAppPickerForSlot(slotIndex: Int) {
        val appList = viewModel.apps
        val appNames = appList.map { it.label }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Chọn ứng dụng ghim vào Dock (Vị trí ${slotIndex + 1})")
            .setItems(appNames) { _, which ->
                val selectedApp = appList[which]
                viewModel.setDockSlot(this, slotIndex, selectedApp.packageName)
                isEditMode = false
                dockAdapter.updateData(getDockAppItems(), editMode = false)
            }
            .setNegativeButton("Hủy") { _, _ ->
                isEditMode = false
                dockAdapter.setEditMode(false)
            }
            .show()
    }

    private fun showSlotOptionDialog(slotIndex: Int, app: AppItem) {
        val options = arrayOf("Đổi ứng dụng khác", "Gỡ khỏi thanh Dock", "Bật chế độ chỉnh sửa Dock")
        AlertDialog.Builder(this)
            .setTitle(app.label)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openAppPickerForSlot(slotIndex)
                    1 -> {
                        viewModel.setDockSlot(this, slotIndex, null)
                        dockAdapter.updateData(getDockAppItems(), editMode = isEditMode)
                    }
                    2 -> toggleEditMode()
                }
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    private fun setupScreenInteractions() {
        // Nút mở Danh sách ứng dụng toàn màn hình dạng lưới 6 cột
        findViewById<View>(R.id.btnMainMenu)?.setOnClickListener {
            showFullAppDrawerDialog()
        }

        // Mở Dr Sơn Music
        findViewById<View>(R.id.btnDrSonMusic)?.setOnClickListener {
            val intent = Intent(this, PureMusicActivity::class.java)
            startActivity(intent)
        }

        // Bấm vào Logo để mở Dr Sơn Music
        findViewById<View>(R.id.imgBrandLogo)?.setOnClickListener {
            val intent = Intent(this, PureMusicActivity::class.java)
            startActivity(intent)
        }

        // Nhấn giữ Widget Logo / Đồng hồ -> Menu cài đặt Launcher
        findViewById<View>(R.id.brandClockContainer)?.setOnLongClickListener {
            showLauncherSettingsDialog()
            true
        }

        // Nhấn giữ Dock -> Chế độ chỉnh sửa ô trống (+)
        findViewById<View>(R.id.bottomDockCard)?.setOnLongClickListener {
            toggleEditMode()
            true
        }
    }

    /**
     * Mở giao diện danh sách toàn bộ ứng dụng dạng lưới 6 CỘT
     */
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

        // THIẾT LẬP LƯỚI 6 CỘT
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

    private fun showLauncherSettingsDialog() {
        val options = arrayOf(
            if (isEditMode) "Tắt chế độ chỉnh sửa Dock" else "Chỉnh sửa thanh Dock (Thêm/Bớt App)",
            "Mở ứng dụng Dr Sơn Music",
            "Mở danh sách tất cả ứng dụng",
            "Mở Cài đặt hệ thống xe"
        )
        AlertDialog.Builder(this)
            .setTitle("Tùy chọn Màn hình chính")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> toggleEditMode()
                    1 -> startActivity(Intent(this, PureMusicActivity::class.java))
                    2 -> showFullAppDrawerDialog()
                    3 -> {
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
            dockAdapter.updateData(getDockAppItems(), editMode = isEditMode)
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
