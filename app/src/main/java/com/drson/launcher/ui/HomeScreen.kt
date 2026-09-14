package com.drson.launcher.ui

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.drson.launcher.R
import com.drson.launcher.data.HOME_GRID_COLUMNS
import com.drson.launcher.model.AppItem
import com.drson.launcher.model.HomeSlotContent
import com.drson.launcher.widget.WidgetHostController
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

private val GOLD_BRIGHT = Color(0xFFE6C178)

/** Ngữ cảnh khi mở App Drawer: chỉ để duyệt/mở app, hoặc để gán vào 1 ô cụ thể trên Home/Dock. */
private sealed class DrawerTarget {
    object Browse : DrawerTarget()
    data class AssignHome(val index: Int) : DrawerTarget()
    data class AssignDock(val index: Int) : DrawerTarget()
}

/** Widget đang trong quá trình thêm (chờ người dùng đồng ý bind / cấu hình xong). */
private data class PendingWidget(val targetIndex: Int, val appWidgetId: Int, val info: AppWidgetProviderInfo)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.load(context) }

    var isControlCenterOpen by remember { mutableStateOf(false) }
    var isNotificationCenterOpen by remember { mutableStateOf(false) }
    var isAppSwitcherOpen by remember { mutableStateOf(false) }
    var drawerTarget by remember { mutableStateOf<DrawerTarget?>(null) }
    var slotChooserIndex by remember { mutableStateOf<Int?>(null) }
    var widgetTargetIndex by remember { mutableStateOf<Int?>(null) }
    var pendingWidget by remember { mutableStateOf<PendingWidget?>(null) }

    // ---------- Luồng thêm widget thật (AppWidgetHost chuẩn của Android) ----------
    // 2 bước có thể cần người dùng xác nhận qua màn hình hệ thống: (1) đồng ý cho phép thêm widget
    // - chỉ hỏi nếu bindAppWidgetIdIfAllowed() không tự thành công, và (2) màn hình cấu hình riêng
    // của widget đó (vd chọn thành phố cho widget thời tiết) - chỉ có ở 1 số widget.
    val configureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val pending = pendingWidget
        pendingWidget = null
        if (pending != null) {
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.setHomeSlot(
                    context, pending.targetIndex,
                    HomeSlotContent.Widget(pending.appWidgetId, pending.info.provider.flattenToString()),
                )
            } else {
                WidgetHostController.deleteId(context, pending.appWidgetId)
            }
        }
    }

    fun finishAfterBind(pending: PendingWidget) {
        val configureComponent = pending.info.configure
        if (configureComponent != null) {
            pendingWidget = pending
            try {
                configureLauncher.launch(
                    Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                        component = configureComponent
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pending.appWidgetId)
                    },
                )
            } catch (e: Exception) {
                // Không mở được màn hình cấu hình riêng - vẫn thêm widget bình thường.
                pendingWidget = null
                viewModel.setHomeSlot(
                    context, pending.targetIndex,
                    HomeSlotContent.Widget(pending.appWidgetId, pending.info.provider.flattenToString()),
                )
            }
        } else {
            viewModel.setHomeSlot(
                context, pending.targetIndex,
                HomeSlotContent.Widget(pending.appWidgetId, pending.info.provider.flattenToString()),
            )
        }
    }

    val bindLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val pending = pendingWidget
        if (pending != null) {
            if (result.resultCode == Activity.RESULT_OK) {
                finishAfterBind(pending)
            } else {
                pendingWidget = null
                WidgetHostController.deleteId(context, pending.appWidgetId)
            }
        }
    }

    fun startAddWidget(targetIndex: Int, info: AppWidgetProviderInfo) {
        val id = WidgetHostController.allocateId(context)
        val pending = PendingWidget(targetIndex, id, info)
        if (WidgetHostController.bindIfAllowed(context, id, info)) {
            finishAfterBind(pending)
        } else {
            pendingWidget = pending
            bindLauncher.launch(
                Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                },
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DrivingRoadBackground(modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            StatusBar(
                onSwipeDownLeft = { isNotificationCenterOpen = true },
                onSwipeDownRight = { isControlCenterOpen = true },
            )

            SlotGrid(
                viewModel = viewModel,
                onEmptySlotTap = { index -> slotChooserIndex = index },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )

            BottomDock(
                viewModel = viewModel,
                onSwipeUpToOpenAppSwitcher = { isAppSwitcherOpen = true },
                onOpenMenu = { drawerTarget = DrawerTarget.Browse },
                onEmptyDockSlotTap = { index -> drawerTarget = DrawerTarget.AssignDock(index) },
            )
        }

        ControlCenterOverlay(
            isOpen = isControlCenterOpen,
            onDismiss = { isControlCenterOpen = false },
        )

        NotificationCenterOverlay(
            isOpen = isNotificationCenterOpen,
            onDismiss = { isNotificationCenterOpen = false },
        )

        AppSwitcherOverlay(
            isOpen = isAppSwitcherOpen,
            recentApps = viewModel.recentApps,
            onDismiss = { isAppSwitcherOpen = false },
            onOpenApp = { app ->
                viewModel.launchApp(context, app)
                isAppSwitcherOpen = false
            },
            onRemoveFromRecents = { app -> viewModel.removeFromRecents(app) },
        )

        AppDrawerOverlay(
            isOpen = drawerTarget != null,
            apps = viewModel.apps,
            title = when (drawerTarget) {
                is DrawerTarget.AssignHome, is DrawerTarget.AssignDock -> "Chọn ứng dụng cho ô này"
                else -> "Tất cả ứng dụng"
            },
            onDismiss = { drawerTarget = null },
            onPick = { app ->
                when (val target = drawerTarget) {
                    is DrawerTarget.AssignHome -> viewModel.setHomeSlot(context, target.index, HomeSlotContent.App(app.packageName))
                    is DrawerTarget.AssignDock -> viewModel.setDockSlot(context, target.index, app.packageName)
                    else -> viewModel.launchApp(context, app)
                }
                drawerTarget = null
            },
        )

        WidgetPickerOverlay(
            isOpen = widgetTargetIndex != null,
            onDismiss = { widgetTargetIndex = null },
            onPick = { info ->
                widgetTargetIndex?.let { index -> startAddWidget(index, info) }
                widgetTargetIndex = null
            },
        )

        if (slotChooserIndex != null) {
            val index = slotChooserIndex!!
            SlotTypeChooserDialog(
                onDismiss = { slotChooserIndex = null },
                onPickApp = {
                    drawerTarget = DrawerTarget.AssignHome(index)
                    slotChooserIndex = null
                },
                onPickWidget = {
                    widgetTargetIndex = index
                    slotChooserIndex = null
                },
            )
        }
    }
}

