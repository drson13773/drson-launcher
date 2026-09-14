package com.drson.launcher.model

/**
 * Nội dung của 1 ô trên lưới Home Screen: có thể là icon ứng dụng (như trước giờ), hoặc 1 widget
 * Android thật (đồng hồ, thời tiết, nhạc... của bất kỳ app nào có cung cấp widget) - ô nào không
 * gán gì thì để `null` (ô trống, hiện dấu "+").
 *
 * Được lưu xuống DataStore dưới dạng 1 chuỗi đơn giản qua [encode]/[decode] để không cần thêm thư
 * viện JSON - xem `data/HomeLayoutRepository.kt`.
 */
sealed class HomeSlotContent {
    data class App(val packageName: String) : HomeSlotContent()

    /** [provider] là tên component dạng "package/package.WidgetProvider" (AppWidgetProviderInfo.provider.flattenToString()). */
    data class Widget(val appWidgetId: Int, val provider: String) : HomeSlotContent()

    fun encode(): String = when (this) {
        is App -> "app:$packageName"
        is Widget -> "widget:$appWidgetId:$provider"
    }

    companion object {
        fun decode(raw: String?): HomeSlotContent? {
            if (raw.isNullOrBlank()) return null
            val parts = raw.split(":", limit = 3)
            return when (parts.getOrNull(0)) {
                "app" -> parts.getOrNull(1)?.let { App(it) }
                "widget" -> {
                    val id = parts.getOrNull(1)?.toIntOrNull() ?: return null
                    val provider = parts.getOrNull(2) ?: return null
                    Widget(id, provider)
                }
                // Dữ liệu cũ (trước khi có widget) chỉ lưu thẳng packageName, không có tiền tố.
                else -> App(raw)
            }
        }
    }
}
