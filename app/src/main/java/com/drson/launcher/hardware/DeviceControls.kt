package com.drson.launcher.hardware

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Các hàm điều khiển phần cứng CHUẨN của Android (âm lượng, độ sáng, Wi-Fi, Bluetooth, máy bay,
 * data di động).
 *
 * LƯU Ý QUAN TRỌNG: từ Android 10 trở lên, Google chặn app THƯỜNG tự bật/tắt trực tiếp Wi-Fi,
 * chế độ máy bay và Data di động bằng code - đây là giới hạn của nền tảng, không phải do app này
 * viết thiếu. Cách duy nhất để 1 app thường (không phải app hệ thống) làm được việc này là được
 * cấp thêm quyền hệ thống (`NETWORK_SETTINGS`, `WRITE_SECURE_SETTINGS`, `MODIFY_PHONE_STATE`) qua
 * lệnh ADB **một lần duy nhất** khi cài lên đầu màn hình (đầu màn hình ô tô thường cho bật gỡ lỗi
 * USB, nên việc này khả dụng). Xem hướng dẫn cấp quyền ở cuối README.
 *
 * Nếu CHƯA cấp quyền ADB, các hàm bên dưới sẽ tự động rơi về mở đúng màn hình Cài đặt tương ứng
 * để người dùng bật/tắt trong 1 chạm, y như trước - tức là app KHÔNG BAO GIỜ crash vì thiếu quyền.
 */
object DeviceControls {

    // ---------- Âm lượng ----------
    fun getVolumeFraction(context: Context): Float {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        return if (max == 0) 0f else cur.toFloat() / max
    }