@Composable
private fun StatusBar(
    onSwipeDownLeft: () -> Unit,
    onSwipeDownRight: () -> Unit,
) {
    var time by remember { mutableStateOf(currentTimeString()) }
    var date by remember { mutableStateOf(currentDateString()) }
    LaunchedEffect(Unit) {
        while (true) {
            time = currentTimeString()
            date = currentDateString()
            delay(1000)
        }
    }

    var barWidthPx by remember { mutableStateOf(0f) }
    var dragStartX by remember { mutableStateOf(0f) }
    var triggered by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { barWidthPx = it.size.width.toFloat() }
            .padding(horizontal = 20.dp, vertical = 10.dp)
            // Vuốt xuống từ NỬA TRÁI mở Thông báo, NỬA PHẢI mở Control Center.
            .pointerInputStatusBar(
                onDragStartX = { dragStartX = it; triggered = false },
                onDrag = { dragAmount ->
                    if (!triggered && dragAmount > 6f) {
                        triggered = true
                        if (dragStartX < barWidthPx / 2f) onSwipeDownLeft() else onSwipeDownRight()
                    }
                },
                onEnd = { triggered = false },
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(time, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Medium)
            Text(date, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Wi-Fi", color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp)
            Text("BT", color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp)
        }
    }
}

