package com.drson.launcher.ui.dialer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drson.launcher.telephony.CallPlacer
import com.drson.launcher.telephony.ContactEntry
import com.drson.launcher.telephony.ContactsRepository

private val GOLD = Color(0xFFC99E5C)
private val GOLD_BRIGHT = Color(0xFFE6C178)

@Composable
fun ContactsScreen() {
    val context = LocalContext.current
    var contacts by remember { mutableStateOf<List<ContactEntry>>(emptyList()) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        contacts = ContactsRepository.loadContacts(context)
    }

    val filtered = remember(contacts, query) {
        if (query.isBlank()) contacts
        else contacts.filter { it.name.contains(query, ignoreCase = true) || it.number.contains(query) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            if (query.isEmpty()) {
                Text("Tim danh ba", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
            }
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                cursorBrush = SolidColor(GOLD),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(filtered, key = { it.id.toString() + it.number }) { contact ->
                ContactRow(contact = contact, onClick = { CallPlacer.placeCall(context, contact.number) })
            }
        }
    }
}

@Composable
private fun ContactRow(contact: ContactEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(GOLD.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                contact.name.firstOrNull()?.uppercase() ?: "?",
                color = GOLD_BRIGHT,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(contact.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(contact.number, color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
        }
    }
}
