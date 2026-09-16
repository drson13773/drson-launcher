package com.drson.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.drson.launcher.music.PureMusicActivity
import com.drson.launcher.ui.DrivingRoadBackground
import com.drson.launcher.ui.HomeViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            val speed by viewModel.currentSpeed

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // 1. Nền đường, xe chạy và hiệu ứng tim đường/cảnh chuyển động theo tốc độ
                DrivingRoadBackground(
                    speedKmH = speed,
                    modifier = Modifier.fillMaxSize()
                )

                // 2. Logo thương hiệu góc trên trái & Đồng hồ thời gian font mềm mại
                TopBrandAndClock(
                    onLogoClick = {
                        openMusicApp()
                    },
                    modifier = Modifier.padding(start = 24.dp, top = 20.dp)
                )

                // 3. Đồng hồ đo tốc độ (Speedometer) ở góc trên phải hoặc trung tâm
                SpeedometerView(
                    speedKmH = speed,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 20.dp, end = 24.dp)
                )

                // 4. Thanh Dock ứng dụng ở cạnh dưới (bao gồm Dr Sơn Music)
                AppDockBar(
                    onAppClick = { packageName ->
                        if (packageName == "drson_music") {
                            openMusicApp()
                        } else {
                            // Mở các ứng dụng hệ thống khác nếu có
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                )
            }
        }
    }

    private fun openMusicApp() {
        try {
            startActivity(
                Intent(this, PureMusicActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun TopBrandAndClock(
    onLogoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var timeStr by remember { mutableStateOf("00:00") }
    var dateStr by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val tFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dFormat = SimpleDateFormat("EEEE, dd 'thg' M", Locale("vi", "VN"))
        while (true) {
            try {
                val now = Calendar.getInstance().time
                timeStr = tFormat.format(now)
                dateStr = dFormat.format(now)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(1000)
        }
    }

    Row(
        modifier = modifier.clickable { onLogoClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo gốc chuẩn xác (có sẵn chữ & dao mổ)
        Image(
            painter = painterResource(id = R.drawable.icon_menu_brand),
            contentDescription = "Brand Logo",
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Đồng hồ thời gian font mềm mại, thanh thoát
        Column {
            Text(
                text = timeStr,
                color = Color(0xFFFFEEB2),
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.5.sp
            )
            Text(
                text = dateStr,
                color = Color(0xFFD4AF37).copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Light
            )
        }
    }
}

@Composable
fun SpeedometerView(
    speedKmH: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${speedKmH.toInt()}",
            color = Color(0xFFFFD700),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "KM/H",
            color = Color(0xFFD4AF37),
            fontSize = 10.sp,
            fontWeight = FontWeight.Light
        )
    }
}

@Composable
fun AppDockBar(
    onAppClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Biểu tượng ứng dụng nghe nhạc Dr Sơn Music trong Dock
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { onAppClick("drson_music") },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.icon_menu_brand), // Hoặc icon riêng của nhạc
                contentDescription = "Dr Sơn Music",
                modifier = Modifier.size(40.dp)
            )
        }
        
        // Bạn có thể bổ sung thêm các icon ứng dụng khác vào đây nếu muốn
    }
}
