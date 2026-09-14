package com.drson.launcher.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context

private const val HOST_ID = 1001

/**
 * Bọc `AppWidgetHost` CHUẨN của Android (API nền tảng có sẵn, không cần thư viện ngoài) để
 * launcher này "chứa" (host) được widget thật của bất kỳ app nào lên lưới Home Screen - đúng cơ
 * chế mà Nova Launcher, Apex, Lawnchair... vẫn dùng, không phải giả lập.
 *
 * Vì đây là app THƯỜNG (không phải app hệ thống), launcher không có sẵn quyền bind widget - phải
 * xin qua Intent `AppWidgetManager.ACTION_APPWIDGET_BIND` (Android tự hiện hộp thoại "Cho phép
 * ứng dụng thêm widget này?" cho người dùng đồng ý, xem `HomeScreen.kt` - hàm `addWidgetFlow`).
 * Đây là cách làm chuẩn, không cần khai báo quyền đặc biệt nào trong Manifest.
 */
object WidgetHostController {
    private var host: AppWidgetHost? = null

    private fun get(context: Context): AppWidgetHost =
        host ?: AppWidgetHost(context.applicationContext, HOST_ID).also { host = it }

    fun manager(context: Context): AppWidgetManager = AppWidgetManager.getInstance(context.applicationContext)

    /** Gọi ở Activity.onStart() - bắt buộc để widget nhận cập nhật (giờ, thời tiết...). */
    fun startListening(context: Context) {
        try { get(context).startListening() } catch (e: Exception) { /* bỏ qua nếu host lỗi vặt */ }
    }

    /** Gọi ở Activity.onStop() để tiết kiệm tài nguyên khi app không hiển thị. */
    fun stopListening(context: Context) {
        try { get(context).stopListening() } catch (e: Exception) { }
    }

    fun allocateId(context: Context): Int = get(context).allocateAppWidgetId()

    /** Giải phóng 1 widget id khi người dùng gỡ widget khỏi ô, hoặc khi huỷ giữa chừng lúc thêm. */
    fun deleteId(context: Context, appWidgetId: Int) {
        try { get(context).deleteAppWidgetId(appWidgetId) } catch (e: Exception) { }
    }

    /** Danh sách mọi widget mà các app đã cài trên máy cung cấp. */
    fun installedProviders(context: Context): List<AppWidgetProviderInfo> =
        try { manager(context).installedProviders } catch (e: Exception) { emptyList() }

    fun providerInfoFor(context: Context, componentFlattened: String): AppWidgetProviderInfo? =
        installedProviders(context).find { it.provider.flattenToString() == componentFlattened }

    /**
     * Thử cấp quyền bind trực tiếp (thành công với hầu hết widget đơn giản không cần đặc quyền).
     * Nếu trả về false, phải mở Intent ACTION_APPWIDGET_BIND cho người dùng đồng ý (xem HomeScreen.kt).
     */
    fun bindIfAllowed(context: Context, appWidgetId: Int, info: AppWidgetProviderInfo): Boolean = try {
        manager(context).bindAppWidgetIdIfAllowed(appWidgetId, info.provider)
    } catch (e: Exception) {
        false
    }

    fun createHostView(context: Context, appWidgetId: Int, info: AppWidgetProviderInfo): AppWidgetHostView =
        get(context).createView(context, appWidgetId, info)
}
