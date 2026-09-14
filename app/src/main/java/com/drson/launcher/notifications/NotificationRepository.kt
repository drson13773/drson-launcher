package com.drson.launcher.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * `NotificationListenerService` được hệ thống khởi động độc lập, không đi qua ViewModel/Activity
 * nào - nên cần 1 nơi trung gian đơn giản (in-memory, sống theo vòng đời process) để đẩy dữ liệu
 * thông báo sang Compose UI. Compose quan sát [notifications] qua `collectAsState()`.
 */
object NotificationRepository {
    private val _notifications = MutableStateFlow<List<NotificationEntry>>(emptyList())
    val notifications: StateFlow<List<NotificationEntry>> = _notifications

    fun upsert(entry: NotificationEntry) {
        val current = _notifications.value.filterNot { it.key == entry.key }
        _notifications.value = (listOf(entry) + current).sortedByDescending { it.postTime }
    }

    fun remove(key: String) {
        _notifications.value = _notifications.value.filterNot { it.key == key }
    }

    fun replaceAll(entries: List<NotificationEntry>) {
        _notifications.value = entries.sortedByDescending { it.postTime }
    }

    fun clear() {
        _notifications.value = emptyList()
    }
}
