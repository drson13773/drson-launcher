package com.drson.launcher.notifications

import android.app.PendingIntent
import android.graphics.drawable.Drawable

data class NotificationEntry(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val icon: Drawable?,
    val title: String,
    val text: String,
    val postTime: Long,
    val contentIntent: PendingIntent?,
)
