package com.drson.launcher.ui.dialer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drson.launcher.telephony.CallPlacer

private val GOLD = Color(0xFFC99E5C)
private val GOLD_BRIGHT = Color(0xFFE6C178)

private val KEYS = listOf(
    "1" to "", "2" to "ABC", "3" to "DEF",
    "4" to "GHI", "5" to "JKL", "6" to "MNO",
    "7" to "PQRS", "8" to "TUV", "9" to "WXYZ",
    "*" to "", "0" to "+", "#" to "",
)

@Composable
fun DialpadScreen() {
    val context = LocalContext.current
    var number by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth().weight(0.22f), contentAlignment = Alignment.Center) {
            Text(
                number.ifEmpty { "Nhap so dien thoai" },
                color = if (number.isEmpty()) Color.White.copy(alpha = 0.4f) else Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Column(
            modifier = Modifier.weight(0.62f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            KEYS.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { (digit, letters) ->
                        DialKey(
                            digit = digit,
                            letters = letters,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = { number += digit },
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.weight(0.16f).fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(GOLD)
                    .clickable(enabled = number.isNotEmpty()) {
                        CallPlacer.placeCall(context, number)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("Goi", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            if (number.isNotEmpty()) {
                Spacer(Modifier.width(20.dp))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable { number = number.dropLast(1) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Xoa", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun DialKey(digit: String, letters: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(digit, color = GOLD_BRIGHT, fontSize = 24.sp, fontWeight = FontWeight.Medium)
            if (letters.isNotEmpty()) {
                Text(letters, color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp)
            }
        }
    }
}
