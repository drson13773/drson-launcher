package com.drson.launcher.ui.dialer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BAT_DARK = Color(0xFF0A0907)
private val BAT_CARBON = Color(0xFF161410)
private val GOLD_HUD = Color(0xFFFFD700)
private val GOLD_BRIGHT = Color(0xFFFFF0B8)
private val GOLD_DARK = Color(0xFF8B7500)

class DialerActivity : ComponentActivity() {

    private var dialedNumber by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BatmobileDialerScreen(
                dialedNumber = dialedNumber,
                onDigitPress = { digit ->
                    if (dialedNumber.length < 15) dialedNumber += digit
                },
                onBackspace = {
                    if (dialedNumber.isNotEmpty()) dialedNumber = dialedNumber.dropLast(1)
                },
                onClearAll = { dialedNumber = "" },
                onCall = {
                    if (dialedNumber.isNotBlank()) {
                        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$dialedNumber")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            startActivity(callIntent)
                        } catch (_: Exception) {
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$dialedNumber")).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(dialIntent)
                        }
                    }
                },
                onBack = { finish() }
            )
        }
    }
}

@Composable
fun BatmobileDialerScreen(
    dialedNumber: String,
    onDigitPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onClearAll: () -> Unit,
    onCall: () -> Unit,
    onBack: () -> Unit
) {
    val keypadKeys = listOf(
        listOf("1" to "", "2" to "ABC", "3" to "DEF"),
        listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
        listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
        listOf("*" to "", "0" to "+", "#" to "")
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(BAT_DARK)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // CỘT TRÁI: Màn hình HUD buồng lái siêu xe hiển thị số gọi
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Nút Thoát
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CutCornerShape(8.dp))
                    .background(BAT_CARBON)
                    .border(1.dp, GOLD_DARK, CutCornerShape(8.dp))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GOLD_BRIGHT)
            }

            // Màn hình hiển thị số kiểu HUD Cockpit
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp)
                    .clip(CutCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF14120D), Color(0xFF080705)))
                    )
                    .border(1.5.dp, GOLD_HUD.copy(alpha = 0.6f), CutCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "MAZDA COCKPIT DIALER",
                        color = GOLD_DARK,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = if (dialedNumber.isEmpty()) "NHẬP SỐ ĐIỆN THOẠI" else dialedNumber,
                        color = if (dialedNumber.isEmpty()) Color.Gray.copy(alpha = 0.5f) else GOLD_BRIGHT,
                        fontSize = if (dialedNumber.length > 10) 24.sp else 30.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Nút Gọi khẩn cấp / Gọi điện siêu xe
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(CutCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFFB8860B), Color(0xFFFFD700)))
                    )
                    .border(2.dp, GOLD_BRIGHT, CutCornerShape(12.dp))
                    .clickable(onClick = onCall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Call, contentDescription = "Call", tint = Color.Black, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "BẮT ĐẦU CUỘC GỌI",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        // CỘT PHẢI: Bàn phím số Batman / Supercar góc cạnh
        Column(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            for (row in keypadKeys) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for ((digit, subText) in row) {
                        BatKeypadButton(
                            digit = digit,
                            subText = subText,
                            modifier = Modifier.weight(1f),
                            onClick = { onDigitPress(digit) }
                        )
                    }
                }
            }

            // Dòng phím chức năng Xóa lùi / Xóa sạch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onClearAll,
                    shape = CutCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BAT_CARBON),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CutCornerShape(10.dp))
                ) {
                    Text("XÓA HẾT", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onBackspace,
                    shape = CutCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BAT_CARBON),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .border(1.dp, GOLD_DARK, CutCornerShape(10.dp))
                ) {
                    Icon(Icons.Default.Backspace, contentDescription = "Backspace", tint = GOLD_BRIGHT, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun BatKeypadButton(
    digit: String,
    subText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(CutCornerShape(10.dp))
            .background(if (isFocused) Color(0xFF2E2619) else BAT_CARBON)
            .focusable(interactionSource = interactionSource)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) GOLD_BRIGHT else GOLD_DARK.copy(alpha = 0.4f),
                shape = CutCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = digit,
                color = if (isFocused) GOLD_BRIGHT else Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            if (subText.isNotEmpty()) {
                Text(
                    text = subText,
                    color = GOLD_HUD.copy(alpha = 0.7f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