private fun Modifier.pointerInputStatusBar(
    onDragStartX: (Float) -> Unit,
    onDrag: (Float) -> Unit,
    onEnd: () -> Unit,
): Modifier = this.then(
    Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { offset -> onDragStartX(offset.x) },
            onDragEnd = onEnd,
            onDragCancel = onEnd,
            onDrag = { _, dragAmount -> onDrag(dragAmount.y) },
        )
    }
)

private fun currentTimeString(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

/** Luôn hiển thị ngày tháng bằng tiếng Việt có dấu (vd: "Thứ Hai, 14 tháng 9"), bất kể ngôn ngữ
 * hệ thống của đầu màn hình đang đặt là gì. */
private fun currentDateString(): String {
    val vietnamese = Locale("vi", "VN")
    return SimpleDateFormat("EEEE, d 'tháng' M", vietnamese)
        .format(Date())
        .replaceFirstChar { it.titlecase(vietnamese) }
}

/** Số cột bên trái được CHỪA TRỐNG cho cụm đồng hồ giờ + đồng hồ tốc độ luôn hiển thị trên nền
 * Driving (xem `DrivingRoadBackground.kt`) - không gán app/widget vào đây để icon khỏi đè lên. */
private const val HOME_GRID_RESERVED_COLUMNS = 3

/**
 * Lưới ô phủ kín TOÀN BỘ màn hình nền (không cuộn): số hàng x số cột được chia đều theo cả
 * chiều ngang lẫn chiều dọc bằng `weight`, nên dù màn hình xe to/nhỏ/tỉ lệ khác nhau, lưới vẫn
 * luôn giãn ra lấp đầy đúng phần nền đang trống, mỗi ô cách đều nhau. Mỗi ô có thể chứa icon ứng
 * dụng HOẶC 1 widget thật (xem `HomeSlotContent`) - ô trống chạm vào sẽ hỏi muốn thêm loại nào.
 * `HOME_GRID_RESERVED_COLUMNS` cột đầu tiên (bên trái) luôn để trống, dành chỗ cho cụm đồng hồ.
 */
@Composable
private fun SlotGrid(viewModel: HomeViewModel, onEmptySlotTap: (Int) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val columns = HOME_GRID_COLUMNS
    val rows = (viewModel.homeSlots.size + columns - 1) / columns

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        repeat(rows) { rowIndex ->
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                repeat(columns) { colIndex ->
                    val index = rowIndex * columns + colIndex
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight().padding(4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (colIndex >= HOME_GRID_RESERVED_COLUMNS && index < viewModel.homeSlots.size) {
                            when (val content = viewModel.homeSlots[index]) {
                                is HomeSlotContent.Widget -> WidgetSlotCell(
                                    content = content,
                                    onRemove = { viewModel.setHomeSlot(context, index, null) },
                                )
                                is HomeSlotContent.App -> {
                                    val app = viewModel.appFor(content.packageName)
                                    if (app != null) {
                                        SlotCell(
                                            app = app,
                                            onTap = { viewModel.launchApp(context, app) },
                                            onLongPress = { viewModel.setHomeSlot(context, index, null) },
                                        )
                                    } else {
                                        // App đã bị gỡ cài đặt khỏi máy - hiện như ô trống để gán lại.
                                        SlotCell(
                                            app = null,
                                            onTap = { onEmptySlotTap(index) },
                                            onLongPress = { viewModel.setHomeSlot(context, index, null) },
                                        )
                                    }
                                }
                                null -> SlotCell(
                                    app = null,
                                    onTap = { onEmptySlotTap(index) },
                                    onLongPress = {},
                                )
                            }
                        }
                        // Cột bị chừa trống (colIndex < HOME_GRID_RESERVED_COLUMNS): không vẽ gì cả,
                        // để lộ nguyên vẹn cụm đồng hồ/tốc độ ở lớp nền phía sau.
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SlotCell(app: AppItem?, onTap: () -> Unit, onLongPress: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.combinedClickable(onClick = onTap, onLongClick = onLongPress),
    ) {
        if (app != null) {
            Image(
                bitmap = app.icon,
                contentDescription = app.label,
                modifier = Modifier.size(84.dp).clip(RoundedCornerShape(20.dp)),
            )
            Spacer(Modifier.height(6.dp))
            Text(app.label, color = Color.White, fontSize = 13.sp, maxLines = 1, textAlign = TextAlign.Center)
        } else {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("+", color = Color.White.copy(alpha = 0.35f), fontSize = 30.sp, fontWeight = FontWeight.Light)
            }
            Spacer(Modifier.height(6.dp))
            Text("Trống", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
        }
    }
}

/**
 * Ô chứa 1 widget thật (dùng `AppWidgetHostView` chuẩn của Android qua `AndroidView` để nhúng vào
 * Compose) - lấp kín cả ô, không thu nhỏ như icon app. Vì widget tự xử lý thao tác chạm bên trong
 * nó (cuộn, nút bấm...) nên chạm-giữ để gỡ như ô app thường sẽ bị chính widget "nuốt" mất - thay
 * vào đó có sẵn nút "×" nhỏ ở góc để gỡ widget khỏi ô.
 */
@Composable
private fun WidgetSlotCell(content: HomeSlotContent.Widget, onRemove: () -> Unit) {
    val context = LocalContext.current
    val info = remember(content.provider) { WidgetHostController.providerInfoFor(context, content.provider) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (info != null) {
            AndroidView(
                factory = { ctx -> WidgetHostController.createHostView(ctx, content.appWidgetId, info) },
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)),
            )
        } else {
            // App cung cấp widget đã bị gỡ cài đặt khỏi máy.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Widget không khả dụng",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(22.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Text("×", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Hỏi người dùng muốn gán ứng dụng hay widget vào 1 ô trống trên Home Screen. */
@Composable
private fun SlotTypeChooserDialog(
    onDismiss: () -> Unit,
    onPickApp: () -> Unit,
    onPickWidget: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF141210))
                .clickable(enabled = false) {}
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Thêm gì vào ô này?", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SlotChooserOption(label = "Ứng dụng", onClick = onPickApp)
                SlotChooserOption(label = "Widget", onClick = onPickWidget)
            }
        }
    }
}

