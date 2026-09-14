@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BottomDock(
    viewModel: HomeViewModel,
    isEditMode: Boolean,
    onSwipeUpToOpenAppSwitcher: () -> Unit,
    onOpenMenu: () -> Unit,
    onEmptyDockSlotTap: (Int) -> Unit,
    onLongPressSlot: () -> Unit,
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -6f) onSwipeUpToOpenAppSwitcher()
                }
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // NÚT MỞ APP DRAWER: Giữ nguyên hình ảnh gốc, không bị mất hình
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1912))
                .border(
                    width = 1.dp,
                    color = GOLD_BRIGHT.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable(onClick = onOpenMenu)
                .padding(3.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.icon_menu_brand),
                contentDescription = "Menu ứng dụng",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        viewModel.dockSlots.forEachIndexed { index, packageName ->
            val app = viewModel.appFor(packageName)
            DockSlotCell(
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

        Spacer(Modifier.weight(1f))

        NowPlayingBar()
    }
}
