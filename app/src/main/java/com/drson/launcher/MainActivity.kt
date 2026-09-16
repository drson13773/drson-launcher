package com.drson.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

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

                // 2. Logo, chữ Dr. Sơn và đồng hồ thời gian font mềm mại góc trên trái
                TopBrandAndClock(
                    onLogoClick = {
                        startActivity(
                            Intent(this@MainActivity, PureMusicActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    modifier = Modifier.padding(start = 24.dp, top = 20.dp)
                )
            }
        }
    }
}

@Composable
fun TopBrandAndClock(
    onLogoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var timeStr by remember { mutableStateOf("") }
    var dateStr by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val tFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dFormat = SimpleDateFormat("EEEE, dd 'thg' M", Locale("vi", "VN"))
        while (true) {
            val now = Calendar.getInstance().time
            timeStr = tFormat.format(now)
            dateStr = dFormat.format(now)
            delay(1000)
        }
    }

    Row(
        modifier = modifier.clickable { onLogoClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cột chứa Logo và chữ Dr. Sơn bên dưới
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.icon_menu_brand),
                contentDescription = "Brand Logo",
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Dr. Sơn",
                color = Color(0xFFD4AF37),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Cột chứa đồng hồ thời gian với font mềm mại, thanh thoát
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
