package com.drson.launcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
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
                // 1. Nền đường và xe chạy
                DrivingRoadBackground(
                    speedKmH = speed,
                    modifier = Modifier.fillMaxSize()
                )

                // 2. Logo và đồng hồ thời gian góc trên trái
                TopBrandAndClock(
                    onLogoClick = {},
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp)
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
        Image(
            painter = painterResource(id = R.drawable.icon_menu_brand),
            contentDescription = "Brand Logo",
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = timeStr, color = Color(0xFFFFF0B8), fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(text = dateStr, color = Color(0xFFD4AF37), fontSize = 12.sp)
        }
    }
}
