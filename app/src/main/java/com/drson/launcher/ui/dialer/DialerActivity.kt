package com.drson.launcher.ui.dialer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

private val BAT_DARK = Color(0xFF0A0907)
private val BAT_CARBON = Color(0xFF161410)
private val GOLD_HUD = Color(0xFFFFD700)
private val GOLD_BRIGHT = Color(0xFFFFF0B8)
private val GOLD_DARK = Color(0xFF8B7500)
private val GOLD_ACCENT = Color(0xFFD4AF37)

data class ContactItem(val name: String, val number: String)
data class CallLogItem(val name: String?, val number: String, val type: Int, val date: String)

class DialerActivity : ComponentActivity() {

    private var dialedNumber by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BatmobileDialerScreen(
                dialedNumber = dialedNumber,
                onDigitPress = { digit ->
                    if (dialedNumber.length < 16) dialedNumber += digit
                },
                onBackspace = {
                    if (dialedNumber.isNotEmpty()) dialedNumber = dialedNumber.dropLast(1)
                },
                onClearAll = { dialedNumber = "" },
                onSelectNumber = { number ->
                    dialedNumber = number
                },
                onCall = { numberToCall ->
                    val num = if (numberToCall.isNotBlank()) numberToCall else dialedNumber
                    if (num.isNotBlank()) {
                        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$num")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            startActivity(callIntent)
                        } catch (_: Exception) {
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num")).apply {
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
    onSelectNumber: (String) -> Unit,
    onCall: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Lịch sử cuộc gọi, 1: Danh bạ
    var contactsList by remember { mutableStateOf<List<ContactItem>>(emptyList()) }
    var callLogList by remember { mutableStateOf<List<CallLogItem>>(emptyList()) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasPermission = perms[Manifest.permission.READ_CONTACTS] == true || perms[Manifest.permission.READ_CALL_LOG] == true
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.CALL_PHONE
                )
            )
        } else {
            contactsList = loadContacts(context)
            callLogList = loadCallLogs(context)
        }
    }

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
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ================= CỘT TRÁI: BÀN PHÍM BẤM SỐ (GẦN TAY LÁI XE) =================
        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Nút Quay lại
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CutCornerShape(8.dp))
                        .background(BAT_CARBON)
                        .border(1.dp, GOLD_DARK, CutCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GOLD_BRIGHT)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "BÀN PHÍM SIÊU XE",
                    color = GOLD_HUD,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Lưới 12 phím bấm số
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (row in keypadKeys) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
            }

            // Hàng nút Xóa sạch / Xóa lùi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onClearAll,
                    shape = CutCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BAT_CARBON),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CutCornerShape(8.dp))
                ) {
                    Text("XÓA HẾT", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onBackspace,
                    shape = CutCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BAT_CARBON),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .border(1.dp, GOLD_DARK, CutCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Backspace, contentDescription = "Backspace", tint = GOLD_BRIGHT, modifier = Modifier.size(18.dp))
                }
            }
        }

        // ================= CỘT PHẢI: HIỂN THỊ SỐ + DANH BẠ & LỊCH SỬ CUỘC GỌI =================
        Column(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Màn hình HUD buồng lái hiển thị số đang nhập
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clip(CutCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF16130D), Color(0xFF0A0907)))
                    )
                    .border(1.2.dp, GOLD_HUD.copy(alpha = 0.6f), CutCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (dialedNumber.isEmpty()) "CHỌN DANH BẠ HOẶC NHẬP SỐ" else dialedNumber,
                        color = if (dialedNumber.isEmpty()) Color.Gray.copy(alpha = 0.6f) else GOLD_BRIGHT,
                        fontSize = if (dialedNumber.length > 11) 20.sp else 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 2. Chuyển Tab: LỊCH SỬ GỌI / DANH BẠ
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TabButton(
                    title = "Lịch sử cuộc gọi",
                    isSelected = selectedTab == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 0 }
                )
                TabButton(
                    title = "Danh bạ liên kết",
                    isSelected = selectedTab == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 1 }
                )
            }

            // 3. Khung danh sách Lịch sử hoặc Danh bạ
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BAT_CARBON)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(6.dp)
            ) {
                if (selectedTab == 0) {
                    // Hiển thị Lịch sử cuộc gọi
                    if (callLogList.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Chưa có nhật ký cuộc gọi", color = Color.Gray, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(callLogList) { log ->
                                CallLogRow(
                                    item = log,
                                    onClick = { onSelectNumber(log.number) },
                                    onCall = { onCall(log.number) }
                                )
                            }
                        }
                    }
                } else {
                    // Hiển thị Danh bạ
                    if (contactsList.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Chưa có danh bạ liên kết", color = Color.Gray, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(contactsList) { contact ->
                                ContactRow(
                                    item = contact,
                                    onClick = { onSelectNumber(contact.number) },
                                    onCall = { onCall(contact.number) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // 4. Nút Gọi Bắt đầu cuộc gọi phong cách Siêu Xe
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(CutCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFFB8860B), Color(0xFFFFD700)))
                    )
                    .border(1.5.dp, GOLD_BRIGHT, CutCornerShape(10.dp))
                    .clickable { onCall(dialedNumber) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Call, contentDescription = "Call", tint = Color.Black, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "BẮT ĐẦU CUỘC GỌI",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
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
            .height(48.dp)
            .clip(CutCornerShape(8.dp))
            .background(if (isFocused) Color(0xFF2E2619) else BAT_CARBON)
            .focusable(interactionSource = interactionSource)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) GOLD_BRIGHT else GOLD_DARK.copy(alpha = 0.4f),
                shape = CutCornerShape(8.dp)
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
                fontSize = 18.sp,
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

@Composable
private fun TabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) GOLD_ACCENT.copy(alpha = 0.25f) else Color.Transparent)
            .border(1.dp, if (isSelected) GOLD_ACCENT else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) GOLD_BRIGHT else Color.Gray,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun ContactRow(
    item: ContactItem,
    onClick: () -> Unit,
    onCall: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(item.number, color = GOLD_HUD.copy(alpha = 0.8f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        IconButton(onClick = onCall, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Phone, contentDescription = "Call", tint = GOLD_ACCENT, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun CallLogRow(
    item: CallLogItem,
    onClick: () -> Unit,
    onCall: () -> Unit
) {
    val iconColor = when (item.type) {
        CallLog.Calls.INCOMING_TYPE -> Color(0xFF4CAF50)
        CallLog.Calls.OUTGOING_TYPE -> Color(0xFF2196F3)
        CallLog.Calls.MISSED_TYPE -> Color(0xFFFF5252)
        else -> Color.Gray
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = when (item.type) {
                    CallLog.Calls.INCOMING_TYPE -> Icons.Default.CallReceived
                    CallLog.Calls.OUTGOING_TYPE -> Icons.Default.CallMade
                    CallLog.Calls.MISSED_TYPE -> Icons.Default.CallMissed
                    else -> Icons.Default.Call
                },
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(item.name ?: item.number, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("${item.number} • ${item.date}", color = Color.Gray, fontSize = 9.sp)
            }
        }
        IconButton(onClick = onCall, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Phone, contentDescription = "Call", tint = GOLD_ACCENT, modifier = Modifier.size(16.dp))
        }
    }
}

// Truy vấn danh bạ điện thoại
private fun loadContacts(context: Context): List<ContactItem> {
    val list = mutableListOf<ContactItem>()
    try {
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )
        cursor?.use {
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val name = it.getString(nameIdx) ?: ""
                val number = it.getString(numIdx) ?: ""
                list.add(ContactItem(name, number))
            }
        }
    } catch (_: Exception) {}
    return list
}

// Truy vấn nhật ký cuộc gọi
private fun loadCallLogs(context: Context): List<CallLogItem> {
    val list = mutableListOf<CallLogItem>()
    try {
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE
            ),
            null,
            null,
            CallLog.Calls.DATE + " DESC LIMIT 50"
        )
        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        cursor?.use {
            val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val numIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
            val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
            val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
            while (it.moveToNext()) {
                val name = it.getString(nameIdx)
                val number = it.getString(numIdx) ?: ""
                val type = it.getInt(typeIdx)
                val date = sdf.format(Date(it.getLong(dateIdx)))
                list.add(CallLogItem(name, number, type, date))
            }
        }
    } catch (_: Exception) {}
    return list
}
