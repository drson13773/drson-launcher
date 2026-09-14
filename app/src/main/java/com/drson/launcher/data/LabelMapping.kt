package com.drson.launcher.data

/**
 * Tên hiển thị tiếng Việt có dấu cho các app phổ biến - thay cho nhãn gốc của app (thường là
 * tiếng Anh hoặc tên thương hiệu). App nào không có trong danh sách vẫn hiện đúng tên gốc của nó.
 */
object LabelMapping {
    val packageToLabel: Map<String, String> = mapOf(
        // Điện thoại / Dialer (đã đặt "Điện thoại" trực tiếp trong Manifest cho app Dialer riêng)
        "com.android.dialer" to "Điện thoại",
        "com.google.android.dialer" to "Điện thoại",
        "com.samsung.android.dialer" to "Điện thoại",
        // Danh bạ
        "com.android.contacts" to "Danh bạ",
        "com.google.android.contacts" to "Danh bạ",
        "com.samsung.android.app.contacts" to "Danh bạ",
        // Tin nhắn
        "com.android.mms" to "Tin nhắn",
        "com.google.android.apps.messaging" to "Tin nhắn",
        "com.samsung.android.messaging" to "Tin nhắn",
        // Camera
        "com.android.camera" to "Máy ảnh",
        "com.android.camera2" to "Máy ảnh",
        "com.google.android.GoogleCamera" to "Máy ảnh",
        "com.sec.android.app.camera" to "Máy ảnh",
        // Thư viện ảnh
        "com.android.gallery3d" to "Thư viện ảnh",
        "com.google.android.apps.photos" to "Thư viện ảnh",
        "com.sec.android.gallery3d" to "Thư viện ảnh",
        // Nhạc
        "com.android.music" to "Nhạc",
        "com.google.android.music" to "Nhạc",
        "com.google.android.apps.youtube.music" to "Nhạc",
        "com.spotify.music" to "Nhạc",
        "com.zing.mp3" to "Nhạc",
        // Bản đồ
        "com.google.android.apps.maps" to "Bản đồ",
        // Cài đặt
        "com.android.settings" to "Cài đặt",
        // Trình duyệt
        "com.android.browser" to "Trình duyệt",
        "com.android.chrome" to "Trình duyệt",
        "com.google.android.apps.chrome" to "Trình duyệt",
        "org.mozilla.firefox" to "Trình duyệt",
        // Vài app phổ biến khác
        "com.google.android.gm" to "Thư điện tử",
        "com.google.android.youtube" to "YouTube",
        "com.facebook.katana" to "Facebook",
        "com.zing.zalo" to "Zalo",
    )
}
