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

        // 1. Nạp bộ Icon Gold Custom với chất lượng cao (300x300 px)
        val phoneIcon = loadDrawableBitmap(R.drawable.icon_phone_gold) ?: createFallbackIcon("#D4AF37")
        val musicIcon = loadDrawableBitmap(R.drawable.icon_music_gold) ?: createFallbackIcon("#D4AF37")
        val contactsIcon = loadDrawableBitmap(R.drawable.icon_contacts_gold)
        val galleryIcon = loadDrawableBitmap(R.drawable.icon_gallery_gold)
        val browserIcon = loadDrawableBitmap(R.drawable.icon_browser_gold)
        val settingsIcon = loadDrawableBitmap(R.drawable.icon_settings_gold)

        // 2. Thêm App Gọi Điện Thoại Batman Gold
        apps.add(
            AppItem(
                packageName = "com.drson.launcher.dialer",
                activityClassName = DialerActivity::class.java.name,
                label = "Điện thoại",
                icon = phoneIcon
            )
        )

        // 3. Thêm App Dr. Sơn Music
        apps.add(
            AppItem(
                packageName = "com.drson.launcher.music",
                activityClassName = DrSonMusicActivity::class.java.name,
                label = "Dr. Sơn Music",
                icon = musicIcon
            )
        )

        // 4. Quét và thay thế icon cho các ứng dụng hệ thống Zestech
        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            if (pkg == getApplication<Application>().packageName) continue
            val actName = info.activityInfo.name ?: ""
            val label = info.loadLabel(pm).toString()
            val lowerPkg = pkg.lowercase()
            val lowerLabel = label.lowercase()

            val customIcon: ImageBitmap? = when {
                (lowerPkg.contains("contact") || lowerLabel.contains("danh bạ")) && contactsIcon != null -> contactsIcon
                (lowerPkg.contains("gallery") || lowerPkg.contains("media") || lowerPkg.contains("photo") || lowerLabel.contains("ảnh") || lowerLabel.contains("bộ sưu tập")) && galleryIcon != null -> galleryIcon
                (lowerPkg.contains("browser") || lowerPkg.contains("chrome") || lowerLabel.contains("trình duyệt") || lowerLabel.contains("web")) && browserIcon != null -> browserIcon
                (lowerPkg.contains("setting") || lowerLabel.contains("cài đặt") || lowerLabel.contains("thiết lập")) && settingsIcon != null -> settingsIcon
                (lowerPkg.contains("music") || lowerPkg.contains("audio") || lowerLabel.contains("nhạc")) && musicIcon != null -> musicIcon
                else -> null
            }

            val icon = customIcon ?: try {
                val drawable = info.loadIcon(pm)
                drawable.toBitmap(256, 256, Bitmap.Config.ARGB_8888).asImageBitmap()
            } catch (_: Exception) {
                createFallbackIcon("#555555")
            }

            apps.add(
                AppItem(
                    packageName = pkg,
                    activityClassName = actName,
                    label = label,
                    icon = icon
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
            drawable?.toBitmap(320, 320, Bitmap.Config.ARGB_8888)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    private fun createFallbackIcon(hexColor: String): ImageBitmap {
        val b = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor(hexColor) }
        c.drawRoundRect(10f, 10f, 246f, 246f, 40f, 40f, p)
        return b.asImageBitmap()
    }
}