@Composable
private fun SlotChooserOption(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(140.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = GOLD_BRIGHT, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BottomDock(
    viewModel: HomeViewModel,
    onSwipeUpToOpenAppSwitcher: () -> Unit,
    onOpenMenu: () -> Unit,
    onEmptyDockSlotTap: (Int) -> Unit,
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -6f) onSwipeUpToOpenAppSwitcher()
                }
            },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon "Dr Sơn" - mở danh sách toàn bộ ứng dụng.
        Image(
            painter = painterResource(id = R.drawable.icon_menu_brand),
            contentDescription = "Menu ứng dụng",
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onOpenMenu),
        )

        viewModel.dockSlots.forEachIndexed { index, packageName ->
            val app = viewModel.appFor(packageName)
            DockSlotCell(
                app = app,
                onTap = {
                    if (app != null) viewModel.launchApp(context, app) else onEmptyDockSlotTap(index)
                },
                onLongPress = { if (app != null) viewModel.setDockSlot(context, index, null) },
            )
        }

        Spacer(Modifier.weight(1f))

        NowPlayingBar()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockSlotCell(app: AppItem?, onTap: () -> Unit, onLongPress: () -> Unit) {
    if (app != null) {
        Image(
            bitmap = app.icon,
            contentDescription = app.label,
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(onClick = onTap, onLongClick = onLongPress),
        )
    } else {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .combinedClickable(onClick = onTap, onLongClick = onLongPress),
            contentAlignment = Alignment.Center,
        ) {
            Text("+", color = Color.White.copy(alpha = 0.35f), fontSize = 22.sp, fontWeight = FontWeight.Light)
        }
    }
}

