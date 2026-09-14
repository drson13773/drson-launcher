package com.drson.launcher.ui.dialer

import android.provider.CallLog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.drson.launcher.telephony.CallLogEntry
import com.drson.launcher.telephony.CallLogRepository
import com.drson.launcher.telephony.CallPlacer
import java.text.SimpleDateFormat
import java.util.*

private val GOLD_BRIGHT = Color(0xFFE6C178)
private val MISSED_RED = Color(0xFFD86A5A)

@Composable
fun CallLogScreen() {
    val context = LocalContext.current
    var entries by remember { mutableStateOf<List<CallLogEntry>>(emptyList()) }

    LaunchedEffect(Unit) {
        entries = CallLogRepository.loadRecent(context)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(entries, key = { it.id }) { entry ->
            CallLogRow(entry = entry, onClick = { CallPlacer.placeCall(context, entry.number) })
        }
    }
}

@Composable
private fun CallLogRow(entry: CallLogEntry, onClick: () -> Unit) {
    val isMissed = entry.type == CallLog.Calls.MISSED_TYPE
    val typeLabel = when (entry.type) {
        CallLog.Calls.INCOMING_TYPE -> "Cuoc goi den"
        CallLog.Calls.OUTGOING_TYPE -> "Cuoc goi di"
        CallLog.Calls.MISSED_TYPE -> "Cuoc goi nho"
        else -> "Cuoc goi"
    }
    val timeStr = remember(entry.timestampMs) {
        SimpleDateFormat("HH:mm dd/MM", Locale.getDefault()).format(Date(entry.timestampMs))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                entry.name,
                color = if (isMissed) MISSED_RED else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(typeLabel, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        }
        Text(timeStr, color = GOLD_BRIGHT.copy(alpha = 0.85f), fontSize = 12.sp)
    }
}
