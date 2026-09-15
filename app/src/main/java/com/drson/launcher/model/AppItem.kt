package com.drson.launcher.model

import androidx.compose.ui.graphics.ImageBitmap

data class AppItem(
    val label: String,
    val packageName: String,
    val icon: ImageBitmap
)
