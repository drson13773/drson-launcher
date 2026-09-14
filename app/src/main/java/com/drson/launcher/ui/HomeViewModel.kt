package com.drson.launcher.ui

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
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drson.launcher.data.HomeLayoutRepository
import com.drson.launcher.data.IconMapping
import com.drson.launcher.data.LabelMapping
import com.drson.launcher.model.AppItem
import com.drson.launcher.model.HomeSlotContent
import com.drson.launcher.widget.WidgetHostController
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    val apps = mutableStateListOf<AppItem>()
    val homeSlots = mutableStateListOf<HomeSlotContent?>().apply { repeat(com.drson.launcher.data.HOME_SLOT_COUNT) { add(null) } }
    
    // Cố định đúng 4 ô ứng dụng trên Dock (ngoài nút Menu chính)
    val dockSlots = mutableStateListOf<String?>().apply { repeat(4) { add(null) } }

    private var layoutRepo: HomeLayoutRepository? = null

    private fun createPhoneIconBitmap(): Bitmap {
        val size = 120
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#1B5E20") }
        canvas.drawRoundRect(8f, 8f, size - 8f, size - 8f, 26f, 26f, bgPaint)
        
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#FFF0B8")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(8f, 8f, size - 8f, size - 8f, 26f, 26f, strokePaint)

        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#FFFFFF")
            style = Paint.Style.FILL
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 10f
        }
        canvas.drawCircle(size * 0.42f, size * 0.42f, 14f, iconPaint)
        canvas.drawCircle(size * 0.58f, size * 0.58f, 14f, iconPaint)
        canvas.drawLine(size * 0.42f, size * 0.42f, size * 0.58f, size * 0.58f, iconPaint)
        return bitmap
    }

    private fun createMusicIconBitmap(): Bitmap {
        val size = 120
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#2B1E05") }
        canvas.drawRoundRect(8f, 8f, size - 8f, size - 8f, 26f, 26f, bgPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#D4AF37")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(8f, 8f, size - 8f, size - 8f, 26f, 26f, strokePaint)

        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#FFF0B8")
            style = Paint.Style.FILL
            strokeWidth = 8f
        }
        canvas.drawCircle(size * 0.38f, size * 0.65f, 12f, goldPaint)
        canvas.drawCircle(size * 0.65f, size * 0.55f, 12f, goldPaint)
        canvas.drawLine(size * 0.46f, size * 0.65f, size * 0.46f, size * 0.30f, goldPaint)
        canvas.drawLine(size * 0.73f, size * 0.55f, size * 0.73f, size * 0.20f, goldPaint)
        canvas.drawLine(size * 0.46f, size * 0.30f, size * 0.73f, size * 0.20f, goldPaint)
        return bitmap
    }

    suspend fun load(context: Context) {
        val appContext = context.applicationContext
        val lRepo = layoutRepo ?: HomeLayoutRepository(appContext).also { layoutRepo = it }

        val pm = appContext.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        
        val resolved = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .filterNot { 
                val pkg = it.activityInfo.packageName.lowercase()
                val label = it.loadLabel(pm).toString().lowercase()

                (pkg == appContext.packageName && it.activityInfo.name.endsWith(".MainActivity")) ||
                pkg.contains("camera") || label.contains("máy ảnh") || label.contains("camera") ||
                pkg.contains("gallery") || pkg.contains("photos") || label.contains("thư viện") || label.contains("ảnh")
            }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }

        val items = resolved.map { info ->
            val pkg = info.activityInfo.packageName
            val activityName = info.activityInfo.name

            val iconBitmap: ImageBitmap = when {
                activityName.contains("DialerActivity") -> {
                    createPhoneIconBitmap().asImageBitmap()
                }
                activityName.contains("DrSonMusicActivity") -> {
                    createMusicIconBitmap().asImageBitmap()
                }
                else -> {
                    val customIconRes = IconMapping.packageToIcon[pkg]
                    val mappedBitmap = if (customIconRes != null) {
                        try {
                            androidx.core.content.res.ResourcesCompat.getDrawable(appContext.resources, customIconRes, null)
                                ?.toBitmap(120, 120)?.asImageBitmap()
                        } catch (_: Exception) { null }
                    } else null

                    mappedBitmap ?: info.loadIcon(pm).toBitmap(120, 120).asImageBitmap()
                }
            }

            val label = when {
                activityName.contains("DialerActivity") -> "Điện thoại"
                activityName.contains("DrSonMusicActivity") -> "Dr Sơn Music"
                else -> LabelMapping.packageToLabel[pkg] ?: info.loadLabel(pm).toString()
            }

            AppItem(
                label = label,
                packageName = pkg,
                activityClassName = activityName,
                icon = iconBitmap,
            )
        }
        apps.clear()
        apps.addAll(items)

        val savedHome = lRepo.loadHomeSlots()
        homeSlots.clear(); homeSlots.addAll(savedHome)
        
        val savedDock = lRepo.loadDockSlots().take(4)
        dockSlots.clear()
        while (dockSlots.size < 4) dockSlots.add(null)
        savedDock.forEachIndexed { index, s -> if (index < 4) dockSlots[index] = s }

        if (dockSlots.all { it == null }) {
            val phone = apps.find { it.activityClassName.contains("DialerActivity") }
            if (phone != null) setDockSlot(context, 0, phone.packageName)

            val music = apps.find { it.activityClassName.contains("DrSonMusicActivity") }
            if (music != null) setDockSlot(context, 1, music.packageName)
        }
    }

    fun appFor(packageName: String?): AppItem? = packageName?.let { pn -> apps.find { it.packageName == pn } }

    fun setHomeSlot(context: Context, index: Int, content: HomeSlotContent?) {
        if (index !in homeSlots.indices) return
        val old = homeSlots[index]
        if (old is HomeSlotContent.Widget && old != content) {
            WidgetHostController.deleteId(context, old.appWidgetId)
        }
        homeSlots[index] = content
        val lRepo = layoutRepo ?: HomeLayoutRepository(context.applicationContext).also { layoutRepo = it }
        viewModelScope.launch { lRepo.setHomeSlot(index, content) }
    }

    fun setDockSlot(context: Context, index: Int, packageName: String?) {
        if (index !in 0..3) return
        dockSlots[index] = packageName
        val lRepo = layoutRepo ?: HomeLayoutRepository(context.applicationContext).also { layoutRepo = it }
        viewModelScope.launch { lRepo.setDockSlot(index, packageName) }
    }

    fun launchApp(context: Context, app: AppItem) {
        val explicitIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(app.packageName, app.activityClassName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val started = try {
            context.startActivity(explicitIntent)
            true
        } catch (_: Exception) {
            false
        }
        if (!started) {
            context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { fallback ->
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(fallback)
            }
        }
    }
}
