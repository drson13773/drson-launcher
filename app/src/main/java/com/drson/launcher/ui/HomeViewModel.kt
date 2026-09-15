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

    fun load() {
        loadInstalledApps()
    }

    fun loadInstalledApps() {
        val pm = getApplication<Application>().packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)

        apps.clear()

        // 1. Icon App Điện thoại (icon_phone_gold.png độ phân giải gốc 512x512)
        val phoneIcon = loadDrawableBitmap(R.drawable.icon_phone_gold) ?: createPhoneGoldFallbackBitmap()
        apps.add(
            AppItem(
                packageName = "com.drson.launcher.dialer",
                activityClassName = DialerActivity::class.java.name,
                label = "Điện thoại",
                icon = phoneIcon
            )
        )

        // 2. Icon App Nghe nhạc Dr Sơn Music (Icon Nhạc Gold sắc nét)
        val musicIcon = createMusicGoldIconBitmap()
        apps.add(
            AppItem(
                packageName = "com.drson.launcher.music",
                activityClassName = DrSonMusicActivity::class.java.name,
                label = "Dr. Sơn Music",
                icon = musicIcon
            )
        )

        // 3. Các ứng dụng khác của hệ thống
        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            if (pkg == getApplication<Application>().packageName) continue
            val actName = info.activityInfo.name ?: ""
            val label = info.loadLabel(pm).toString()
            val drawable = info.loadIcon(pm)
            val bitmap = drawable.toBitmap(256, 256, Bitmap.Config.ARGB_8888).asImageBitmap()
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

        // Gán thanh Dock: Ô 1 Điện thoại, Ô 2 Music
        dockSlots.add("com.drson.launcher.dialer")
        dockSlots.add("com.drson.launcher.music")
        dockSlots.add(null)
        dockSlots.add(null)
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
            drawable?.toBitmap(300, 300, Bitmap.Config.ARGB_8888)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    private fun createPhoneGoldFallbackBitmap(): ImageBitmap {
        val b = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#D4AF37") }
        c.drawRoundRect(10f, 10f, 246f, 246f, 40f, 40f, p)
        return b.asImageBitmap()
    }

    private fun createMusicGoldIconBitmap(): ImageBitmap {
        val b = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        
        // Khung nền vuông bo góc Carbon
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#151310") }
        c.drawRoundRect(8f, 8f, 248f, 248f, 48f, 48f, bgPaint)
        
        // Viền vàng kim 3D
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
            color = android.graphics.Color.parseColor("#D4AF37")
            style = Paint.Style.STROKE
            strokeWidth = 10f
        }
        c.drawRoundRect(8f, 8f, 248f, 248f, 48f, 48f, borderPaint)

        // Vòng tròn trung tâm
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
            color = android.graphics.Color.parseColor("#B8860B")
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        c.drawCircle(128f, 128f, 85f, circlePaint)

        // Biểu tượng tam giác Play / Nốt nhạc vàng
        val playPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
            color = android.graphics.Color.parseColor("#FFF0B8")
            style = Paint.Style.FILL
        }
        val path = android.graphics.Path().apply {
            moveTo(110f, 95f)
            lineTo(165f, 128f)
            lineTo(110f, 161f)
            close()
        }
        c.drawPath(path, playPaint)

        return b.asImageBitmap()
    }
}
