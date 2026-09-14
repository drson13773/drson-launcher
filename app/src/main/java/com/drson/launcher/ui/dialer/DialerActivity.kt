package com.drson.launcher.ui.dialer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.drson.launcher.telephony.DialerRoleHelper

private val GOLD = Color(0xFFC99E5C)
private val GOLD_BRIGHT = Color(0xFFE6C178)

private enum class DialerTab(val label: String) {
    DIALPAD("Ban phim"), CONTACTS("Danh ba"), CALL_LOG("Lich su"),
}

class DialerActivity : ComponentActivity() {

    private val requiredPermissions = arrayOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_PHONE_STATE,
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* UI tu doc lai state qua remember */ }

    private val roleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { isDefaultDialer = DialerRoleHelper.isDefaultDialer(this) }

    private var isDefaultDialer by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isDefaultDialer = DialerRoleHelper.isDefaultDialer(this)

        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }

        setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                DialerScreen(
                    isDefaultDialer = isDefaultDialer,
                    onRequestDefaultDialer = {
                        roleLauncher.launch(DialerRoleHelper.createRequestRoleIntent(this))
                    },
                )
            }
        }
    }
}

@Composable
private fun DialerScreen(isDefaultDialer: Boolean, onRequestDefaultDialer: () -> Unit) {
    var tab by remember { mutableStateOf(DialerTab.DIALPAD) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A090A)),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!isDefaultDialer) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GOLD.copy(alpha = 0.15f))
                        .clickable(onClick = onRequestDefaultDialer)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Chạm để đặt làm ứng dụng Điện thoại mặc định",
                        color = GOLD_BRIGHT,
                        fontSize = 12.sp,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                DialerTab.values().forEach { t ->
                    val selected = t == tab
                    Text(
                        t.label,
                        color = if (selected) GOLD_BRIGHT else Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .clickable { tab = t }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    DialerTab.DIALPAD -> DialpadScreen()
                    DialerTab.CONTACTS -> ContactsScreen()
                    DialerTab.CALL_LOG -> CallLogScreen()
                }
            }
        }
    }
}
