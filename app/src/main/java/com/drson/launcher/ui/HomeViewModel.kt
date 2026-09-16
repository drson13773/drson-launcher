package com.drson.launcher.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.drson.launcher.model.AppItem
import java.util.*

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentSpeed = mutableFloatStateOf(0f)
    val currentSpeed: State<Float> = _currentSpeed

    private val _apps = mutableStateListOf<AppItem>()
    val apps: List<AppItem> = _apps

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        val pm = getApplication<Application>().packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val list = mutableListOf<AppItem>()

        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            // Bỏ qua chính launcher để tránh tự hiển thị
            if (pkg == getApplication<Application>().packageName) continue
            val label = info.loadLabel(pm).toString()
            val iconDrawable = info.loadIcon(pm)
            val bitmap = drawableToBitmap(iconDrawable)
            list.add(AppItem(packageName = pkg, label = label, icon = bitmap))
        }

        list.sortBy { it.label.lowercase(Locale.getDefault()) }
        _apps.clear()
        _apps.addAll(list)
    }

    fun appFor(packageName: String): AppItem? {
        return _apps.find { it.packageName == packageName }
    }

    fun launchApp(context: Context, app: AppItem) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        } catch (_: Exception) {}
    }

    fun initGpsSpeedListener() {
        try {
            val context = getApplication<Application>()
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
            
            val hasFine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

            if (!hasFine && !hasCoarse) return

            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    val speedMs = location.speed
                    val speedKm = if (speedMs > 0f) speedMs * 3.6f else 0f
                    // Cập nhật tốc độ an toàn trên luồng chính
                    Handler(Looper.getMainLooper()).post {
                        _currentSpeed.floatValue = speedKm
                    }
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, locationListener)
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 1f, locationListener)
            }
        } catch (_: Exception) {}
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            if (drawable.bitmap != null) return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
