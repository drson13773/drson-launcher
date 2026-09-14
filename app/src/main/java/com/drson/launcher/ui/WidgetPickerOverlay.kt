package com.drson.launcher.ui

import android.appwidget.AppWidgetProviderInfo
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawable.toBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.drson.launcher.widget.WidgetHostController

private val GOLD_BRIGHT = Color(0xFFE6C178)

/**
 * Danh sách widget thật do các app đã cài trên máy cung cấp (đọc qua `AppWidgetManager`, API
 * chuẩn của Android) - chạm 1 widget để gán vào ô trống đang chọn trên Home Screen.
 */
@Composable
fun WidgetPickerOverlay(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onPick: (AppWidgetProviderInfo) -> Unit,
) {
    val context = LocalContext.current
    val providers = remember(isOpen) { if (isOpen) WidgetHostController.installedProviders(context) else emptyList() }

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(150)),
        modifier = Modifier.zIndex(40f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable(onClick = onDismiss),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp)
                    .clickable(enabled = false) {},
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Chọn widget cho ô này", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Đóng",
                        color = GOLD_BRIGHT,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                if (providers.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "Máy chưa cài app nào có widget.\nCài thêm app (nhạc, thời tiết...) rồi quay lại đây.",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    ) {
                        items(providers, key = { it.provider.flattenToString() }) { info ->
                            WidgetProviderCell(info = info, onClick = { onPick(info) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetProviderCell(info: AppWidgetProviderInfo, onClick: () -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager
    val label = try { info.loadLabel(pm) } catch (e: Exception) { info.provider.packageName }
    val appLabel = try {
        pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        info.provider.packageName
    }
    val icon = remember(info) {
        try { info.loadIcon(context, 0)?.toBitmap()?.asImageBitmap() } catch (e: Exception) { null }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = label,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.1f)),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = Color.White, fontSize = 12.sp, maxLines = 1, textAlign = TextAlign.Center)
        Text(appLabel, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, maxLines = 1, textAlign = TextAlign.Center)
    }
}
