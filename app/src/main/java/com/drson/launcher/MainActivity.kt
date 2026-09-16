package com.drson.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Bản test tối giản không gọi hàm phức tạp để kiểm tra khả năng render của BlueStacks
        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF14120E)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Zestech Launcher - Test OK!",
                    color = Color(0xFFD4AF37),
                    fontSize = 24.sp
                )
            }
        }
    }
}
