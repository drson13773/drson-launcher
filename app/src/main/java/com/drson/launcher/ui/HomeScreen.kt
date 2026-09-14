package com.drson.launcher.ui

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drson.launcher.R
import com.drson.launcher.model.AppItem
import com.drson.launcher.model.HomeSlotContent
import java.lang.reflect.Method

private val GOLD_BRIGHT = Color(0xFFFFF0B8)
private val GOLD_ACCENT = Color(0xFFD4AF37)

// Hàm mở Bảng Thông Báo (Vuốt bên Trái)
@SuppressLint("WrongConstant")
fun openNotificationPanel(context: Context) {
    try {
        val statusBarService = context.getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val expandMethod: Method = statusBarManager.getMethod("expandNotificationsPanel")
        expandMethod.invoke(statusBarService)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// Hàm mở Trung Tâm Điều Khiển / Quick Settings (Vuốt bên Phải)
@SuppressLint("WrongConstant")
fun openQuickSettingsPanel(context: Context) {
    try {
        val statusBarService = context.getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val expandMethod: Method = try {
            statusBarManager.getMethod("expandSettingsPanel")
        } catch (_: Exception) {
            statusBarManager.getMethod("expandNotificationsPanel")
        }
        expandMethod.invoke(statusBarService)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isEditMode by remember { mutableStateOf(false) }
    var isAppDrawerOpen by remember { mutableStateOf(false) }
    val initialFocusRequester = remember { FocusRequester() }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }

    LaunchedEffect(Unit) {
        try {
            initialFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Nhận diện vuốt trái / phải độc lập
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        // Chỉ kích hoạt khi vuốt xuống một đoạn rõ ràng (> 30px)
                        if (dragAmount > 30f) {
                            val touchX = change.position.x
                            if (touchX < screenWidthPx / 2) {
                                // Nửa bên TRÁI -> Bảng thông báo
                                openNotificationPanel(context)
                            } else {
                                // Nửa bên PHẢI -> Trung tâm điều khiển
                                openQuickSettingsPanel(context)
                            }
                        }
                    }
                )
            }
    ) {
        DrivingRoadBackground()

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(10.dp))

            // Desktop Slots (Phía trên bên phải)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    val chunkedSlots = remember(viewModel.homeSlots.toList()) {
                        viewModel.homeSlots.chunked(3)
                    }
                    var isFirstSlot = true
                    for (rowSlots in chunkedSlots) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (slot in rowSlots) {
                                val pkgName = when (slot) {
                                    is HomeSlotContent.App -> slot.packageName
                                    else -> null
                                }
                                val app = viewModel.appFor(pkgName)
                                val currentModifier = if (isFirstSlot && app != null) {
                                    isFirstSlot = false
                                    Modifier.focusRequester(initialFocusRequester)
                                } else Modifier

                                FocusableDesktopSlot(
                                    app = app,
                                    modifier = currentModifier,
                                    onClick = {
                                        if (app != null) viewModel.launchApp(context, app)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Thanh Dock 1 Menu + 4 Slot + Trình phát nhạc mở rộng
            BottomDock(
                viewModel = viewModel,
                isEditMode = isEditMode,
                onOpenMenu = { isAppDrawerOpen = true },
                onEmptyDockSlotTap = { _ -> },
                onLongPressSlot = { isEditMode = !isEditMode }
            )
        }

        AppDrawerOverlay(
            isOpen = isAppDrawerOpen,
            apps = viewModel.apps,
            onDismiss = { isAppDrawerOpen = false },
            onPick = { app ->
                viewModel.launchApp(context, app)
                isAppDrawerOpen = false
            }
        )
    }
}

