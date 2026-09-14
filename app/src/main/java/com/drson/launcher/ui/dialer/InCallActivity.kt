package com.drson.launcher.ui.dialer

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.telecom.Call
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drson.launcher.telephony.CallRepository
import com.drson.launcher.telephony.CallUiState
import com.drson.launcher.telephony.MyInCallService

private val GOLD = Color(0xFFC99E5C)
private val GOLD_BRIGHT = Color(0xFFE6C178)
private val RED = Color(0xFFD8503A)
private val GREEN = Color(0xFF5FAE72)

class InCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Hiện màn hình đè cả khi máy đang khoá - đúng hành vi màn hình cuộc gọi chuẩn.
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                InCallScreen(onFinish = { finish() })
            }
        }
    }
}

@Composable
private fun InCallScreen(onFinish: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val callState by CallRepository.currentCall.collectAsState()

    LaunchedEffect(callState) {
        if (callState == null) onFinish()
    }

    val state = callState ?: return

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A090A)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(96.dp).clip(CircleShape).background(GOLD.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    state.displayName.firstOrNull()?.uppercase() ?: "?",
                    color = GOLD_BRIGHT,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(state.displayName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(
                callStateLabel(state.state),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
            )

            Spacer(Modifier.height(48.dp))

            when (state.state) {
                Call.STATE_RINGING -> RingingControls()
                else -> ActiveCallControls(state = state, context = context)
            }
        }
    }
}

private fun callStateLabel(state: Int): String = when (state) {
    Call.STATE_RINGING -> "Dang goi den..."
    Call.STATE_DIALING -> "Dang goi..."
    Call.STATE_ACTIVE -> "Dang trong cuoc goi"
    Call.STATE_HOLDING -> "Dang giu may"
    Call.STATE_DISCONNECTED -> "Da ket thuc"
    else -> ""
}

@Composable
private fun RingingControls() {
    Row(horizontalArrangement = Arrangement.spacedBy(64.dp)) {
        CircleActionButton(label = "Tu choi", color = RED, onClick = { CallRepository.reject() })
        CircleActionButton(label = "Tra loi", color = GREEN, onClick = { CallRepository.answer() })
    }
}

@Composable
private fun ActiveCallControls(state: CallUiState, context: Context) {
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        ToggleIconButton(
            label = if (state.isMuted) "Da tat tieng" else "Tat tieng",
            active = state.isMuted,
            onClick = { CallRepository.toggleMute(audioManager) },
        )
        ToggleIconButton(
            label = if (state.isSpeakerOn) "Loa ngoai: Bat" else "Loa ngoai",
            active = state.isSpeakerOn,
            onClick = {
                MyInCallService.instance?.let { svc ->
                    CallRepository.setSpeaker(svc, !state.isSpeakerOn)
                }
            },
        )
    }
    Spacer(Modifier.height(36.dp))
    CircleActionButton(label = "Ket thuc", color = RED, onClick = { CallRepository.hangUp() })
}

@Composable
private fun CircleActionButton(label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(color)
                .clickable(onClick = onClick),
        )
        Spacer(Modifier.height(8.dp))
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
    }
}

@Composable
private fun ToggleIconButton(label: String, active: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (active) GOLD else Color.White.copy(alpha = 0.1f))
                .clickable(onClick = onClick),
        )
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
    }
}
