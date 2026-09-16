setContent {
    val speed by viewModel.currentSpeed

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Nền đường, xe chạy và mặt trăng góc trên phải
        DrivingRoadBackground(
            speedKmH = speed,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Logo thương hiệu và đồng hồ thời gian mềm mại góc trên trái (đã bỏ chữ "Đr Sơn" bên dưới)
        TopBrandAndClock(
            onLogoClick = {
                startActivity(
                    Intent(this@MainActivity, PureMusicActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
            modifier = Modifier.padding(start = 24.dp, top = 24.dp)
        )
        
        // Không còn nút bấm đổi hình nền ở góc trên bên phải nữa
    }
}
