package com.drson.launcher

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.drson.launcher.ui.HomeScreen
import com.drson.launcher.ui.HomeViewModel
import com.drson.launcher.ui.theme.DrSonLauncherTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private var lastScrollTime = 0L
    private val SCROLL_DEBOUNCE_MS = 140L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DrSonLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(viewModel = homeViewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            homeViewModel.load(this@MainActivity)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_MUSIC, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                val launchIntent = packageManager.getLaunchIntentForPackage("com.zing.mp3")
                    ?: packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(launchIntent)
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NAVIGATE_PREVIOUS, KeyEvent.KEYCODE_NAVIGATE_NEXT -> {
                super.onKeyDown(keyCode, event)
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_SCROLL) {
            val currentTime = SystemClock.uptimeMillis()
            if (currentTime - lastScrollTime < SCROLL_DEBOUNCE_MS) {
                return true
            }

            val scrollY = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            val scrollX = event.getAxisValue(MotionEvent.AXIS_HSCROLL)

            if (scrollY > 0 || scrollX > 0) {
                lastScrollTime = currentTime
                window.decorView.rootView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
                window.decorView.rootView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT))
                return true
            } else if (scrollY < 0 || scrollX < 0) {
                lastScrollTime = currentTime
                window.decorView.rootView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT))
                window.decorView.rootView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT))
                return true
            }
        }
        return super.onGenericMotionEvent(event)
    }
}