@Composable
private fun FocusableDesktopSlot(
    app: AppItem?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .size(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (app != null) Color.Black.copy(alpha = 0.45f) else Color.Transparent)
            .focusable(enabled = (app != null), interactionSource = interactionSource)
            .then(
                if (isFocused && app != null) {
                    Modifier.border(2.5.dp, GOLD_BRIGHT, RoundedCornerShape(16.dp))
                } else {
                    Modifier
                }
            )
            .clickable(enabled = (app != null), onClick = onClick)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (app != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    bitmap = app.icon,
                    contentDescription = app.label,
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = app.label,
                    color = Color.White,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun BottomDock(
    viewModel: HomeViewModel,
    isEditMode: Boolean,
    onOpenMenu: () -> Unit,
    onEmptyDockSlotTap: (Int) -> Unit,
    onLongPressSlot: () -> Unit,
) {
    val context = LocalContext.current
    val menuInteractionSource = remember { MutableInteractionSource() }
    val isMenuFocused by menuInteractionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Nút Menu chính
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1E1912))
                .focusable(enabled = true, interactionSource = menuInteractionSource)
                .then(
                    if (isMenuFocused) Modifier.border(2.dp, GOLD_BRIGHT, RoundedCornerShape(14.dp))
                    else Modifier.border(1.dp, GOLD_BRIGHT.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                )
                .clickable(onClick = onOpenMenu)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.icon_menu_brand),
                contentDescription = "Menu ứng dụng",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        // 4 ô Ứng dụng trên Dock
        viewModel.dockSlots.take(4).forEachIndexed { index, packageName ->
            val app = viewModel.appFor(packageName)
            FocusableDockSlotCell(
                app = app,
                isEditMode = isEditMode,
                onTap = {
                    if (app != null) viewModel.launchApp(context, app) else onEmptyDockSlotTap(index)
                },
                onLongPress = {
                    onLongPressSlot()
                    if (app != null) viewModel.setDockSlot(context, index, null)
                },
            )
        }

        Spacer(Modifier.width(8.dp))

        // Thanh phát nhạc & Âm lượng kéo dài
        ExpandedNowPlayingBar(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun FocusableDockSlotCell(
    app: AppItem?,
    isEditMode: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (app != null) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f))
            .focusable(enabled = (app != null), interactionSource = interactionSource)
            .then(
                if (isFocused && app != null) Modifier.border(2.dp, GOLD_BRIGHT, RoundedCornerShape(14.dp))
                else Modifier.border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
            )
            .clickable(onClick = onTap)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (app != null) {
            Image(
                bitmap = app.icon,
                contentDescription = app.label,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
            )
        }
    }
}

@Composable
private fun ExpandedNowPlayingBar(modifier: Modifier = Modifier) {
    var isPlaying by remember { mutableStateOf(false) }
    var trackProgress by remember { mutableFloatStateOf(0.35f) }
    var volumeLevel by remember { mutableFloatStateOf(0.7f) }

    Row(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF14120E))
            .border(1.dp, GOLD_ACCENT.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = GOLD_ACCENT, modifier = Modifier.size(20.dp))
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(GOLD_ACCENT)
                    .clickable { isPlaying = !isPlaying },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = GOLD_ACCENT, modifier = Modifier.size(20.dp))
            }
        }

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("01:25", color = Color.Gray, fontSize = 10.sp)
            Slider(
                value = trackProgress,
                onValueChange = { trackProgress = it },
                colors = SliderDefaults.colors(
                    thumbColor = GOLD_BRIGHT,
                    activeTrackColor = GOLD_ACCENT,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier.weight(1f).height(18.dp)
            )
            Text("04:10", color = Color.Gray, fontSize = 10.sp)
        }

        Row(
            modifier = Modifier.width(130.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Icons.Default.VolumeUp, contentDescription = "Volume", tint = GOLD_ACCENT, modifier = Modifier.size(18.dp))
            Slider(
                value = volumeLevel,
                onValueChange = { volumeLevel = it },
                colors = SliderDefaults.colors(
                    thumbColor = GOLD_BRIGHT,
                    activeTrackColor = GOLD_ACCENT,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier.weight(1f).height(18.dp)
            )
        }
    }
}
