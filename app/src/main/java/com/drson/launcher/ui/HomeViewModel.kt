package com.drson.launcher.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import com.drson.launcher.R
import com.drson.launcher.model.AppItem
import com.drson.launcher.model.HomeSlotContent
import com.drson.launcher.ui.dialer.DialerActivity
import com.drson.launcher.ui.music.DrSonMusicActivity

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    val apps = mutableStateListOf<AppItem>()
    val homeSlots = mutableStateListOf<HomeSlotContent>()
    val dockSlots = mutableStateListOf<String?>()

    init {
        loadInstalledApps()
        loadDefaultSlots()
    }

    fun loadInstalledApps() {
        val pm = getApplication<Application>().packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)

        apps.clear()

        // 1. Thêm Ứng dụng Điện thoại (Dùng icon_phone_gold.png)
        val phoneIcon = loadDrawableBitmap(R.drawable.icon_phone_gold) ?: createPhoneGoldFallbackBitmap()
        apps.add(
            AppItem(
                packageName = "com.drson.launcher.dialer",
                activityClassName = DialerActivity::class.java.name,
                label = "Điện thoại",
                icon = phoneIcon
            )
        )

        // 2. Thêm Ứng dụng YouTube Music Dr Sơn
        val musicIcon = loadDrawableBitmap(R.drawable.icon_menu_brand) ?: createMusicFallbackBitmap()
        apps.add(
            AppItem(
                packageName = "com.drson.launcher.music",
                activityClassName = DrSonMusicActivity::class.java.name,
                label = "Dr. Sơn Music",
                icon = musicIcon
            )
        )

        // 3. Thêm các ứng dụng cài đặt trên màn hình Android
        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            if (pkg == getApplication<Application>().packageName) continue
            val actName = info.activityInfo.name ?: ""
            val label = info.loadLabel(pm).toString()
            val drawable = info.loadIcon(pm)
            val bitmap = drawable.toBitmap(128, 128, Bitmap.Config.ARGB_8888).asImageBitmap()
            apps.add(
                AppItem(
                    packageName = pkg,
                    activityClassName = actName,
                    label = label,
                    icon = bitmap
                )
            )
        }
    }

    private fun loadDefaultSlots() {
        homeSlots.clear()
        dockSlots.clear()

        // Gán sẵn Dock: Ô 1 Điện thoại, Ô 2 Music
        dockSlots.add("com.drson.launcher.dialer")
        dockSlots.add("com.drson.launcher.music")
        dockSlots.add(null)
        dockSlots.add(null)

        // Các slot trên Desktop: chỉ nạp các app khả dụng
        val availableApps = apps.filter {
            it.packageName != "com.drson.launcher.dialer" && it.packageName != "com.drson.launcher.music"
        }
        for (i in 0 until minOf(6, availableApps.size)) {
            homeSlots.add(HomeSlotContent.App(availableApps[i].packageName))
        }
    }

    fun appFor(packageName: String?): AppItem? {
        if (packageName == null) return null
        return apps.find { it.packageName == packageName }
    }

    fun launchApp(context: Context, app: AppItem) {
        when (app.packageName) {
            "com.drson.launcher.dialer" -> {
                val intent = Intent(context, DialerActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
            "com.drson.launcher.music" -> {
                val intent = Intent(context, DrSonMusicActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
            else -> {
                if (app.activityClassName.isNotBlank()) {
                    val intent = Intent().apply {
                        component = ComponentName(app.packageName, app.activityClassName)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(intent)
                        return
                    } catch (_: Exception) {}
                }
                val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                }
            }
        }
    }

    fun setDockSlot(context: Context, index: Int, packageName: String?) {
        if (index in 0 until dockSlots.size) {
            dockSlots[index] = packageName
        }
    }

    private fun loadDrawableBitmap(resId: Int): ImageBitmap? {
        return try {
            val drawable = ResourcesCompat.getDrawable(getApplication<Application>().resources, resId, null)
            drawable?.toBitmap(160, 160, Bitmap.Config.ARGB_8888)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    private fun createPhoneGoldFallbackBitmap(): ImageBitmap {
        val b = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#D4AF37") }
        c.drawRoundRect(10f, 10f, 150f, 150f, 30f, 30f, p)
        return b.asImageBitmap()
    }

    private fun createMusicFallbackBitmap(): ImageBitmap {
        val b = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#FF0000") }
        c.drawRoundRect(10f, 10f, 150f, 150f, 30f, 30f, p)
        return b.asImageBitmap()
    }
}
