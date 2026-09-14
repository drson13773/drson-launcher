package com.drson.launcher.data

import com.drson.launcher.R

/**
 * Ánh xạ packageName của các app phổ biến sang icon tuỳ chỉnh trong bộ "Dr Sơn".
 * App nào không có trong danh sách này sẽ tự động dùng icon thật của chính app đó
 * (xem HomeViewModel.loadInstalledApps) - không bị vỡ hình hay thiếu icon.
 *
 * Đầu màn hình mỗi hãng cài sẵn dialer/camera/gallery... với packageName khác nhau,
 * nên danh sách liệt kê nhiều biến thể phổ biến (AOSP, Google, Samsung...). Nếu đầu máy
 * của bạn dùng app hệ thống với packageName khác, chỉ cần thêm dòng tương ứng vào đây.
 */
object IconMapping {
    val packageToIcon: Map<String, Int> = mapOf(
        // Điện thoại / Dialer
        "com.android.dialer" to R.drawable.icon_phone,
        "com.google.android.dialer" to R.drawable.icon_phone,
        "com.samsung.android.dialer" to R.drawable.icon_phone,
        // Danh bạ
        "com.android.contacts" to R.drawable.icon_contacts,
        "com.google.android.contacts" to R.drawable.icon_contacts,
        "com.samsung.android.app.contacts" to R.drawable.icon_contacts,
        // Tin nhắn
        "com.android.mms" to R.drawable.icon_messages,
        "com.google.android.apps.messaging" to R.drawable.icon_messages,
        "com.samsung.android.messaging" to R.drawable.icon_messages,
        // Camera
        "com.android.camera" to R.drawable.icon_camera,
        "com.android.camera2" to R.drawable.icon_camera,
        "com.google.android.GoogleCamera" to R.drawable.icon_camera,
        "com.sec.android.app.camera" to R.drawable.icon_camera,
        // Thư viện ảnh
        "com.android.gallery3d" to R.drawable.icon_gallery,
        "com.google.android.apps.photos" to R.drawable.icon_gallery,
        "com.sec.android.gallery3d" to R.drawable.icon_gallery,
        // Nhạc
        "com.android.music" to R.drawable.icon_music,
        "com.google.android.music" to R.drawable.icon_music,
        "com.google.android.apps.youtube.music" to R.drawable.icon_music,
        "com.spotify.music" to R.drawable.icon_music,
        // Bản đồ
        "com.google.android.apps.maps" to R.drawable.icon_maps,
        // Cài đặt
        "com.android.settings" to R.drawable.icon_settings,
        // Trình duyệt
        "com.android.browser" to R.drawable.icon_browser,
        "com.android.chrome" to R.drawable.icon_browser,
        "com.google.android.apps.chrome" to R.drawable.icon_browser,
        "org.mozilla.firefox" to R.drawable.icon_browser,
    )
}
