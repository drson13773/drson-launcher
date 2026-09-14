package com.drson.launcher.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

/**
 * Android không cho app thường (kể cả launcher) đọc danh sách "recent tasks" thật của hệ thống
 * hay lấy live-thumbnail của app khác - các API đó (`ActivityManager.getRunningTasks`,
 * `IActivityTaskManager`...) chỉ dành cho app hệ thống/privileged. Cách duy nhất một app thường
 * có thể mở ĐÚNG màn hình Recents/Overview thật của Android mà không cần root là thông qua
 * `AccessibilityService.performGlobalAction(GLOBAL_ACTION_RECENTS)`.
 *
 * Service này không làm gì khác ngoài việc giữ 1 tham chiếu tĩnh để gọi hành động đó khi người
 * dùng bấm nút "Recents hệ thống" trong App Switcher. Người dùng cần bật quyền Accessibility cho
 * app 1 lần trong Settings (xem SystemRecents.openAccessibilitySettings).
 */
class LauncherAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = WeakReference(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        instanceRef = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Không cần xử lý sự kiện gì - service này chỉ dùng để gọi performGlobalAction.
    }

    override fun onInterrupt() {}

    companion object {
        private var instanceRef: WeakReference<LauncherAccessibilityService>? = null

        fun isEnabled(): Boolean = instanceRef?.get() != null

        /** Trả về true nếu đã gọi được hành động mở Recents thật; false nếu service chưa được bật. */
        fun openSystemRecents(): Boolean {
            val service = instanceRef?.get() ?: return false
            return service.performGlobalAction(GLOBAL_ACTION_RECENTS)
        }

        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
