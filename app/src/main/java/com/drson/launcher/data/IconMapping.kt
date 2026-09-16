package com.drson.launcher.data

import com.drson.launcher.R

object IconMapping {
    fun getIconRes(packageName: String): Int {
        return when {
            packageName.contains("phone") || packageName.contains("dialer") -> R.drawable.icon_phone_gold
            packageName.contains("music") || packageName.contains("zing") || packageName.contains("spotify") -> R.drawable.ic_drson_music
            packageName.contains("camera") || packageName.contains("cam360") || packageName.contains("dvr") -> R.drawable.icon_camera_gold
            packageName.contains("map") || packageName.contains("navigation") || packageName.contains("vietmap") -> R.drawable.icon_camera_gold
            else -> R.drawable.ic_launcher
        }
    }
}
