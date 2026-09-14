package com.drson.launcher.ui.dialer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GOLD_BORDER = Color(0xFFC99E5C)
private val GOLD_BRIGHT = Color(0xFFFFF0B8)
private val BG_DARK = Color(0xFF0D0F12)
private val KEY_BG = Color(0xFF14181F)

class DialerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DialerScreen(
                onCall = { number ->
                    val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try { startActivity(intent) } catch (_: Exception) {}
                },
                onBack = { finish() }
            )
        }
    }
}

@Composable
fun DialerScreen(
    onCall: (String) -> Unit,
    onBack: () -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("NHẬT KÝ", "YÊU THÍCH", "DANH BẠ")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BG_DARK)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Tab: Nhật ký / Yêu thích / Danh bạ
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(38.dp)
                    .background(Color.Black.copy(alpha = 0.6f)),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, tabName ->
                    Text(
                        text = tabName,
                        color = if (selectedTab == index) GOLD_BRIGHT else Color.Gray,
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = FontFamily.Default,
                        modifier = Modifier
                            .clickable { selectedTab = index }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Màn hình hiển thị số đang bấm
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(48.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = phoneNumber.ifEmpty { "Nhập số điện thoại" },
                    color = if (phoneNumber.isEmpty()) Color.Gray else GOLD_BRIGHT,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Default,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                if (phoneNumber.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Backspace,
                        contentDescription = "Xóa",
                        tint = GOLD_BORDER,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { phoneNumber = phoneNumber.dropLast(1) }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // BÀN PHÍM ĐA GIÁC BATMAN STYLE
            BatmanDialPad(
                onKeyClick = { key -> phoneNumber += key },
                onCall = { if (phoneNumber.isNotEmpty()) onCall(phoneNumber) }
            )
        }
    }
}

@Composable
private fun BatmanDialPad(
    onKeyClick: (String) -> Unit,
    onCall: () -> Unit
) {
    val keyRows = listOf(
        listOf(Triple("1", "oo", KeyShape.LEFT_WING), Triple("2", "ABC", KeyShape.CENTER_HEX), Triple("3", "DEF", KeyShape.RIGHT_WING)),
        listOf(Triple("4", "GHI", KeyShape.LEFT_WING), Triple("5", "JKL", KeyShape.CENTER_HEX), Triple("6", "MNO", KeyShape.RIGHT_WING)),
        listOf(Triple("7", "PQRS", KeyShape.LEFT_WING), Triple("8", "TUV", KeyShape.CENTER_HEX), Triple("9", "WXYZ", KeyShape.RIGHT_WING)),
        listOf(Triple("*", "", KeyShape.LEFT_WING), Triple("0", "+", KeyShape.CENTER_HEX), Triple("#", "", KeyShape.RIGHT_WING))
    )

    Column(
        modifier = Modifier.width(360.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        keyRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                row.forEach { (digit, letters, shape) ->
                    BatmanKey(
                        digit = digit,
                        letters = letters,
                        shape = shape,
                        onClick = { onKeyClick(digit) }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Nút Gọi điện thoại
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color(0xFF2E7D32), Color(0xFF1B5E20))))
                .clickable(onClick = onCall),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Call, contentDescription = "Gọi", tint = Color.White, modifier = Modifier.size(26.dp))
        }
    }
}

private enum class KeyShape { LEFT_WING, CENTER_HEX, RIGHT_WING }

@Composable
private fun BatmanKey(
    digit: String,
    letters: String,
    shape: KeyShape,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val width = if (shape == KeyShape.CENTER_HEX) 115.dp else 95.dp
    val height = 46.dp

    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .focusable(interactionSource = interactionSource)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val path = Path()

            when (shape) {
                KeyShape.CENTER_HEX -> {
                    // Đa giác hình lục giác vát chéo trung tâm
                    path.moveTo(w * 0.18f, 0f)
                    lineTo(w * 0.82f, 0f)
                    lineTo(w, h * 0.5f)
                    lineTo(w * 0.82f, h)
                    lineTo(w * 0.18f, h)
                    lineTo(0f, h * 0.5f)
                    path.close()
                }
                KeyShape.LEFT_WING -> {
                    // Cánh trái vát chéo ôm theo tâm
                    path.moveTo(0f, 0f)
                    lineTo(w * 0.95f, 0f)
                    lineTo(w * 0.82f, h * 0.5f)
                    lineTo(w * 0.95f, h)
                    lineTo(0f, h)
                    path.close()
                }
                KeyShape.RIGHT_WING -> {
                    // Cánh phải vát chéo ôm theo tâm
                    path.moveTo(w * 0.05f, 0f)
                    lineTo(w, 0f)
                    lineTo(w, h)
                    lineTo(w * 0.05f, h)
                    lineTo(w * 0.18f, h * 0.5f)
                    path.close()
                }
            }

            // Đổ nền nút
            drawPath(path, color = if (isFocused) Color(0xFF2A2418) else KEY_BG)
            // Vẽ viền kim loại ánh vàng Batman
            drawPath(
                path = path,
                color = if (isFocused) GOLD_BRIGHT else GOLD_BORDER.copy(alpha = 0.65f),
                style = Stroke(width = if (isFocused) 2.5f else 1.2f)
            )
        }

        // Ký tự số & chữ phụ
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = digit,
                color = GOLD_BRIGHT,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default
            )
            if (letters.isNotEmpty()) {
                Text(
                    text = letters,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.Default
                )
            }
        }
    }
}
