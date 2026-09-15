package com.drson.launcher.ui.dialer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

private val GOLD_BRIGHT = Color(0xFFFFF0B8)
private val GOLD_ACCENT = Color(0xFFD4AF37)
private val DARK_BG = Color(0xFF0C0B0A)
private val CALL_RED = Color(0xFFD32F2F)
private val CALL_GREEN = Color(0xFF2E7D32)

class DialerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DialerScreen(onClose = { finish() })
        }
    }
}

@SuppressLint("MissingPermission")
fun endActiveCall(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            telecomManager?.endCall()
        }
    } catch (_: Exception) {}
}

@Composable
fun DialerScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var inputNumber by remember { mutableStateOf("") }
    var isInCall by remember { mutableStateOf(false) }
    var callDurationSeconds by remember { mutableIntStateOf(0) }

    var hasCallPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCallPermission = isGranted
    }

    // Đếm thời gian khi đang đàm thoại
    LaunchedEffect(isInCall) {
        if (isInCall) {
            callDurationSeconds = 0
            while (isInCall) {
                delay(1000)
                callDurationSeconds++
            }
        }
    }

    // Lắng nghe trạng thái ngắt cuộc gọi từ hệ thống
    DisposableEffect(Unit) {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    if (state == TelephonyManager.CALL_STATE_IDLE) {
                        isInCall = false
                    } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                        isInCall = true
                    }
                }
            }
            try {
                telephonyManager?.registerTelephonyCallback(context.mainExecutor, callback)
            } catch (_: Exception) {}
            onDispose {
                try {
                    telephonyManager?.unregisterTelephonyCallback(callback)
                } catch (_: Exception) {}
            }
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    if (state == TelephonyManager.CALL_STATE_IDLE) {
                        isInCall = false
                    } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                        isInCall = true
                    }
                }
            }
            @Suppress("DEPRECATION")
            telephonyManager?.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
            onDispose {
                @Suppress("DEPRECATION")
                telephonyManager?.listen(listener, PhoneStateListener.LISTEN_NONE)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DARK_BG)
            .padding(20.dp)
    ) {
        // Nút Đóng góc trên bên phải
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(36.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = GOLD_ACCENT)
        }

        if (!isInCall) {
            // ================= TRẠNG THÁI 1: BÀN PHÍM QUAY SỐ =================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Ô hiển thị số điện thoại đang nhập
                Text(
                    text = if (inputNumber.isEmpty()) "Nhập số điện thoại..." else inputNumber,
                    color = if (inputNumber.isEmpty()) GOLD_ACCENT.copy(alpha = 0.4f) else GOLD_BRIGHT,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Bàn phím số 3x4
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("*", "0", "#")
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    keys.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            row.forEach { digit ->
                                DialKey(
                                    text = digit,
                                    onClick = { inputNumber += digit }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Hàng nút: Xóa số và Nút Gọi
                Row(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Nút Xóa
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable {
                                if (inputNumber.isNotEmpty()) {
                                    inputNumber = inputNumber.dropLast(1)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Backspace, contentDescription = "Delete", tint = GOLD_ACCENT, modifier = Modifier.size(22.dp))
                    }

                    // Nút Gọi (Call Button)
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(CALL_GREEN)
                            .border(1.5.dp, GOLD_BRIGHT, CircleShape)
                            .clickable {
                                if (inputNumber.isNotBlank()) {
                                    if (!hasCallPermission) {
                                        permissionLauncher.launch(Manifest.permission.CALL_PHONE)
                                    } else {
                                        isInCall = true
                                        val intent = Intent(Intent.ACTION_CALL).apply {
                                            data = Uri.parse("tel:$inputNumber")
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
        } else {
            // ================= TRẠNG THÁI 2: ĐANG TRONG CUỘC GỌI =================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Biểu tượng cuộc gọi đang kết nối
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1B1812))
                        .border(2.dp, GOLD_ACCENT, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = GOLD_BRIGHT,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Số điện thoại đang gọi
                Text(
                    text = inputNumber,
                    color = GOLD_BRIGHT,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )

                // Thời gian đàm thoại
                val minutes = callDurationSeconds / 60
                val seconds = callDurationSeconds % 60
                Text(
                    text = String.format("%02d:%02d", minutes, seconds),
                    color = GOLD_ACCENT,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Serif,
                    modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
                )

                // NÚT KẾT THÚC CUỘC GỌI (NÚT ĐỎ LỚN DỄ BẤM KHI LÁI XE)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(CALL_RED)
                        .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                        .clickable {
                            endActiveCall(context)
                            isInCall = false
                            inputNumber = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Kết thúc cuộc gọi",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DialKey(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 72.dp, height = 50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF14120E))
            .border(1.dp, GOLD_ACCENT.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = GOLD_BRIGHT,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
