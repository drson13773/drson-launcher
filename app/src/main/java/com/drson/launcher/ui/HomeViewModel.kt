package com.drson.launcher.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drson.launcher.R
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
    val dockSlots = mutableStateListOf<String?>().apply { repeat(4) { add(null) } }

    private var layoutRepo: HomeLayoutRepository? = null

    // Nạp trực tiếp ảnh icon_phone_gold từ tài nguyên res/drawable
    private fun getPhoneGoldIcon(context: Context): ImageBitmap {
        return try {
            ResourcesCompat.getDrawable(context.resources, R.drawable.icon_phone_gold, null)
                ?.toBitmap(192, 192)?.asImageBitmap() ?: createFallbackPhoneIcon()
        } catch (_: Exception) {
            createFallbackPhoneIcon()
        }
    }

    private fun createFallbackPhoneIcon(): ImageBitmap {
        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#12110E") }
        canvas.drawRoundRect(8f, 8f, size - 8f, size - 8f, 28f, 28f, bgPaint)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#D4AF37")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(8f, 8f, size - 8f, size - 8f, 28f, 28f, borderPaint)
        return bitmap.asImageBitmap()
    }

    // Logo YouTube Vàng - Đen Chuẩn
    private fun createYouTubeMusicIconBitmap(): Bitmap {
        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#12110E") }
        canvas.drawRoundRect(8f, 8f, size - 8f, size - 8f, 28f, 28f, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#D4AF37")
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }
        canvas.drawRoundRect(8f, 8f, size - 8f, size - 8f, 28f, 28f, borderPaint)

        val ytBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#262015") }
        canvas.drawRoundRect(size * 0.22f, size * 0.30f, size * 0.78f, size * 0.70f, 16f, 16f, ytBoxPaint)
        val ytBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#FFF0B8")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(size * 0.22f, size * 0.30f, size * 0.78f, size * 0.70f, 16f, 16f, ytBorderPaint)

        val playPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#FFDF00")
            style = Paint.Style.FILL
        }
        val triangle = Path().apply {
            moveTo(size * 0.44f, size * 0.40f)
            lineTo(size * 0.62f, size * 0.50f)
            lineTo(size * 0.44f, size * 0.60f)
            close()
        }
        canvas.drawPath(triangle, playPaint)
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
                val act = it.activityInfo.name.lowercase()
                val label = it.loadLabel(pm).toString().lowercase()

                (pkg == appContext.packageName && act.endsWith(".mainactivity")) ||
                pkg.contains("camera") || label.contains("máy ảnh") || label.contains("camera") ||
                pkg.contains("gallery") || pkg.contains("photos") || label.contains("thư viện") || label.contains("ảnh")
            }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }

        val phoneBitmap = getPhoneGoldIcon(appContext)

        val items = resolved.map { info ->
            val pkg = info.activityInfo.packageName
            val activityName = info.activityInfo.name

            val iconBitmap: ImageBitmap = when {
                activityName.contains("DialerActivity") -> phoneBitmap
                activityName.contains("DrSonMusicActivity") -> createYouTubeMusicIconBitmap().asImageBitmap()
                else -> {
                    val customIconRes = IconMapping.packageToIcon[pkg]
                    val mappedBitmap = if (customIconRes != null) {
                        try {
                            ResourcesCompat.getDrawable(appContext.resources, customIconRes, null)
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
        }.toMutableList()

        if (items.none { it.activityClassName.contains("DialerActivity") }) {
            items.add(
                AppItem(
                    label = "Điện thoại",
                    packageName = appContext.packageName,
                    activityClassName = "com.drson.launcher.ui.dialer.DialerActivity",
                    icon = phoneBitmap
                )
            )
        }
        if (items.none { it.activityClassName.contains("DrSonMusicActivity") }) {
            items.add(
                AppItem(
                    label = "Dr Sơn Music",
                    packageName = appContext.packageName,
                    activityClassName = "com.drson.launcher.ui.music.DrSonMusicActivity",
                    icon = createYouTubeMusicIconBitmap().asImageBitmap()
                )
            )
        }

        apps.clear()
        apps.addAll(items.sortedBy { it.label.lowercase() })

        val savedHome = lRepo.loadHomeSlots()
        homeSlots.clear(); homeSlots.addAll(savedHome)
        
        val savedDock = lRepo.loadDockSlots().take(4)
        dockSlots.clear()
        while (dockSlots.size < 4) dockSlots.add(null)
        savedDock.forEachIndexed { index, s -> if (index < 4) dockSlots[index] = s }

        if (dockSlots[0] == null) {
            dockSlots[0] = "com.drson.launcher.ui.dialer.DialerActivity"
        }
        if (dockSlots[1] == null) {
            dockSlots[1] = "com.drson.launcher.ui.music.DrSonMusicActivity"
        }
    }

    fun appFor(identifier: String?): AppItem? {
        if (identifier == null) return null
        return apps.find { it.activityClassName == identifier || it.packageName == identifier }
    }

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

    fun setDockSlot(context: Context, index: Int, identifier: String?) {
        if (index !in 0..3) return
        dockSlots[index] = identifier
        val lRepo = layoutRepo ?: HomeLayoutRepository(context.applicationContext).also { layoutRepo = it }
        viewModelScope.launch { lRepo.setDockSlot(index, identifier) }
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