    fun setVolumeFraction(context: Context, fraction: Float) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (fraction.coerceIn(0f, 1f) * max).toInt()
        am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
    }

    // ---------- Độ sáng ----------
    fun canWriteSystemSettings(context: Context): Boolean = Settings.System.canWrite(context)

    /** Mở màn hình hệ thống để người dùng cấp quyền "Sửa đổi cài đặt hệ thống" (chỉ cần cấp 1 lần). */
    fun requestWriteSettingsPermission(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun getBrightnessFraction(context: Context): Float = try {
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
    } catch (e: Settings.SettingNotFoundException) {
        0.5f
    }

    /** Cần [canWriteSystemSettings] = true trước khi gọi, nếu không hàm sẽ bỏ qua im lặng. */
    fun setBrightnessFraction(context: Context, fraction: Float) {
        if (!canWriteSystemSettings(context)) return
        val value = (fraction.coerceIn(0f, 1f) * 255).toInt()
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
    }

    private fun hasPermission(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    // ---------- Wi-Fi ----------
    /** Đọc trạng thái thật, không cần quyền đặc biệt (ACCESS_WIFI_STATE là quyền "normal"). */
    fun isWifiEnabled(context: Context): Boolean = try {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        wifiManager?.isWifiEnabled == true
    } catch (e: Exception) {
        false
    }

    /** Có quyền hệ thống NETWORK_SETTINGS (cấp qua ADB) để bật/tắt Wi-Fi trực tiếp hay chưa. */
    fun hasNetworkSettingsPermission(context: Context): Boolean =
        hasPermission(context, "android.permission.NETWORK_SETTINGS")

    /**
     * Bật/tắt Wi-Fi. Thử trực tiếp bằng `WifiManager.setWifiEnabled` trước (chỉ có tác dụng nếu
     * app đã được cấp quyền `NETWORK_SETTINGS` qua ADB); nếu không thành công thì mở panel Wi-Fi
     * hệ thống để người dùng bật/tắt trong 1 chạm.
     */
    fun toggleWifi(context: Context) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val target = !(wifiManager?.isWifiEnabled ?: false)
        val handled = try {
            @Suppress("DEPRECATION")
            wifiManager?.setWifiEnabled(target) == true
        } catch (e: SecurityException) {
            false
        }
        if (!handled) openWifiPanel(context)
    }

    /** Mở panel Wi-Fi hệ thống - dùng khi chưa có quyền NETWORK_SETTINGS. */
    fun openWifiPanel(context: Context) {
        val intent = Intent(Settings.Panel.ACTION_WIFI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    // ---------- Bluetooth ----------
    fun isBluetoothEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter?.isEnabled == true
    }

    /** Trên Android 12+ cần quyền runtime BLUETOOTH_CONNECT; trước đó luôn khả dụng. */
    fun hasBluetoothConnectPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return hasPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT)
    }

    /**
     * Bật/tắt Bluetooth trực tiếp. Gọi hàm này SAU KHI đã chắc chắn có quyền
     * [hasBluetoothConnectPermission] (ở Control Center, quyền được xin qua hộp thoại hệ thống
     * ngay khi người dùng chạm nút lần đầu - xem `ControlCenterOverlay.kt`).
     */
    @Suppress("MissingPermission")
    fun toggleBluetooth(context: Context) {
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        if (adapter != null && hasBluetoothConnectPermission(context)) {
            if (adapter.isEnabled) adapter.disable() else adapter.enable()
        } else {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    // ---------- Chế độ máy bay ----------
    /** Đọc được trạng thái thật, không cần quyền đặc biệt. */
    fun isAirplaneModeOn(context: Context): Boolean =
        Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0

    /** Có quyền hệ thống WRITE_SECURE_SETTINGS (cấp qua ADB) để bật/tắt máy bay trực tiếp hay chưa. */
    fun hasWriteSecureSettingsPermission(context: Context): Boolean =
        hasPermission(context, android.Manifest.permission.WRITE_SECURE_SETTINGS)

    /**
     * Bật/tắt chế độ máy bay. Thử ghi trực tiếp vào `Settings.Global` + phát broadcast chuẩn của
     * hệ thống (cách này hoạt động thật trên mọi bản Android khi app có quyền WRITE_SECURE_SETTINGS
     * - quyền này chỉ có thể cấp qua ADB, không có màn hình Cài đặt nào để cấp tay). Nếu chưa có
     * quyền, mở màn hình Cài đặt máy bay để bật/tắt trong 1 chạm như trước.
     */
    fun toggleAirplaneMode(context: Context) {
        if (hasWriteSecureSettingsPermission(context)) {
            val target = !isAirplaneModeOn(context)
            Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, if (target) 1 else 0)
            val intent = Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED).apply {
                putExtra("state", target)
            }
            context.sendBroadcast(intent)
        } else {
            openAirplaneModeSettings(context)
        }
    }

    /** Mở màn hình Cài đặt máy bay - dùng khi chưa có quyền WRITE_SECURE_SETTINGS. */
    fun openAirplaneModeSettings(context: Context) {
        val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    // ---------- Data di động (4G) ----------
    /**
     * Không có API công khai để đọc trạng thái Data di động trên app thường; thử qua reflection
     * (hoạt động trên phần lớn thiết bị/bản Android nhưng không đảm bảo 100%). Nếu thất bại,
     * trả về null để UI tự giữ trạng thái theo lượt bấm gần nhất trong phiên (giống cách xử lý Wi-Fi).
     */
    fun isMobileDataEnabledOrNull(context: Context): Boolean? = try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
        val method = cm.javaClass.getDeclaredMethod("getMobileDataEnabled")
        method.isAccessible = true
        method.invoke(cm) as? Boolean
    } catch (e: Exception) {
        null
    }

    /** Có quyền hệ thống MODIFY_PHONE_STATE (cấp qua ADB) để bật/tắt Data trực tiếp hay chưa. */
    fun hasModifyPhoneStatePermission(context: Context): Boolean =
        hasPermission(context, android.Manifest.permission.MODIFY_PHONE_STATE)

    /**
     * Bật/tắt Data di động. Thử gọi thẳng hàm ẩn `TelephonyManager.setDataEnabled` qua reflection
     * (chỉ thành công nếu app có quyền MODIFY_PHONE_STATE cấp qua ADB, và tuỳ hãng đầu màn hình có
     * cho phép hay không). Nếu thất bại, mở màn hình Cài đặt mạng di động để bật/tắt trong 1 chạm.
     */
    fun toggleMobileData(context: Context) {
        val handled = if (hasModifyPhoneStatePermission(context)) {
            try {
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE)
                val current = isMobileDataEnabledOrNull(context) ?: true
                val method = tm.javaClass.getDeclaredMethod("setDataEnabled", Boolean::class.javaPrimitiveType)
                method.isAccessible = true
                method.invoke(tm, !current)
                true
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
        if (!handled) openMobileDataSettings(context)
    }

    /** Mở màn hình Cài đặt mạng di động (có công tắc Data) - cách khả dụng cho app thường. */
    fun openMobileDataSettings(context: Context) {
        val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
