package com.drson.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.drson.launcher.ui.HomeScreen
import com.drson.launcher.ui.HomeViewModel
import com.drson.launcher.widget.WidgetHostController

class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                HomeScreen(viewModel = viewModel)
            }
        }
    }

    // Bắt buộc để widget trên Home Screen (đồng hồ, nhạc, thời tiết...) nhận cập nhật khi app
    // đang hiển thị, và dừng lại khi app bị che để đỡ tốn tài nguyên - xem WidgetHostController.
    override fun onStart() {
        super.onStart()
        WidgetHostController.startListening(this)
    }

    override fun onStop() {
        WidgetHostController.stopListening(this)
        super.onStop()
    }

    // Launcher là màn hình gốc - không có gì để "back" về.
    override fun onBackPressed() {
        // no-op
    }
}
