package com.drson.launcher.notifications

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.lang.ref.WeakReference

/**
 * Đọc thông báo THẬT của hệ thống - khác với App Switcher (Android không cho app thường xem
 * task/thumbnail thật), thông báo thì CÓ API chính thức cho việc này: `NotificationListenerService`.
 * Người dùng cần bật quyền "Notification access" cho app 1 lần trong Settings (xem
 * [NotificationAccess]); sau đó service này chạy nền, nhận mọi thông báo hệ thống và đẩy vào
 * [NotificationRepository] để Compose UI hiển thị.
 */
class LauncherNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instanceRef = WeakReference(this)
        refreshAll()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instanceRef = null
        NotificationRepository.clear()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        if (sbn.packageName == packageName) return // bỏ qua thông báo của chính launcher (nếu có)
        NotificationRepository.upsert(sbn.toEntry(this))
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        NotificationRepository.remove(sbn.key)
    }

    private fun refreshAll() {
        val entries = try {
            activeNotifications
                .filter { it.packageName != packageName }
                .map { it.toEntry(this) }
        } catch (e: SecurityException) {
            emptyList()
        }
        NotificationRepository.replaceAll(entries)
    }

    private fun StatusBarNotification.toEntry(context: Context): NotificationEntry {
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()

        val appLabel = try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
        val icon = try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }

        return NotificationEntry(
            key = key,
            packageName = packageName,
            appLabel = appLabel,
            icon = icon,
            title = title,
            text = text,
            postTime = postTime,
            contentIntent = notification.contentIntent,
        )
    }

    companion object {
        private var instanceRef: WeakReference<LauncherNotificationListenerService>? = null

        fun isRunning(): Boolean = instanceRef?.get() != null

        fun dismiss(key: String) {
            instanceRef?.get()?.cancelNotification(key)
        }

        fun dismissAll() {
            instanceRef?.get()?.cancelAllNotifications()
        }
    }
}
