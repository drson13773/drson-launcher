package com.drson.launcher.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.runtime.mutableStateListOf
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

    // Vị trí cố định trên Home Screen (app hoặc widget - null = ô trống) + Dock (chỉ app).
    val homeSlots = mutableStateListOf<HomeSlotContent?>().apply { repeat(com.drson.launcher.data.HOME_SLOT_COUNT) { add(null) } }
    val dockSlots = mutableStateListOf<String?>().apply { repeat(com.drson.launcher.data.DOCK_SLOT_COUNT) { add(null) } }

    private var layoutRepo: HomeLayoutRepository? = null

    suspend fun load(context: Context) {
        val appContext = context.applicationContext
        val lRepo = layoutRepo ?: HomeLayoutRepository(appContext).also { layoutRepo = it }

        val pm = appContext.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val resolved = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .filterNot { it.activityInfo.packageName == appContext.packageName && it.activityInfo.name.endsWith(".MainActivity") }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }

        val items = resolved.map { info ->
            val pkg = info.activityInfo.packageName
            val customIconRes = IconMapping.packageToIcon[pkg]
            val iconBitmap = if (customIconRes != null) {
                androidx.core.content.res.ResourcesCompat.getDrawable(appContext.resources, customIconRes, null)
                    ?.toBitmap()?.asImageBitmap()
            } else {
                info.loadIcon(pm).toBitmap().asImageBitmap()
            }
            AppItem(
                label = LabelMapping.packageToLabel[pkg] ?: info.loadLabel(pm).toString(),
                packageName = pkg,
                activityClassName = info.activityInfo.name,
                icon = iconBitmap ?: info.loadIcon(pm).toBitmap().asImageBitmap(),
            )
        }
        apps.clear()
        apps.addAll(items)

        val savedHome = lRepo.loadHomeSlots()
        homeSlots.clear(); homeSlots.addAll(savedHome)
        val savedDock = lRepo.loadDockSlots()
        dockSlots.clear(); dockSlots.addAll(savedDock)

        // Lần đầu mở app: Gán sẵn Điện thoại (Slot 0) và Dr Sơn Music (Slot 1) vào Dock
        if (savedDock.all { it == null }) {
            // 1. Gán ô 0: Điện thoại (Dialer)
            val phone = apps.find { it.activityClassName.contains("DialerActivity") || it.packageName == appContext.packageName }
            if (phone != null) setDockSlot(context, 0, phone.packageName)

            // 2. Gán ô 1: Dr Sơn Music
            val music = apps.find { it.activityClassName.contains("DrSonMusicActivity") || it.label.contains("Music", ignoreCase = true) }
            if (music != null) {
                setDockSlot(context, 1, music.packageName)
            }
        }
    }

    fun appFor(packageName: String?): AppItem? = packageName?.let { pn -> apps.find { it.packageName == pn } }

    /** Gán app hoặc widget vào 1 ô Home Screen (`content = null` để gỡ ô về trạng thái trống). */
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

    fun setHomeSlotApp(context: Context, index: Int, packageName: String?) {
        setHomeSlot(context, index, packageName?.let { HomeSlotContent.App(it) })
    }

    fun setDockSlot(context: Context, index: Int, packageName: String?) {
        if (index !in dockSlots.indices) return
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
        } catch (e: Exception) {
            false
        }
        if (!started) {
            context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { fallback ->
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(fallback)
            }
        }
        trackRecentApp(app.packageName)
    }

    val recentApps = mutableStateListOf<AppItem>()

    private fun trackRecentApp(packageName: String) {
        val app = apps.find { it.packageName == packageName } ?: return
        recentApps.removeAll { it.packageName == packageName }
        recentApps.add(0, app)
        while (recentApps.size > 8) recentApps.removeAt(recentApps.size - 1)
    }

    fun removeFromRecents(app: AppItem) {
        recentApps.remove(app)
    }
}
