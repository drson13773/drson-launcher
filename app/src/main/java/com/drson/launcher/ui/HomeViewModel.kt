package com.drson.launcher.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drson.launcher.R
import com.drson.launcher.data.HomeLayoutRepository
import com.drson.launcher.model.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HomeLayoutRepository(application)
    
    var apps: List<AppItem> = emptyList()
        private set

    var dockSlots: List<String?> = listOf(null, null, null, null)
        private set

    init {
        loadInstalledApps()
        loadDockSlots()
    }

    fun loadDockSlots() {
        dockSlots = repository.getDockSlots()
    }

    fun setDockSlot(context: Context, index: Int, packageName: String?) {
        repository.setDockSlot(index, packageName)
        loadDockSlots()
    }

    fun appFor(packageName: String): AppItem? {
        return apps.firstOrNull { it.packageName == packageName }
    }

    fun launchApp(context: Context, app: AppItem) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm: PackageManager = getApplication<Application>().packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val resolveInfos: List<ResolveInfo> = pm.queryIntentActivities(mainIntent, 0)
            val appItemList = mutableListOf<AppItem>()

            for (info in resolveInfos) {
                val pkgName = info.activityInfo.packageName
                // Bỏ qua chính ứng dụng Launcher
                if (pkgName == getApplication<Application>().packageName) continue

                val label = info.loadLabel(pm).toString()
                
                // TỰ ĐỘNG GẮN ICON VÀNG GOLD NẾU LÀ ỨNG DỤNG CAMERA
                val isCameraApp = pkgName.lowercase().contains("camera") || label.lowercase().contains("camera")
                val customIcon = if (isCameraApp) {
                    ContextCompat.getDrawable(getApplication(), R.drawable.icon_camera_gold) ?: info.loadIcon(pm)
                } else {
                    info.loadIcon(pm)
                }

                appItemList.add(
                    AppItem(
                        label = label,
                        packageName = pkgName,
                        icon = customIcon
                    )
                )
            }

            // Sắp xếp danh sách A-Z theo tên
            appItemList.sortBy { it.label.lowercase() }

            withContext(Dispatchers.Main) {
                apps = appItemList
            }
        }
    }
}
