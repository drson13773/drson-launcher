package com.drson.launcher.ui.dialer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

private val GOLD_BRIGHT = Color(0xFFFFF0B8)
private val GOLD_MID = Color(0xFFFFDF73)
private val GOLD_ACCENT = Color(0xFFD4AF37)
private val DARK_BG = Color(0xFF0C0B0A)
private val DARK_CARD = Color(0xFF14120E)
private val CALL_RED = Color(0xFFD32F2F)
private val CALL_GREEN = Color(0xFF2E7D32)

data class ContactModel(val name: String, val number: String)
data class RecentCallModel(val name: String?, val number: String, val type: Int, val date: Long)

enum class DialerTab { KEYPAD, RECENTS, CONTACTS }

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
    var selectedTab by remember { mutableStateOf(DialerTab.KEYPAD) }

    var inputNumber by remember { mutableStateOf("") }
    var isInCall by remember { mutableStateOf(false) }
    var callDurationSeconds by remember { mutableIntStateOf(0) }

    // Quản lý cấp quyền
    var hasCallPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasContactsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasCallLogPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasCallPermission = perms[Manifest.permission.CALL_PHONE] ?: hasCallPermission
        hasContactsPermission = perms[Manifest.permission.READ_CONTACTS] ?: hasContactsPermission
        hasCallLogPermission = perms[Manifest.permission.READ_CALL_LOG] ?: hasCallLogPermission
    }

    LaunchedEffect(Unit) {
        val neededPerms = mutableListOf<String>()
        if (!hasCallPermission) neededPerms.add(Manifest.permission.CALL_PHONE)
        if (!hasContactsPermission) neededPerms.add(Manifest.permission.READ_CONTACTS)
        if (!hasCallLogPermission) neededPerms.add(Manifest.permission.READ_CALL_LOG)
        if (neededPerms.isNotEmpty()) {
            permissionLauncher.launch(neededPerms.toTypedArray())
        }
    }

    // Hàm gọi điện
    val makeCall: (String) -> Unit = { numberToCall ->
        if (numberToCall.isNotBlank()) {
            inputNumber = numberToCall
            if (!hasCallPermission) {
                permissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE))
            } else {
                isInCall = true
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$numberToCall")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        }
    }

    // Đếm thời gian đàm thoại
    LaunchedEffect(isInCall) {
        if (isInCall) {
            callDurationSeconds = 0
            while (isInCall) {
                delay(1000)
                callDurationSeconds++
            }
        }
    }

    // Lắng nghe trạng thái ngắt cuộc gọi từ hệ điều hành
    DisposableEffect(Unit) {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    if (state == TelephonyManager.CALL_STATE_IDLE) isInCall = false
                    else if (state == TelephonyManager.CALL_STATE_OFFHOOK) isInCall = true
                }
            }
            try {
                telephonyManager?.registerTelephonyCallback(context.mainExecutor, callback)
            } catch (_: Exception) {}
            onDispose {
                try { telephonyManager?.unregisterTelephonyCallback(callback) } catch (_: Exception) {}
            }
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    if (state == TelephonyManager.CALL_STATE_IDLE) isInCall = false
                    else if (state == TelephonyManager.CALL_STATE_OFFHOOK) isInCall = true
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
            .padding(16.dp)
    ) {
        if (isInCall) {
            // ================= TRẠNG THÁI: ĐANG ĐÀM THOẠI =================
            ActiveCallView(
                phoneNumber = inputNumber,
                durationSeconds = callDurationSeconds,
                onEndCall = {
                    endActiveCall(context)
                    isInCall = false
                }
            )
        } else {
            // ================= TRẠNG THÁI: GIAO DIỆN CHÍNH (BÀN PHÍM / GẦN ĐÂY / DANH BẠ) =================
            Column(modifier = Modifier.fillMaxSize()) {
                // Header: Thanh Tab chuyển đổi + Nút đóng
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cụm Tab chọn chế độ
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(DARK_CARD)
                            .border(1.dp, GOLD_ACCENT.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TabButton(
                            label = "Bàn phím",
                            icon = Icons.Default.Dialpad,
                            isSelected = selectedTab == DialerTab.KEYPAD,
                            onClick = { selectedTab = DialerTab.KEYPAD }
                        )
                        TabButton(
                            label = "Gần đây",
                            icon = Icons.Default.History,
                            isSelected = selectedTab == DialerTab.RECENTS,
                            onClick = { selectedTab = DialerTab.RECENTS }
                        )
                        TabButton(
                            label = "Danh bạ",
                            icon = Icons.Default.Contacts,
                            isSelected = selectedTab == DialerTab.CONTACTS,
                            onClick = { selectedTab = DialerTab.CONTACTS }
                        )
                    }

                    // Nút Đóng App
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(DARK_CARD)
                            .border(1.dp, GOLD_ACCENT.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = GOLD_BRIGHT, modifier = Modifier.size(18.dp))
                    }
                }

                // Nội dung từng Tab
                Box(modifier = Modifier.fillMaxSize()) {
                    when (selectedTab) {
                        DialerTab.KEYPAD -> {
                            KeypadView(
                                inputNumber = inputNumber,
                                onNumberChange = { inputNumber = it },
                                onCall = { makeCall(inputNumber) }
                            )
                        }
                        DialerTab.RECENTS -> {
                            RecentsView(
                                hasPermission = hasCallLogPermission,
                                onRequestPermission = { permissionLauncher.launch(arrayOf(Manifest.permission.READ_CALL_LOG)) },
                                onCall = makeCall
                            )
                        }
                        DialerTab.CONTACTS -> {
                            ContactsView(
                                hasPermission = hasContactsPermission,
                                onRequestPermission = { permissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS)) },
                                onCall = makeCall
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------- 1. VIEW BÀN PHÍM SỐ ----------------
@Composable
private fun KeypadView(
    inputNumber: String,
    onNumberChange: (String) -> Unit,
    onCall: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (inputNumber.isEmpty()) "Nhập số..." else inputNumber,
            color = if (inputNumber.isEmpty()) GOLD_ACCENT.copy(alpha = 0.4f) else GOLD_BRIGHT,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("*", "0", "#")
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            keys.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { digit ->
                        DialKey(
                            text = digit,
                            onClick = { onNumberChange(inputNumber + digit) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .clickable {
                        if (inputNumber.isNotEmpty()) {
                            onNumberChange(inputNumber.dropLast(1))
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Backspace, contentDescription = "Delete", tint = GOLD_ACCENT, modifier = Modifier.size(20.dp))
            }

            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(CALL_GREEN)
                    .border(1.5.dp, GOLD_BRIGHT, CircleShape)
                    .clickable(onClick = onCall),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Call, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(26.dp))
            }
        }
    }
}

// ---------------- 2. VIEW CUỘC GỌI GẦN ĐÂY ----------------
@Composable
private fun RecentsView(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onCall: (String) -> Unit
) {
    val context = LocalContext.current
    var recentCalls by remember { mutableStateOf<List<RecentCallModel>>(emptyList()) }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            val list = mutableListOf<RecentCallModel>()
            val uri = CallLog.Calls.CONTENT_URI
            val projection = arrayOf(
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE
            )
            try {
                val cursor: Cursor? = context.contentResolver.query(
                    uri, projection, null, null, "${CallLog.Calls.DATE} DESC"
                )
                cursor?.use {
                    val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                    val numIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
                    val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
                    val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)

                    var count = 0
                    while (it.moveToNext() && count < 40) {
                        val name = if (nameIdx != -1) it.getString(nameIdx) else null
                        val num = if (numIdx != -1) it.getString(numIdx) ?: "" else ""
                        val type = if (typeIdx != -1) it.getInt(typeIdx) else CallLog.Calls.INCOMING_TYPE
                        val date = if (dateIdx != -1) it.getLong(dateIdx) else 0L
                        if (num.isNotBlank()) {
                            list.add(RecentCallModel(name, num, type, date))
                            count++
                        }
                    }
                }
            } catch (_: Exception) {}
            recentCalls = list
        }
    }

    if (!hasPermission) {
        PermissionNotice(message = "Cần quyền truy cập Nhật ký cuộc gọi", onGrant = onRequestPermission)
    } else if (recentCalls.isEmpty()) {
        EmptyNotice(message = "Chưa có lịch sử cuộc gọi gần đây")
    } else {
        val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(recentCalls) { call ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DARK_CARD)
                        .border(1.dp, GOLD_ACCENT.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .clickable { onCall(call.number) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val icon = when (call.type) {
                            CallLog.Calls.MISSED_TYPE -> Icons.Default.CallMissed
                            CallLog.Calls.OUTGOING_TYPE -> Icons.Default.CallMade
                            else -> Icons.Default.CallReceived
                        }
                        val iconColor = if (call.type == CallLog.Calls.MISSED_TYPE) CALL_RED else GOLD_ACCENT

                        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))

                        Column {
                            Text(
                                text = call.name ?: call.number,
                                color = GOLD_BRIGHT,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (call.name != null) {
                                Text(
                                    text = call.number,
                                    color = GOLD_MID.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Text(
                        text = dateFormat.format(Date(call.date)),
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// ---------------- 3. VIEW DANH BẠ ĐIỆN THOẠI ----------------
@Composable
private fun ContactsView(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onCall: (String) -> Unit
) {
    val context = LocalContext.current
    var contactsList by remember { mutableStateOf<List<ContactModel>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            val list = mutableListOf<ContactModel>()
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            try {
                val cursor: Cursor? = context.contentResolver.query(
                    uri, projection, null, null, "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
                )
                cursor?.use {
                    val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    while (it.moveToNext()) {
                        val name = if (nameIdx != -1) it.getString(nameIdx) ?: "Không tên" else "Không tên"
                        val num = if (numIdx != -1) it.getString(numIdx) ?: "" else ""
                        if (num.isNotBlank()) {
                            list.add(ContactModel(name, num))
                        }
                    }
                }
            } catch (_: Exception) {}
            contactsList = list
        }
    }

    val filteredContacts = remember(contactsList, searchQuery) {
        if (searchQuery.isBlank()) contactsList
        else contactsList.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.number.contains(searchQuery)
        }
    }

    if (!hasPermission) {
        PermissionNotice(message = "Cần quyền truy cập Danh bạ", onGrant = onRequestPermission)
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            // Ô tìm kiếm danh bạ
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DARK_CARD)
                    .border(1.dp, GOLD_ACCENT.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = GOLD_ACCENT, modifier = Modifier.size(18.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Tìm theo tên hoặc số...", color = Color.Gray, fontSize = 13.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = GOLD_BRIGHT,
                        unfocusedTextColor = GOLD_BRIGHT,
                        cursorColor = GOLD_BRIGHT,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(Modifier.height(8.dp))

            if (filteredContacts.isEmpty()) {
                EmptyNotice(message = "Không tìm thấy liên hệ phù hợp")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredContacts) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DARK_CARD)
                                .border(1.dp, GOLD_ACCENT.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .clickable { onCall(contact.number) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = contact.name,
                                    color = GOLD_BRIGHT,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = contact.number,
                                    color = GOLD_MID.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                            Icon(Icons.Default.Call, contentDescription = "Call", tint = CALL_GREEN, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

// ---------------- 4. VIEW ĐANG GỌI & KẾT THÚC ----------------
@Composable
private fun ActiveCallView(
    phoneNumber: String,
    durationSeconds: Int,
    onEndCall: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(Color(0xFF1B1812))
                .border(2.dp, GOLD_ACCENT, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = GOLD_BRIGHT, modifier = Modifier.size(48.dp))
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = phoneNumber,
            color = GOLD_BRIGHT,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp
        )

        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        Text(
            text = String.format("%02d:%02d", minutes, seconds),
            color = GOLD_ACCENT,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            fontStyle = FontStyle.Italic,
            fontFamily = FontFamily.Serif,
            modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
        )

        // Nút Kết thúc cuộc gọi màu Đỏ lớn
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(CALL_RED)
                .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                .clickable(onClick = onEndCall),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CallEnd, contentDescription = "End Call", tint = Color.White, modifier = Modifier.size(36.dp))
        }
    }
}

// ---------------- CÁC THÀNH PHẦN PHỤ TRỢ ----------------
@Composable
private fun TabButton(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) GOLD_ACCENT else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = label, tint = if (isSelected) Color.Black else GOLD_BRIGHT, modifier = Modifier.size(16.dp))
        Text(
            text = label,
            color = if (isSelected) Color.Black else GOLD_BRIGHT,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun DialKey(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 68.dp, height = 46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DARK_CARD)
            .border(1.dp, GOLD_ACCENT.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = GOLD_BRIGHT,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PermissionNotice(message: String, onGrant: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, color = GOLD_BRIGHT, fontSize = 14.sp)
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onGrant,
            colors = ButtonDefaults.buttonColors(containerColor = GOLD_ACCENT)
        ) {
            Text("Cấp quyền", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyNotice(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = Color.Gray, fontSize = 13.sp)
    }
}
