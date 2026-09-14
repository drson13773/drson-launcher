package com.drson.launcher.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawable.toBitmap
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

        // Lần đầu mở app (chưa gán gì) - tự điền sẵn app Điện thoại (dialer riêng của launcher
        // này) vào ô đầu tiên của Dock cho có sẵn, đỡ trống trơn.
        if (savedDock.all { it == null }) {
            val phone = apps.find { it.packageName == appContext.packageName }
            if (phone != null) setDockSlot(context, 0, phone.packageName)
        }
    }

    fun appFor(packageName: String?): AppItem? = packageName?.let { pn -> apps.find { it.packageName == pn } }

    /** Gán app hoặc widget vào 1 ô Home Screen (`content = null` để gỡ ô về trạng thái trống). */
    fun setHomeSlot(context: Context, index: Int, content: HomeSlotContent?) {
        if (index !in homeSlots.indices) return
        val old = homeSlots[index]
        // Nếu ô cũ đang chứa widget và sắp bị thay/gỡ, giải phóng widget id để không rò rỉ.
        if (old is HomeSlotContent.Widget && old != content) {
            WidgetHostController.deleteId(context, old.appWidgetId)
        }
        homeSlots[index] = content
        val lRepo = layoutRepo ?: HomeLayoutRepository(context.applicationContext).also { layoutRepo = it }
        viewModelScope.launch { lRepo.setHomeSlot(index, content) }
    }

    /** Tiện ích gọi nhanh khi gán app (đa số chỗ gọi vẫn chỉ làm việc với app, không phải widget). */
    fun setHomeSlotApp(context: Context, index: Int, packageName: String?) {
        setHomeSlot(context, index, packageName?.let { HomeSlotContent.App(it) })
    }

    fun setDockSlot(context: Context, index: Int, packageName: String?) {
        if (index !in dockSlots.indices) return
        dockSlots[index] = packageName
        val lRepo = layoutRepo ?: HomeLayoutRepository(context.applicationContext).also { layoutRepo = it }
        viewModelScope.launch { lRepo.setDockSlot(index, packageName) }
    }

    /**
     * Mở TRỰC TIẾP đúng activity của app này (`ComponentName(packageName, activityClassName)`)
     * thay vì chỉ dựa vào packageName - tránh trường hợp 1 packageName có nhiều activity cùng là
     * "màn hình chính" (như app Điện thoại riêng dùng chung packageName với chính launcher này)
     * khiến Android chọn nhầm activity. Nếu vì lý do gì đó activity đích không mở được (app đã bị
     * gỡ/đổi bên ngoài chẳng hạn), rơi về cách mở theo packageName như cũ để không bị đứng app.
     */
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

    // ---------- App Switcher: lịch sử app đã mở qua launcher này ----------
    // Android không cấp quyền cho app thường đọc "recent tasks" thật hay lấy live-thumbnail
    // của app khác, nên đây là lịch sử do chính launcher ghi lại (xem AppSwitcherOverlay.kt).
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
