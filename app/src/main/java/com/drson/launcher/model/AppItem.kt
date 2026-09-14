package com.drson.launcher.model

import androidx.compose.ui.graphics.ImageBitmap

/**
 * [activityClassName] lưu đúng tên Activity đã được `queryIntentActivities` phân giải ra cho app
 * này - dùng để mở TRỰC TIẾP đúng màn hình đó (`ComponentName(packageName, activityClassName)`)
 * thay vì chỉ dựa vào packageName. Cần thiết cho trường hợp 1 packageName có NHIỀU activity cùng
 * khai báo category LAUNCHER (như app Điện thoại riêng của launcher này - `DialerActivity` - cùng
 * packageName với `MainActivity` của chính launcher) - dựa packageName không thôi Android có thể
 * mở nhầm activity.
 */
data class AppItem(
    val label: String,
    val packageName: String,
    val activityClassName: String,
    val icon: ImageBitmap,
)
